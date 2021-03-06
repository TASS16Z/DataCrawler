package tropp.database

import org.anormcypher.{Cypher, Neo4jConnection, Neo4jREST}
import play.api.libs.ws.ning
import tropp.config.Neo4jConfig
import tropp.model._

import scala.concurrent.ExecutionContext

class Neo4jClient {

  implicit val wsclient = ning.NingWSClient()
  implicit val connection: Neo4jConnection =
    Neo4jREST(Neo4jConfig.host, Neo4jConfig.port, Neo4jConfig.user, Neo4jConfig.password)
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

  def saveAreaOPP(areas: Map[String, Any]): Unit = {
    areas foreach { case (area, _) =>
      Cypher(s""" CREATE (a:PublicBenefitArea {name:'$area'})""").execute()
    }
  }

  def saveFormOPP(forms: Map[String, Any]): Unit = {
    forms foreach { case (form, _) =>
      Cypher(s""" CREATE (a:LegalForm {name:'$form'})""").execute()
    }
  }

  def saveCities(cities: Set[City]): Unit = {
    cities.foreach { city =>
      Cypher(s""" CREATE (ci:City {name:'${city.name}', voivodeship: '${city.voivodeship}', district: '${city.district}'})""").execute()
      Cypher(s""" MATCH (co: District { name: '${city.district}', voivodeship: '${city.voivodeship}'}), (ci: City {name:'${city.name}', voivodeship: '${city.voivodeship}', district: '${city.district}'}) CREATE (ci)-[:LIES_IN]->(co)""".stripMargin).execute()
    }
  }

  def saveDistricts(districts: Set[District]): Unit = {
    districts.foreach { district =>
      Cypher(s""" CREATE (c:District {name:'${district.name}', voivodeship: '${district.voivodeship}'})""").execute()
      Cypher(s""" MATCH (v: Voivodeship { name: '${district.voivodeship}'}), (c: District {name:'${district.name}', voivodeship: '${district.voivodeship}'}) CREATE (c)-[:LIES_IN]->(v)""".stripMargin).execute()
    }
  }

  def saveOPPs(opps: Set[OPP]): Unit = {
    opps.foreach { opp =>
      Cypher(
        s""" CREATE (opp:OPP {name:'${opp.name}', krs: '${opp.krs}', voivodeship: '${opp.voivodeship}', district: '${opp.district}', city: '${opp.city}',
           |salaries: ${opp.salaries}, average_salary: ${opp.averageSalary}, no_of_employees: ${opp.noOfEmployees}, no_of_beneficiaries: ${opp.noOfBeneficiaries}})""".stripMargin).execute()
      Cypher(s""" MATCH (ci: City {name:'${opp.city}', voivodeship: '${opp.voivodeship}', district: '${opp.district}'}), (opp: OPP {krs: '${opp.krs}'}) CREATE (opp)-[:REGISTERED_IN]->(ci)""".stripMargin).execute()

      opp.areas foreach { area =>
        Cypher(s""" MATCH (a:PublicBenefitArea {name:'${area}'}), (opp: OPP {krs: '${opp.krs}'}) CREATE (opp)-[:CATEGORY]->(a)""".stripMargin).execute()
      }

      opp.forms foreach { form =>
        Cypher(s""" MATCH (f:LegalForm {name:'${form}'}), (opp: OPP {krs: '${opp.krs}'}) CREATE (opp)-[:OPERATES_AS]->(f)""".stripMargin).execute()
      }

      // M to N relation :(
      opp.people.foreach { person =>
        Cypher(s""" MATCH (p:Person {name:'$person'}), (opp: OPP {krs: '${opp.krs}'}) CREATE (p)-[:MANAGES]->(opp)""".stripMargin).execute()
      }
    }
  }

  def savePeople(people: Set[Person]): Unit = {
    people.foreach { person =>
      Cypher(
        s""" CREATE (p:Person {name:'${person.name}'})""".stripMargin).execute()
    }
  }

  def saveVoivodeships(voivodeships: Set[Voivodeship]): Unit = {
    voivodeships.foreach { voivodeship =>
      Cypher(s"""create (v:Voivodeship {name:'${voivodeship.name}'})""").execute()
    }
  }

  def close(): Unit = {
    wsclient.close()
  }
}
