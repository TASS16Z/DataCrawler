package tropp.odfparser

import java.io.{File, FileOutputStream}

import org.odftoolkit.odfdom.doc.OdfSpreadsheetDocument
import tropp.model.OrganizationBasicInfo

/**
  * Created by EternalSH on 09.01.2017.
  */
class OrganizationListParser(fileContents: Array[Byte]) {

  private val odsTempFile: File = {
    val file = File.createTempFile("tropp-ods-", "-spreadsheet.ods")
    file.deleteOnExit()
    file
  }

  private val spreadsheet = {
    val outputStream = new FileOutputStream(odsTempFile)
    outputStream.write(fileContents)
    outputStream.close()

    OdfSpreadsheetDocument.loadDocument(odsTempFile.getAbsolutePath)
  }

  val rowsStream: Stream[OrganizationBasicInfo] = {
    import collection.JavaConverters._
    val firstTable = spreadsheet.getTableList.asScala.head

    println(firstTable.getTableName)
    val numberOfRows = firstTable.getRowCount
    println(s"Number of rows: $numberOfRows")


    /* Prepare output stream: create case classes from raw ODF rows, discard anything from an empty row down. */
    val rowsStream = (1 until numberOfRows).toStream.map(firstTable.getRowByIndex)
    rowsStream
      .map { row =>
        val asArray = (1 to 7).toArray
          .map(columnIndex => row.getCellByIndex(columnIndex).getStringValue)

        OrganizationBasicInfo(
          asArray(0), Some(asArray(1)).map(_.trim()).filterNot(_.isEmpty), asArray(2),
          asArray(3), asArray(4), asArray(5), asArray(6)
        )
      }
      .takeWhile(_.krs.trim.nonEmpty)
      .filter(_.krs forall Character.isDigit)
  }

  def close(): Unit = {
    spreadsheet.close()
  }
}
