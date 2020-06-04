name := "spark-streaming-scala"

version := "0.1"

scalaVersion := "2.12.10"


libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2",
  // https://mvnrepository.com/artifact/org.apache.spark/spark-core
  "org.apache.spark" %% "spark-core" % "3.0.0-preview2",
  "org.apache.spark" %% "spark-sql" % "3.0.0-preview2",
  "org.apache.spark" %% "spark-streaming" % "3.0.0-preview2",

  "org.apache.spark" %% "spark-streaming-kafka-0-10" % "3.0.0-preview2",
//  "org.apache.spark" %% "spark-sql-kafka-0-10" % "3.0.0-preview2" % "provided"

  "org.json4s" %% "json4s-native" % "3.6.8",

  "edu.stanford.nlp" % "stanford-corenlp" % "3.5.2" artifacts (Artifact("stanford-corenlp", "models"), Artifact("stanford-corenlp"))
//  "edu.stanford.nlp" % "stanford-corenlp" % "3.8.0"

)