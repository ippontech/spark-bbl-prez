package fr.ippon.spark.ml

import org.apache.spark.sql.types.DoubleType
import org.apache.spark.sql.{functions, Column, DataFrame, SQLContext}

/**
  * Created by ahars on 29/01/2016.
  */
object Titanic {

  // Fonction de récupération des données d'un fichier de Titanic dans un DataFrame
  def dataframeFromTitanicFile(sqlc: SQLContext, file: String): DataFrame = sqlc.read
    .format("com.databricks.spark.csv")
    .option("header", "true")
    .option("inferSchema", "true")
    .load(file)

  // Fonction de calcul de l'age moyen
  def calcMeanAge(df: DataFrame, inputCol: String): Double = df
    .agg(functions.avg(df(inputCol)))
    .head
    .getDouble(0)

  // Fonction nous donnant l'age ou la moyenne des ages
  def fillMissingAge(df: DataFrame, inputCol: String, outputCol: String, replacementValue: Double): DataFrame = {
    val ageValue: (Any) => Double = age => age match {
      case age: Double => age
      case _ => replacementValue
    }
    df.withColumn(outputCol, functions.callUDF(ageValue, DoubleType, df(inputCol)))
  }
}
