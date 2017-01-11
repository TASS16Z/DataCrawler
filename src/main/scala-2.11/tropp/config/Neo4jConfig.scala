package tropp.config

import com.typesafe.config.ConfigFactory

/**
  * Neo4j configuration holder.
  *  Field names are same as in application.conf file.
  */
object Neo4jConfig {
  private val config = ConfigFactory.load()
  private val neo4jConfig = config.getConfig("neo4j")

  val host: String = neo4jConfig.getString("host")
  val port: Int = neo4jConfig.getInt("port")
  val user: String = neo4jConfig.getString("user")
  val password: String = neo4jConfig.getString("password")
}
