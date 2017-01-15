package tropp.crawler

import gigahorse.Gigahorse
import tropp.config.CrawlerConfig

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.Duration

object DocumentCrawler {

  def runForOrganization(krs: String, awaitAtMost: Duration)(implicit executionContext: ExecutionContext): Option[Array[Byte]] = {
    Gigahorse.withHttp { http =>
      val request = Gigahorse.url(CrawlerConfig.oppDetailsUrl + krs).post("")
      val responseBodyFuture = http.run(request).map(_.body)
      val responseBody = Await.result(responseBodyFuture, awaitAtMost)

      val pdfUrlPattern = s"""a href="(${CrawlerConfig.oppPdfPrefix}[0-9]+\\${CrawlerConfig.oppPdfSuffix})" """.r
      val pdfAlternativeUrlPattern = s"""a href="(${CrawlerConfig.oppPdfPrefix}[0-9]+\\${CrawlerConfig.oppPdfAlternativeSuffix})" """.r

      val documentId = pdfUrlPattern.findFirstMatchIn(responseBody)
        .map(_.group(1)) match {
          case Some(u) =>
            Some(u)
          case None =>
            pdfAlternativeUrlPattern.findFirstMatchIn(responseBody).map(_.group(1))
        }

      val url = documentId.map(id => CrawlerConfig.oppBaseUrl + id)
      url.map { url =>
        val documentFileFuture = http.run(Gigahorse.url(url))
          .map(_.bodyAsBytes)

        Await.result(documentFileFuture, awaitAtMost)
      }
    }
  }
}
