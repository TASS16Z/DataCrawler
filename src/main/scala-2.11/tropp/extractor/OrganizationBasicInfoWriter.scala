package tropp.extractor

import tropp.database.Neo4jClient
import tropp.model._

import scala.util.Random

class OrganizationBasicInfoWriter {

  val neo4jClient = new Neo4jClient

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

  def saveBasicData(rowStream: Stream[OrganizationBasicInfo]): Unit = {
    case class ParsedRows(voivodeships: Set[Voivodeship] = Set.empty,
                          districts: Set[District] = Set.empty,
                          cities: Set[City] = Set.empty,
                          opps: Set[OPP] = Set.empty)

    val parsedBasicData: ParsedRows = rowStream
      .foldLeft(ParsedRows()) { case (current: ParsedRows, row: OrganizationBasicInfo) =>
        val voivodeshipName = voivodeshipsFixes.getOrElse(row.voivodeship, row.voivodeship)
        val voivodeship = Voivodeship(voivodeshipName)

        val districtName = {
          if(singletonCities.contains(row.city.toLowerCase))
            row.city
          else
            row.district
        }

        val district = District(districtName, row.voivodeship)
        val city = City(row.city, row.city, row.voivodeship)

        // FixMe: Random data hack
        val opp = OPP(row.krs, row.name, Random.nextInt(), Random.nextInt(), Random.nextInt(), Random.nextInt(),
          row.city, districtName, voivodeshipName)

        ParsedRows(
          current.voivodeships + voivodeship,
          current.districts + district,
          current.cities + city,
          current.opps + opp
        )
      }


      println("Saving voivodeships")
      neo4jClient.saveVoivodeships(parsedBasicData.voivodeships)

      println("Saving counties")
      neo4jClient.saveDistricts(parsedBasicData.districts)

      println("Saving cities")
      neo4jClient.saveCities(parsedBasicData.cities)

      println("Saving OPPs")
      neo4jClient.saveOPPs(parsedBasicData.opps)

      println("Done saving basic data")
  }

  def close(): Unit = {
    neo4jClient.close()
  }
}
