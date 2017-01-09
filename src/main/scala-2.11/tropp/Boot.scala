package tropp

import tropp.crawler.OrganizationListCrawler
import tropp.odfparser.OrganizationListParser

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Application entry point:
  *  - set up Neo4j connection
  *  - set up http client
  *  - run crawling
  */
object Boot extends App {

  val rawOrganizationsListOdf = OrganizationListCrawler.run(30.seconds)
  val organizationsList = {
    val parser = new OrganizationListParser(rawOrganizationsListOdf)
    parser.rowsStream.foreach(row => println(s"Organization data row: $row"))
    parser.close()
  }
}
