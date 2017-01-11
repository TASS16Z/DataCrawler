package tropp.extractor

import org.anormcypher.Neo4jTransaction
import tropp.database.Neo4jClient
import tropp.model.OrganizationBasicInfo

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

case class City(id: String, name: String, commune: String)
case class County(id: String, name: String, voivodship: String)
case class Voivodship(id: String, name: String)
case class OPP(krs: String, name: String, salaries: Int, averageSalary: Int, noOfEmployees: Int, noOfBeneficiaries: Int)

class OrganizationBasicInfoWriter {

  val neo4jClient = new Neo4jClient

  def saveBasicData(rowStream: Stream[OrganizationBasicInfo]): Unit = {
    def generateId[A](input: Set[A], mapper: (A, Int) => A): Set[A] = {
      input
        .zipWithIndex
        .map { case (value, index) => mapper(value, index) }
    }

    case class ParsedRows(voivodships: Set[Voivodship] = Set.empty,
                          counties: Set[County] = Set.empty,
                          cities: Set[City] = Set.empty,
                          opps: Set[OPP] = Set.empty)

    val parsedBasicData: ParsedRows = rowStream
      .foldLeft(ParsedRows()){ case (current: ParsedRows, row: OrganizationBasicInfo) =>
          val voivodship = Voivodship("", row.voivodship)
          val county = County("", row.commune, row.voivodship)
          val city = City("", row.city, row.commune)
          val opp = OPP(row.krs, row.name, 129, 239, 291, 192)

          ParsedRows(
            current.voivodships + voivodship,
            current.counties + county,
            current.cities + city,
            current.opps + opp
          )
      }

    val indexedBasicData = ParsedRows(
      generateId[Voivodship](parsedBasicData.voivodships, (v, index) => v.copy(id = s"voi$index")),
      generateId[County](parsedBasicData.counties, (v, index) => v.copy(id = s"county$index")),
      generateId[City](parsedBasicData.cities, (v, index) => v.copy(id = s"cit$index")),
      parsedBasicData.opps
    )

    import neo4jClient.connection
    import neo4jClient.wsclient
    val importResult = Neo4jTransaction.withTx { implicit tx =>
      println("Saving voivodships")
      indexedBasicData.voivodships.foreach(neo4jClient.writeVoivodship(_)())
      println("Done saving voivodships")

      println("Saving counties")
      indexedBasicData.counties.foreach(neo4jClient.writeCounty(_)())
      println("Done saving counties")

      println("Saving cities")
      indexedBasicData.cities.foreach(neo4jClient.writeCity(_)())
      println("Done saving cities")

      println("Saving OPPs")
      indexedBasicData.opps.foreach(neo4jClient.writeOpp(_)())
      println("Done saving OPPs")

      val voivodshipsMap: Map[String, String] = indexedBasicData.voivodships.map(voivodship => voivodship.name -> voivodship.id).toMap
      val countiesMap: Map[String, String] = indexedBasicData.counties.map(county => county.name -> county.id).toMap

      val countyToVoivodship = indexedBasicData.counties.flatMap(county => voivodshipsMap.get(county.voivodship).map(county.id -> _))
      countyToVoivodship.foreach { case (countyId, voivodshipId) =>
        neo4jClient.connectCounty(voivodshipId, countyId)()
      }

      val cityToCounty = indexedBasicData.cities.flatMap(city => countiesMap.get(city.commune).map(city.id -> _))
    }

    Await.result(importResult, 5.minutes)


  }

  def close(): Unit = {
    neo4jClient.close()
  }
}
