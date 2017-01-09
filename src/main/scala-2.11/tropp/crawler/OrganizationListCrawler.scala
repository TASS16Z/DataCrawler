package tropp.crawler

import gigahorse.Gigahorse
import tropp.config.CrawlerConfig

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

/**
  * Organization list crawler.
  *  Extracts full list of most basic data (name, city, KRS) of organizations in ODS format.
  */
object OrganizationListCrawler {

  def run(awaitAtMost: Duration)(implicit executionContext: ExecutionContext): Array[Byte] = {
    Gigahorse.withHttp { http =>
      val request = Gigahorse.url(CrawlerConfig.entryPointUrl)
      val responseBodyFuture = http.run(request).map(_.body)

      val lastOrganizationPattern = """<a href="([^"]+)">Wykaz_organizacji_[^<%]+%_w_[\d]+_r.ods""".r

      val lastOrganizationsListUrlFuture = responseBodyFuture
        .map(lastOrganizationPattern.findFirstMatchIn)
        .map(urlOption => urlOption.getOrElse(sys.error(s"No matching urls found in $urlOption")))
        .map(_.group(1))

      lastOrganizationsListUrlFuture.foreach(url => println(s"Organizations list url: $url"))

      val lastOrganizationsFileFuture = lastOrganizationsListUrlFuture
        .map(url => Gigahorse.url(s"${CrawlerConfig.baseUrl}$url"))
        .flatMap(http.run)
        .map(_.bodyAsBytes)

      Await.result(lastOrganizationsFileFuture, awaitAtMost)
    }
  }
}
