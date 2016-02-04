package fr.ippon.spark.streaming

import java.text.SimpleDateFormat

import com.cybozu.labs.langdetect.DetectorFactory
import org.apache.spark.sql.SQLContext
import org.apache.spark.streaming.twitter.TwitterUtils
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.{SparkContext, SparkConf}
import org.elasticsearch.spark.rdd.EsSpark
import play.api.libs.json._
import org.scalatest.{BeforeAndAfter, Matchers, FlatSpec}
import twitter4j.auth.AuthorizationFactory
import twitter4j.conf.ConfigurationContext

/**
  * Created by ahars on 26/01/2016.
  */
class processTweets extends FlatSpec with Matchers with BeforeAndAfter {

  private var sc: SparkContext = _
  private var sqlc: SQLContext = _
  private var ssc: StreamingContext = _

  before {
    val sparkConf = new SparkConf()
      .setAppName("processTweets")
      .setMaster("local[*]")
      .set("es.nodes", "localhost:9200")
      .set("es.index.auto.create", "true")

    sc = new SparkContext(sparkConf)
    sqlc = new SQLContext(sc)
    ssc = new StreamingContext(sc, Seconds(2))
  }

  after {
    ssc.stop()
    sc.stop()
  }

  "Live processing of tweets with #Android" should "collect them from Twitter, print and store into ElasticSearch" in {

    // Twitter4J
    // IMPORTANT: ajuster vos clés d'API dans twitter4J.properties
    val twitterConf = ConfigurationContext.getInstance()
    val twitterAuth = Option(AuthorizationFactory.getInstance(twitterConf))

    // Language Detection
    DetectorFactory.loadProfile("src/main/resources/profiles")
    val lang = new LangProcessing

    // Formatage des dates
    val df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")

    // Choix des Hashtags à filtrer
    val filters: Array[String] = Array("#Android")

    TwitterUtils
      .createStream(ssc, twitterAuth, filters)
      .map(data => Tweet(data.getUser.getName(), data.getText(), df.format(data.getCreatedAt()), lang.detectLanguage((data.getText()))))
      .map(tweet => Json.writes[Tweet].writes(tweet)) // formatage des Tweets
      .foreachRDD(json => {
        json.foreach(println) // affichage
        EsSpark.saveJsonToEs(json, "streaming/tweets") // stockage des tweets sur ElasticSearch
      })

    ssc.start()
    ssc.awaitTerminationOrTimeout(1000000)

    // Lecture des Tweets depuis ElasticSearch
    sqlc.read
      .format("org.elasticsearch.spark.sql")
      .load("streaming/tweets")
      .show()
  }

  "Collect of live tweets with #Android" should "collect them from Twitter and store into files" in {

    // Twitter4J
    // IMPORTANT: ajuster vos clés d'API dans twitter4J.properties
    val twitterConf = ConfigurationContext.getInstance()
    val twitterAuth = Option(AuthorizationFactory.getInstance(twitterConf))

    // Language Detection
    DetectorFactory.loadProfile("src/main/resources/profiles")
    val lang = new LangProcessing

    // Formatage des dates
    val df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")

    // Choix des Hashtags à filtrer
    val filters: Array[String] = Array("#Android")

    TwitterUtils
      .createStream(ssc, twitterAuth, filters)
      .map(data => Tweet(data.getUser.getName(), data.getText(), df.format(data.getCreatedAt()), lang.detectLanguage((data.getText()))))
      .map(tweet => Json.writes[Tweet].writes(tweet))
      .repartition(1)
      .saveAsTextFiles("src/main/resources/data/tweet/part")

    ssc.start()
    ssc.awaitTerminationOrTimeout(1000000)
  }

  "Offline processing of tweets with #Android" should "collect them from files, print and store into ElasticSearch" in {

    ssc
      .textFileStream("src/main/resources/data/tweet/processing")
      .foreachRDD(json => {
        json.foreach(println)
        EsSpark.saveJsonToEs(json, "streaming/tweets")
      })

    ssc.start()
    ssc.awaitTerminationOrTimeout(100000)

    // Lecture des Tweets depuis ElasticSearch
    sqlc.read
      .format("org.elasticsearch.spark.sql")
      .load("streaming/tweets")
      .show()
  }

}