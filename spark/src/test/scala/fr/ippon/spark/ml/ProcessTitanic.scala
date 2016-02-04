package fr.ippon.spark.ml

import org.apache.spark.ml.{PipelineModel, Pipeline}
import org.apache.spark.ml.classification.{RandomForestClassificationModel, RandomForestClassifier}
import org.apache.spark.ml.feature.{VectorAssembler, StringIndexer}
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.SQLContext
import org.elasticsearch.spark.sql.EsSparkSQL
import org.scalatest.{BeforeAndAfter, Matchers, FlatSpec}

/**
  * Created by ahars on 29/01/2016.
  */
class ProcessTitanic extends FlatSpec with Matchers with BeforeAndAfter {

  private var sc: SparkContext = _
  private var sqlc: SQLContext = _

  before {
    val sparkConf = new SparkConf()
      .setAppName("processTitanic")
      .setMaster("local[*]")
      .set("es.nodes", "localhost:9200")
      .set("es.index.auto.create", "true")

    sc = new SparkContext(sparkConf)
    sqlc = new SQLContext(sc)
  }

  after {
    sc.stop()
  }

  "ML - Apprentissage supervisé : Prédiction du nombre de survivants du Titanic" should "send results to ElasticSearch" in {

    // Récupération des données
    val trainDf = Titanic.dataframeFromTitanicFile(sqlc, "src/main/resources/data/titanic/train.csv")
    val testDf = Titanic.dataframeFromTitanicFile(sqlc, "src/main/resources/data/titanic/test.csv")
    val expectedDf = Titanic.dataframeFromTitanicFile(sqlc, "src/main/resources/data/titanic/gendermodel.csv")

    println
    println("Jeu d'entrainement")
    trainDf.show()

    println
    println("Jeu de test")
    testDf.show()

    println
    println("Label à trouver")
    expectedDf.show()

    println
    println("Jeu d'entrainement : " + trainDf.count() + " passagers")
    println("Jeu de test : " + testDf.count() + " passagers")

    println
    println("----------------------------------------------")
    println

    // Data Cleansing

    // Correction des valeurs erronées sur l'age des passagers
    val meanAge = Titanic.calcMeanAge(trainDf, "Age")

    println("Age moyen calculé : " + meanAge)

    val trainWithCorrectionsDf = Titanic.fillMissingAge(trainDf, "Age", "Age_cleaned", meanAge)
    val testWithCorrectionsDf = Titanic.fillMissingAge(testDf, "Age", "Age_cleaned", meanAge)

    println
    println("Jeu d'entrainement avec l'âge corrigé")
    trainWithCorrectionsDf.show()

    println
    println("Jeu de test avec l'âge corrigé")
    testWithCorrectionsDf.show()

    println
    println("----------------------------------------------")
    println

    // Features Engineering

    // Conversion de la colonne "Survived" en colonne numérique "Label" utilisée par l'algorithme de ML
    val labelIndexModel = new StringIndexer()
      .setInputCol("Survived")
      .setOutputCol("label")
      .fit(trainWithCorrectionsDf)

    val trainWithLabelDf = labelIndexModel.transform(trainWithCorrectionsDf)

    println
    println("Jeu d'apprentissage")
    trainWithLabelDf.show()

    // Conversion de la colonne "Sex" en colonne numérique "Sex_indexed"
    val sexIndexModel = new StringIndexer()
      .setInputCol("Sex")
      .setOutputCol("Sex_indexed")

    // Assemblage des Features dans un tableau
    val vectorizedFeaturesModel = new VectorAssembler()
      .setInputCols(Array("Pclass", "Sex_indexed", "Age_cleaned"))
      .setOutputCol("features")

    // Instanciation de l'algorithme supervisé de ML des Random Forests implémenté dans Spark ML
    val randomForestAlgo = new RandomForestClassifier()

    println
    println("----------------------------------------------")
    println

    // Pipeline Building
    val pipeline = new Pipeline()
      .setStages(Array(
        sexIndexModel,
        vectorizedFeaturesModel,
        randomForestAlgo
      ))

    // Training of the Model
    val model = pipeline.fit(trainWithLabelDf)

    println("Le Modèle de ML généré par l'algorithme des Random Forests")
    println(model.asInstanceOf[PipelineModel].stages(2).asInstanceOf[RandomForestClassificationModel].toDebugString)

    println
    println("----------------------------------------------")
    println

    // Test du model
    val predictions = model.transform(testWithCorrectionsDf)

    println("Application du Modèle de ML sur le jeu de test")
    predictions.show()

    println
    println("Comparaison des prédictions avec les valeurs réelles")

    val comparison = predictions
      .join(expectedDf, "PassengerId")
      .select("PassengerId", "Pclass", "Sex", "Age_cleaned", "prediction", "Survived")

    // Affichage des prédictions par rapport au label
    comparison.select("Survived", "prediction")
      .groupBy("Survived", "prediction")
      .count()
      .show()

    EsSparkSQL.saveToEs(comparison, "ml/comparisons")
  }
}
