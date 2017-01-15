package tropp.pdfExtractor

import org.bytedeco.javacpp.tesseract.TessBaseAPI
import tropp.extractor.PeopleUtils

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

  def readDocumentForOrganization(pathPrefix: String, pagesCount: Int): List[String] = {
    val tsImage = org.bytedeco.javacpp.lept.pixRead(s"${pathPrefix}0.png")

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

  def close() = {
    ocrApi.End()
  }
}
