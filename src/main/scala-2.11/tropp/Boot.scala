package tropp

import tropp.crawler.OrganizationListCrawler
import tropp.extractor.OrganizationBasicInfoWriter
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

  val writer = new OrganizationBasicInfoWriter

  val rawOrganizationsListOdf = OrganizationListCrawler.run(30.seconds)
  val organizationsList = {
    val parser = new OrganizationListParser(rawOrganizationsListOdf)
    val basicData = parser.rowsStream
    parser.close()
    basicData
  }

  writer.saveBasicData(organizationsList)
  writer.close()
}
