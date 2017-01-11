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
      val r1 = Cypher(s""" CREATE (ci:City {name:'${city.name}', voivodeship: '${city.voivodship}', county: '${city.commune}'})""").execute()
      val r2 = Cypher(s""" MATCH (co: County { name: '${city.commune}', voivodeship: '${city.voivodship}'}), (ci: City {name:'${city.name}', voivodeship: '${city.voivodship}', county: '${city.commune}'}) CREATE (ci)-[:LIES_IN]->(co)""".stripMargin).execute()
      println(s"WRITE CITY $city: $r1, $r2")
    }
  }

  def saveCounties(counties: Set[County]): Unit = {
    counties.foreach { county =>
      val r1 = Cypher(s""" CREATE (c:County {name:'${county.name}', voivodeship: '${county.voivodship}'})""").execute()
      val r2 = Cypher(s""" MATCH (v: Voivodeship { name: '${county.voivodship}'}), (c: County {name:'${county.name}', voivodeship: '${county.voivodship}'}) CREATE (c)-[:LIES_IN]->(v)""".stripMargin).execute()
      println(s"WRITE COUNTY $county: $r1, $r2")
    }
  }

  def saveOPPs(opps: Set[OPP]): Unit = {
    opps.foreach { opp =>
      val r1 = Cypher(s""" CREATE (opp:OPP {name:'${opp.name}', krs: '${opp.krs}', voivodeship: '${opp.voivodship}', county: '${opp.county}', city: '${opp.city}'})""").execute()
      val r2 = Cypher(s""" MATCH (ci: City {name:'${opp.city}', voivodeship: '${opp.voivodship}', county: '${opp.county}'}), (opp: OPP {krs: '${opp.krs}'}) CREATE (opp)-[:REGISTERED_IN]->(ci)""".stripMargin).execute()
      println(s"WRITE OPP $opp: $r1, $r2")
    }
  }

  def saveVoivodeships(voivodeships: Set[Voivodeship]): Unit = {
    voivodeships.foreach { voivodship =>
      val r = Cypher(s"""create (${voivodship.id}:Voivodeship {name:'${voivodship.name}'})""").execute()
      println(s"WRITE VOIVODESHIP $voivodship: $r")
    }
  }

  def close(): Unit = {
    wsclient.close()
  }
}
