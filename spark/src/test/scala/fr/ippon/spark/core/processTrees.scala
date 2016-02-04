package fr.ippon.spark.core

import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.SQLContext
import org.scalatest.{BeforeAndAfter, Matchers, FlatSpec}

/**
  * Created by ahars on 28/01/2016.
  */
class processTrees extends FlatSpec with Matchers with BeforeAndAfter {

  private var sc: SparkContext = _
  private var sqlc: SQLContext = _

  before {
    val sparkConf = new SparkConf()
      .setAppName("processTrees")
      .setMaster("local[*]")

    sc = new SparkContext(sparkConf)
    sqlc = new SQLContext(sc)
  }

  after {
    sc.stop()
  }

  "RDD : Comptage des arbres de Paris par espèce" should "afficher les résultats" in {

    val trees = sc.textFile("src/main/resources/data/tree/arbresalignementparis2010.csv")

    trees
      .take(20)
      .foreach(println)

    println
    println("-------------------------------")
    println

    trees
      .filter(line => !line.startsWith("geom"))
      .map(line => line.split(";", -1))
      .map(fields => (fields(4), 1))
      .reduceByKey(_+_)  // comptage des espèces
      .sortByKey()  // tri sur la colonne des espèces
      .foreach(result => println(result._1 + " : " + result._2))
  }

  "DataFrame - méthode 1 : Comptage des abres de Paris par espèce" should "afficher les résultats" in {

    val trees = sqlc.read
      .format("com.databricks.spark.csv")
      .option("header", "true")
      .option("inferSchema", "true")
      .option("delimiter", ";")
      .load("src/main/resources/data/tree/arbresalignementparis2010.csv")

    trees.printSchema()
    trees.show()

    trees
      .select(trees("espece"))
      .where(trees("espece") !== "")
      .groupBy(trees("espece"))
      .count() // comptage des espèces
      .sort(trees("espece")) // tri sur la colonne des espèces
      .show()
  }

  "DataFrame - méthode 2 : Comptage des abres de Paris par espèce" should "afficher les résultats" in {

    val trees = sqlc.read
      .format("com.databricks.spark.csv")
      .option("header", "true")
      .option("inferSchema", "true")
      .option("delimiter", ";")
      .load("src/main/resources/data/tree/arbresalignementparis2010.csv")

    trees.printSchema()
    trees.show()

    // Enregistrement du DataFrame en tant que table temporaire
    trees.registerTempTable("tree")

    sqlc
      .sql(
        """SELECT espece, COUNT(*) as count
          |FROM tree
          |WHERE espece != ''
          |GROUP BY espece
          |ORDER BY espece
        """.stripMargin)
      .show()
  }
}
