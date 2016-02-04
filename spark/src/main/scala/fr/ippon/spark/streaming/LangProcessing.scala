package fr.ippon.spark.streaming

import com.cybozu.labs.langdetect.DetectorFactory

/**
  * Created by ahars on 26/01/2016.
  */
class LangProcessing extends Serializable {

  def detectLanguage(text: String): String = {
    try {
      val detector = DetectorFactory.create()
      detector.append(text)
      detector.detect()
    } catch {
      case e: Exception => "NA"
    }
  }
}

