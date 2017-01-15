package tropp.crawler

import gigahorse.Gigahorse
import tropp.config.CrawlerConfig

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.Duration

object FormAndAreaCrawler {

  private val areasStringPattern = """<select id="SelectedAreaOfPublic" [^>]+>((?:<option value="[^"]+">[^<]+</option>)+)</select>""".r
  private val formsStringPattern = """<select id="SelectedLawForm" [^>]+>((?:<option value="[^"]+">[^<]+</option>)+)</select>""".r

  private val rowPattern = """<tr class="webgrid-row-style">\s*<td>\s*[0-9]+\s*</td>\s*<td>\s*([0-9]+)\s""".r
  private val optionRowPattern = """<option value="([^"]+)">([^<]+)</option>""".r

  def run(awaitAtMost: Duration)(implicit executionContext: ExecutionContext): (Map[String, Seq[String]], Map[String, Seq[String]]) = {
    Gigahorse.withHttp { http =>
      val request = Gigahorse.url(CrawlerConfig.oppBaseUrl)
      val responseBodyFuture = http.run(request).map(_.body)
      val responseBody = Await.result(responseBodyFuture, awaitAtMost).replace("\r\n", "").replace("\n", "")

      val formsString = formsStringPattern.findAllMatchIn(responseBody).map(_.group(1)).toList.head
      val allForms = optionRowPattern.findAllMatchIn(formsString).map(m => m.group(1) -> m.group(2)).toList

      val areaString = areasStringPattern.findAllMatchIn(responseBody).map(_.group(1)).toList.head
      val allAreas = optionRowPattern.findAllMatchIn(areaString).map(m => m.group(1) -> m.group(2)).toList

      val crawledAreas = allAreas.map { case (areaId, areaName) =>
        val request = Gigahorse
          .url(CrawlerConfig.oppBaseUrl)
          .post(Map(
            "action" -> "search",
            "Krs" -> "",
            "Name" -> "",
            "Regon" -> "",
            "Province" -> "",
            "District" -> "",
            "City" -> "",
            "IssueDtStart" -> "",
            "IssueDtEnd" -> "",
            "instanceYear" -> "2015",
            "SelectedAreaOfPublic[]" -> areaId,
            "page" -> "",
            "sort" -> "",
            "sortdir" -> ""
          ).mapValues(v => List(v)))

        val areaResponse = Await.result(http.run(request).map(_.body), awaitAtMost)
        val rows = rowPattern.findAllMatchIn(areaResponse).map(_.group(1)).toList

        areaName -> rows
      }

      val crawledForms = allForms.map { case (formId, formName) =>
        val request = Gigahorse
          .url(CrawlerConfig.oppBaseUrl)
          .post(Map(
            "action" -> "search",
            "Krs" -> "",
            "Name" -> "",
            "Regon" -> "",
            "Province" -> "",
            "District" -> "",
            "City" -> "",
            "IssueDtStart" -> "",
            "IssueDtEnd" -> "",
            "instanceYear" -> "2015",
            "SelectedLawForm[]" -> s"$formId",
            "page" -> "",
            "sort" -> "",
            "sortdir" -> ""
          ).mapValues(v => List(v)))

        val formResponse = Await.result(http.run(request).map(_.body), awaitAtMost)
        val rows = rowPattern.findAllMatchIn(formResponse).map(_.group(1)).toList

        formName -> rows
      }

      (crawledAreas.toMap, crawledForms.toMap)
    }
  }

}
