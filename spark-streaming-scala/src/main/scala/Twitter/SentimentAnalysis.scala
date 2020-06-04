package Twitter

import java.util

import org.apache.spark.SparkConf
import org.apache.spark.streaming.{rdd, _}
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.TopicPartition
import org.apache.spark.streaming.kafka010._
import org.apache.spark.storage.StorageLevel
import org.apache.spark.sql.{DataFrame, SQLContext}
import org.json4s._
import org.json4s.native.JsonMethods._
import java.io._

import org.apache.commons._
import org.apache.http._
import org.apache.http.client._
import org.apache.http.client.methods.HttpPost
import org.apache.http.impl.client.DefaultHttpClient
import java.util.ArrayList

import org.apache.http.message.BasicNameValuePair
import org.apache.http.client.entity.UrlEncodedFormEntity
import com.google.gson.Gson
import NLPAnalyzer.sentiment
import NLPAnalyzer.mainSentiment

import scala.collection.mutable.{ArrayBuffer, ListBuffer}



case class TwitterResult(label: String, negative: String, neutral: String, positive: String)
case class TwitterCount(total: String, negative: String, neutral: String, positive: String)

object SentimentAnalysis {

  def get_sentiment_tuple(sense: String): (Int, Int, Int) = {
    if (sense == "POSITIVE") (0, 0, 1)
    else if (sense == "NEUTRAL") (0, 1, 0)
    else if (sense == "NEGATIVE") (1, 0, 0)
    else (0, 0, 0)
  }

  def post_to_dashboard(topTen: Array[(String, (Int, (Int, Int, Int)))], url: String) {

      val labels, negative, neutral, positive = new ArrayBuffer[String]()
      for ((label: String,(_, (neg: Int, neu: Int, pos: Int))) <- topTen) {
        labels.append(label)
        negative.append(neg.toString)
        neutral.append(neu.toString)
        positive.append(pos.toString)
      }
      val postData = TwitterResult(labels.mkString(","), negative.mkString(","), neutral.mkString(","), positive.mkString(","))
      val postDataAsJson = new Gson().toJson(postData)

      println(postDataAsJson)

      // add name value pairs to a post object
      val post = new HttpPost(url)
      val nameValuePairs = new ArrayList[NameValuePair]()
      nameValuePairs.add(new BasicNameValuePair("JSON", postDataAsJson))
      post.setEntity(new UrlEncodedFormEntity(nameValuePairs))

      // send the post request
      val client = new DefaultHttpClient
      val response = client.execute(post)
  }

  def main(args: Array[String]) {
    if (args.length != 2) {
      System.err.println(s"""
        |Usage: SentimentAnalysis <brokers> <topics>
        |  <brokers> is a list of one or more Kafka brokers
        |  <topics> is a list of one or more kafka topics to consume from
        |
        """.stripMargin)
      System.exit(1)
    }
    val Array(brokers, topics) = args

    val conf = new SparkConf()
      .setMaster("local[3]")
      .setAppName("SentimentAnalysis")

    val batchInterval = 2
    val windowLength = 15 * 60
    val slidingInterval = 6

    val ssc = new StreamingContext(conf, Seconds(2))
    ssc.checkpoint("twittercheckpoint")

    val kafkaParams = Map[String, Object](
      "bootstrap.servers" -> brokers,
      "key.deserializer" -> classOf[StringDeserializer],
      "value.deserializer" -> classOf[StringDeserializer],
      "group.id" -> "use_a_separate_group_id_for_each_stream",
      "auto.offset.reset" -> "latest",
      "enable.auto.commit" -> (false: java.lang.Boolean)
    )

    val twitterKafkaStream = KafkaUtils.createDirectStream[String, String](
      ssc,
      LocationStrategies.PreferConsistent,
      ConsumerStrategies.Subscribe[String, String](Array(topics), kafkaParams)
    )

    val tweets = twitterKafkaStream
      .map( x => parse(x.value))
      .map( jsons => (pretty(render((jsons \ "user") \ "screen_name")), pretty(render(jsons \ "text"))) )

    // (user, text, sentiment(mood))
    val sentiments = tweets.map {case (user, text) => (user, text, get_sentiment_tuple(mainSentiment(text).toString))}

    sentiments.persist(StorageLevel.MEMORY_AND_DISK)

    /* Extract top hashtags, start with #
    *  Extract top mentioned user, start with @ */
    val hashtags = sentiments
      .map {case (user, text, mood) => ((user, mood), text) }
      .flatMapValues( text => text.split(" "))
      .filter {case (userMood, word) => word.length > 1 && word.startsWith("#")}
      .map {case ((user, mood), tag) => (tag, (1, mood))}

    val mentioned = sentiments
      .map {case (user, text, mood) => ((user, mood), text) }
      .flatMapValues( text => text.split(" "))
      .filter {case (userMood, word) => word.length > 1 && word.startsWith("@")}
      .map {case ((user, mood), tag) => (tag, (1, mood))}

    val countMood: ( (Int, (Int, Int, Int)), (Int, (Int, Int, Int)) ) => (Int, (Int, Int, Int)) = {
      case ((num1, (a,b,c)), (num2, (d,e,f))) => (num1+num2, (a+d, b+e, c+f))
    }

    val topHashtags = hashtags
      .reduceByKeyAndWindow(
        countMood,
        Seconds(windowLength),
        Seconds(slidingInterval)
      )

    val sortedHashtags = topHashtags
      .map {case (tag, (cnt, mood)) => (cnt, (tag, mood))}
      .transform(rdd => rdd.sortByKey(false)) // descending
      .map {case (cnt, (tag, mood)) => (tag, (cnt, mood))}

    val topMentioned = mentioned
      .reduceByKeyAndWindow(
        countMood,
        Seconds(windowLength),
        Seconds(slidingInterval)
      )

    val sortedMentioned = topMentioned
      .map {case (tag, (cnt, mood)) => (cnt, (tag, mood))}
      .transform(rdd => rdd.sortByKey(false)) // descending
      .map {case (cnt, (tag, mood)) => (tag, (cnt, mood))}


    /* Get most active users */
    val users = sentiments
      .map {case (user, text, mood) => (user, (1, mood)) }

    val topUsers = users
      .reduceByKeyAndWindow(
        countMood,
        Seconds(windowLength),
        Seconds(slidingInterval)
      )

    val sortedUsers = topUsers
      .map {case (user, (cnt, mood)) => (cnt, (user, mood))}
      .transform(rdd => rdd.sortByKey(false)) // descending
      .map {case (cnt, (user, mood)) => (user, (cnt, mood))}

    /* Post sortedHashtags, sortedMentioned, sortedUsers to url dashboard*/
    val server = "http://localhost:5001/"

    sortedHashtags.foreachRDD(rdd => {
      val topTen = rdd.take(10)
      post_to_dashboard(topTen, server+"update_most_used_hashtags")
    })
    sortedMentioned.foreachRDD(rdd => {
      val topTen = rdd.take(10)
      post_to_dashboard(topTen, server+"update_most_mentioned_users")
    })
    sortedUsers.foreachRDD(rdd => {
      val topTen = rdd.take(10)
      post_to_dashboard(topTen, server+"update_most_active_users")
    })

    /* Count tweets' different moods and send to dashboard */
    val moodsNum = sentiments.map {case (user, text, mood) => ("count", (1, mood))}
    val moodsCount = moodsNum.reduceByKeyAndWindow(countMood, Seconds(windowLength), Seconds(slidingInterval))
    val moodsTotalCount = moodsCount.map {case (tag, (cnt, mood)) => (cnt, mood)}

    moodsTotalCount.foreachRDD(rdd => {
      val (total, (neg, neu, pos)) = rdd.first()
      val postData = TwitterCount(total.toString, neg.toString, neu.toString, pos.toString)
      val postDataAsJson = new Gson().toJson(postData)

      println(postDataAsJson)

      // add name value pairs to a post object
      val post = new HttpPost(server + "update_tweet_counters")
      val nameValuePairs = new ArrayList[NameValuePair]()
      nameValuePairs.add(new BasicNameValuePair("JSON", postDataAsJson))
      post.setEntity(new UrlEncodedFormEntity(nameValuePairs))

      // send the post request
      val client = new DefaultHttpClient
      val response = client.execute(post)
    })

    ssc.start()
    ssc.awaitTermination()


  }
}
