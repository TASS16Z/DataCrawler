package tropp.database

import org.anormcypher._
import play.api.libs.ws._
import tropp.extractor.{City, County, OPP, Voivodship}

import scala.concurrent.ExecutionContext

class Neo4jClient {

  implicit val wsclient = ning.NingWSClient()
  implicit val connection: Neo4jConnection = Neo4jREST("localhost", 7474, "neo4j", "admin")
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

  def writeOpp(opp: OPP) = {
    Cypher(s"""create (KRS${opp.krs}:OPP {name:"${opp.name.replace("\"", "'")}"})""")
  }

  def writeCity(city: City) = {
    Cypher(s"""create (${city.id}:City {name:'${city.name}'})""")
  }

  def connectCounty(voivodshipId: String, countyId: String) = {
    Cypher(s"""create ($countyId)-[:LIES_IN]->($voivodshipId)""")
  }

  def writeCounty(county: County) = {
    Cypher(s"""create (${county.id}:County {name:'${county.name}'})""")
  }

  def writeVoivodship(voivodship: Voivodship) = {
    Cypher(s"""create (${voivodship.id}:Voivodeship {name:'${voivodship.name}'})""")
  }

  def close(): Unit = {
    wsclient.close()
  }
}
