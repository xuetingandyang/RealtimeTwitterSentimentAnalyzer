# RealtimeTwitterSentimentAnalyzer

A realtime Twitter Sentiment Analyzer with Spark Streaming, Flume, Kafka, and Flask.

## Data Ingesting with Flume: 
 
Ingest and filter tweets from Twitter API 
with customized event-driven source and interceptor by Flume.
Then, all accepted tweets will end up on a Kafka tipic, by configuring Kafka as a sink.
  
## Sentiment Analysis with Spark Streaming: 

Use Spark STreaming with Scala to process and analyze every 5 second, 
the latest tweets received during a window of the last 30 minutes. 

Calculate the most used hashtags, the users most mentioned, and the most active users. 

Perform a sentiment analysis by using the Stanford CoreNLP library.


## Dashboard Display with Flask: 

Flask Web Application for displaying the results using Chart.js.

