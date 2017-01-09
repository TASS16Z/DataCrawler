package tropp.config

import com.typesafe.config.ConfigFactory

/**
  * Crawler configuration holder.
  *  Field names are same as in application.conf file.
  */
object CrawlerConfig {
  private val config = ConfigFactory.load()
  private val crawlerConfig = config.getConfig("crawler")

  val baseUrl: String = crawlerConfig.getString("baseUrl")
  val entryPointUrl: String = crawlerConfig.getString("entryPointUrl")
}