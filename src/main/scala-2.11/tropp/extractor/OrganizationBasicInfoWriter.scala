package tropp.extractor

import tropp.database.Neo4jClient
import tropp.model._

class OrganizationBasicInfoWriter {

  val neo4jClient = new Neo4jClient

  def saveBasicData(rowStream: Stream[OrganizationBasicInfo]): Unit = {
    case class ParsedRows(voivodships: Set[Voivodeship] = Set.empty,
                          counties: Set[County] = Set.empty,
                          cities: Set[City] = Set.empty,
                          opps: Set[OPP] = Set.empty)

    val parsedBasicData: ParsedRows = rowStream
      .foldLeft(ParsedRows()) { case (current: ParsedRows, row: OrganizationBasicInfo) =>
        val voivodship = Voivodeship("", row.voivodship)
        val county = County("", row.commune, row.voivodship)
        val city = City("", row.city, row.commune, row.voivodship)
        val opp = OPP(row.krs, row.name, 129, 239, 291, 192, row.city, row.commune, row.voivodship)

        ParsedRows(
          current.voivodships + voivodship,
          current.counties + county,
          current.cities + city,
          current.opps + opp
        )
      }


      println("Saving voivodships")
      neo4jClient.saveVoivodeships(parsedBasicData.voivodships)

      println("Saving counties")
      neo4jClient.saveCounties(parsedBasicData.counties)

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
