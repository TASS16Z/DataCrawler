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


  def saveCities(cities: Set[City]): Unit = {
    cities.foreach { city =>
      val r1 = Cypher(s""" CREATE (ci:City {name:'${city.name}', voivodeship: '${city.voivodeship}', district: '${city.district}'})""").execute()
      val r2 = Cypher(s""" MATCH (co: District { name: '${city.district}', voivodeship: '${city.voivodeship}'}), (ci: City {name:'${city.name}', voivodeship: '${city.voivodeship}', district: '${city.district}'}) CREATE (ci)-[:LIES_IN]->(co)""".stripMargin).execute()
      println(s"WRITE CITY $city: $r1, $r2")
    }
  }

  def saveDistricts(districts: Set[District]): Unit = {
    districts.foreach { district =>
      val r1 = Cypher(s""" CREATE (c:District {name:'${district.name}', voivodeship: '${district.voivodship}'})""").execute()
      val r2 = Cypher(s""" MATCH (v: Voivodeship { name: '${district.voivodship}'}), (c: District {name:'${district.name}', voivodeship: '${district.voivodship}'}) CREATE (c)-[:LIES_IN]->(v)""".stripMargin).execute()
      println(s"WRITE DISTRICT $district: $r1, $r2")
    }
  }

  def saveOPPs(opps: Set[OPP]): Unit = {
    opps.foreach { opp =>
      val r1 = Cypher(s""" CREATE (opp:OPP {name:'${opp.name}', krs: '${opp.krs}', voivodeship: '${opp.voivodeship}', county: '${opp.district}', city: '${opp.city}'})""").execute()
      val r2 = Cypher(s""" MATCH (ci: City {name:'${opp.city}', voivodeship: '${opp.voivodeship}', county: '${opp.district}'}), (opp: OPP {krs: '${opp.krs}'}) CREATE (opp)-[:REGISTERED_IN]->(ci)""".stripMargin).execute()
      println(s"WRITE OPP $opp: $r1, $r2")
    }
  }

  def saveVoivodeships(voivodeships: Set[Voivodeship]): Unit = {
    voivodeships.foreach { voivodship =>
      val r = Cypher(s"""create (v:Voivodeship {name:'${voivodship.name}'})""").execute()
      println(s"WRITE VOIVODESHIP $voivodship: $r")
    }
  }

  def close(): Unit = {
    wsclient.close()
  }
}
