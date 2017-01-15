package tropp.extractor

import java.awt.image.RenderedImage
import java.io.{ByteArrayInputStream, File}
import java.nio.file.{Files, Path}
import javax.imageio.ImageIO

import org.ghost4j.document.PDFDocument
import org.ghost4j.renderer.SimpleRenderer
import tropp.crawler.DocumentCrawler
import tropp.database.Neo4jClient
import tropp.model._
import tropp.pdfExtractor.{DocumentParser, OCROPPDetails}

import scala.concurrent.duration._
import scala.util.Random
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.JavaConversions._

class OrganizationBasicInfoWriter {

  val neo4jClient = new Neo4jClient
  val documentParser = new DocumentParser

  // FixMe: move to configuration file.
  val singletonCities = Set(
    "Białystok",
    "Bydgoszcz",
    "Gdańsk",
    "Gorzów Wielkopolski",
    "Katowice",
    "Kielce",
    "Kraków",
    "Lublin",
    "Łódź",
    "Olsztyn",
    "Opole",
    "Poznań",
    "Rzeszów",
    "Szczecin",
    "Toruń",
    "Warszawa",
    "Wrocław",
    "Zielona Góra"
  ).map(_.toLowerCase())

  val voivodeshipsFixes = Map(
    "MAŁOPOLSKA" -> "MAŁOPOLSKIE"
  )

  val tempDirectory: Path = {
    val tmp = Files.createTempDirectory("tropp")
    tmp
  }

  def save(rowStream: Stream[OrganizationBasicInfo], areas: Map[String, Seq[String]], forms: Map[String, Seq[String]]): Unit = {
    case class ParsedRows(voivodeships: Set[Voivodeship] = Set.empty,
                          districts: Set[District] = Set.empty,
                          cities: Set[City] = Set.empty,
                          opps: Set[OPP] = Set.empty,
                          people: Set[Person] = Set.empty)

    val parsedBasicData: ParsedRows = rowStream
      .foldLeft(ParsedRows()) { case (current: ParsedRows, row: OrganizationBasicInfo) =>

        val voivodeshipName = voivodeshipsFixes.getOrElse(row.voivodeship, row.voivodeship)
        val voivodeship = Voivodeship(voivodeshipName)

        val districtName = {
          if(singletonCities.contains(row.city.toLowerCase))
            row.city.replace("M.", "").replace("ST.", "").trim
          else
            row.district
        }
        val district = District(districtName, voivodeshipName)

        val cityName = {
          row.city.replace("M.", "").replace("ST.", "").trim
        }
        val city = City(cityName, districtName, voivodeshipName)

        val details: Option[OCROPPDetails] = DocumentCrawler.runForOrganization(row.krs, 1.minute) map { pdfDocumentRaw: Array[Byte] =>
          val odsTempFile: File = {
            val file = File.createTempFile("tropp-ods-", "-spreadsheet.ods")
            file.deleteOnExit()
            file
          }

          val pdfDocumentRawStream = new ByteArrayInputStream(pdfDocumentRaw)

          val document = new PDFDocument()
          document.load(pdfDocumentRawStream)

          val renderer = new SimpleRenderer()
          // set resolution (in DPI)
          renderer.setResolution(300)

          val images = renderer.render(document)

          (0 until images.size()) map { i =>
            val image = images.get(i).asInstanceOf[RenderedImage]
            ImageIO.write(image, "png", new File(tempDirectory.toString + s"/pdf-$i.png"))
          }

          documentParser.readDocumentForOrganization(tempDirectory.toString + s"/pdf-", images.size())
        }

        val people: List[String] = details.map(_.people).getOrElse(List.empty[String])
        val totalSalaries: Int = details.map(_.totalSalaries).getOrElse(0)
        val avgSalary: Int = details.map(_.avgSalary).getOrElse(0)
        val employeesNo: Int = details.map(_.employeesNo).getOrElse(0)

        val area = areas.find { case (name, opps) => opps.contains(row.krs)} map { case (name, _) => name } getOrElse("")
        val form = forms.find { case (name, opps) => opps.contains(row.krs)} map { case (name, _) => name } getOrElse("")

        // FixMe: Random data hack
        val opp = OPP(row.krs, row.name, totalSalaries, avgSalary, employeesNo, Random.nextInt(10),
          cityName, districtName, voivodeshipName, people, area, form)

        ParsedRows(
          current.voivodeships + voivodeship,
          current.districts + district,
          current.cities + city,
          current.opps + opp,
          current.people ++ people.map(name => Person(name)).toSet
        )
      }

      println("Saving areas")
      neo4jClient.saveAreaOPP(areas)

      println("Saving forms")
      neo4jClient.saveFormOPP(forms)

      println("Saving voivodeships")
      neo4jClient.saveVoivodeships(parsedBasicData.voivodeships)

      println("Saving counties")
      neo4jClient.saveDistricts(parsedBasicData.districts)

      println("Saving cities")
      neo4jClient.saveCities(parsedBasicData.cities)

      println("Saving people")
      neo4jClient.savePeople(parsedBasicData.people)

      println("Saving OPPs")
      neo4jClient.saveOPPs(parsedBasicData.opps)

      println("Done saving data")
  }

  def close(): Unit = {
    neo4jClient.close()
    documentParser.close()
  }
}
