package urlShort

import javax.inject.Inject

import uk.ac.ncl.openlab.intake24.errors.ErrorUtils.asFuture
import uk.ac.ncl.openlab.intake24.services.systemdb.shortUrls.ShortUrlDataService

import scala.concurrent.{ExecutionContext, Future}

class ShortUrlCache @Inject()(shortUrlService: ShortUrlService,
                              dataService: ShortUrlDataService,
                              implicit val ec: ExecutionContext) {

  private def createAndCache(remaining: Seq[String], acc: Map[String, String] = Map(), attemptsRemaining: Int = 10): Future[Map[String, String]] =
    if (attemptsRemaining == 0)
      Future.failed(new RuntimeException("Failed to allocate unique short URLs in 10 attempts"))
    else if (remaining.isEmpty)
      Future.successful(acc)
    else
      for (newShortUrls <- shortUrlService.shorten(remaining).map(remaining.zip(_));
           failed <- asFuture(dataService.saveShortUrls(newShortUrls));
           cached <- Future.successful(newShortUrls.filterNot(u => failed.contains(u._1)));
           res <- createAndCache(failed, acc ++ cached, attemptsRemaining - 1)
      ) yield res

  def getShortUrls(longUrls: Seq[String]): Future[Seq[String]] =
    asFuture(dataService.getShortUrls(longUrls)).flatMap {
      existing =>
        val missing = longUrls.filterNot(existing.keySet.contains(_))

        createAndCache(missing).map {
          created =>
            val urlMap = existing ++ created
            longUrls.map(urlMap)
        }
    }

  def resolve(shortUrl: String): Future[Option[String]] = asFuture(dataService.getLongUrl(shortUrl))
}
