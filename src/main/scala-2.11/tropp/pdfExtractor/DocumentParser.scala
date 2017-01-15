package tropp.pdfExtractor

import org.bytedeco.javacpp.tesseract.TessBaseAPI
import tropp.extractor.PeopleUtils

case class OCROPPDetails(people: List[String], totalSalaries: Int, avgSalary: Int, employeesNo: Int)

class DocumentParser {

  private val ocrApi = {
    val api = new TessBaseAPI()

    if (api.Init(null, "pol") != 0) {
      System.err.println("Could not initialize tesseract.")
      System.exit(1)
    }

    // AUTO_OSD recognition mode.
    api.SetPageSegMode(1)
    api
  }

  def readDocumentForOrganization(pathPrefix: String, pagesCount: Int): OCROPPDetails = {
    val management: List[String] = {
      val tsImage = org.bytedeco.javacpp.lept.pixRead(s"${pathPrefix}0.png")
      ocrApi.SetPageSegMode(1)
      ocrApi.SetImage(tsImage)
      ocrApi.SetRectangle(1029, 2365, 485, 1000)

      // Get OCR result
      val outText = ocrApi.GetUTF8Text()
      val outString = outText.getString

      // Destroy used object and release memory
      outText.deallocate()
      org.bytedeco.javacpp.lept.pixDestroy(tsImage)

      val text = outString.split("\n").map(_.trim).filter(_.nonEmpty).toList.takeWhile(_ != "Imię i nazwisko")
      PeopleUtils.parsePersonNames(text)
    }

    //        ocrApi.SetRectangle(1300, 50, 800, 3400)

    val fullTextStream = {
      (0 until pagesCount).toStream.map { i: Int =>
        val tsImage = org.bytedeco.javacpp.lept.pixRead(s"$pathPrefix$i.png")
        // SPARSE_TEXT_OSD
        ocrApi.SetPageSegMode(12)
        ocrApi.SetImage(tsImage)

        val outText = ocrApi.GetUTF8Text()
        val outString = outText.getString
        outText.deallocate()
        org.bytedeco.javacpp.lept.pixDestroy(tsImage)

        outString
      }
    }

    val totalSalaries: Int = {
      val foundPage = fullTextStream.find(_.contains("Łączna kwota wynagrodzeń"))
      val foundNo: Option[String] =
        foundPage.flatMap(DocumentParser.totalIncomePattern.findFirstMatchIn(_).map(_.group(1)))
      foundNo.map(_.replace(",", "").replace(" ", "").toFloat.toInt).getOrElse(0)
    }

    val employeesNo: Int = {
      val foundPage = fullTextStream.find(_.contains("Liczba osób zatrudnionych w organizacji"))
      val foundNo: Option[String] =
        foundPage.flatMap(DocumentParser.employeesNoPattern.findFirstMatchIn(_).map(_.group(1)))
      foundNo.map(_.replace(",", "").replace(" ", "").toFloat.toInt).getOrElse(0)
    }

    val avgSalary = {
      if(employeesNo == 0)
        0
      else
        totalSalaries / employeesNo
    }

    OCROPPDetails(management, totalSalaries, avgSalary, employeesNo)
  }

  def close() = {
    ocrApi.End()
  }
}

object DocumentParser {
  val totalIncomePattern = """1.Łączna kwota wynagrodzeń [^0-9]+([0-9,.]+) zł""".r
  val employeesNoPattern = """Liczba osób zatrudnionych w organizacji [^0-9]+([0-9.,]+) osób""".r
}
