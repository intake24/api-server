package uk.ac.ncl.openlab.intake24.shorturls

import cats.effect.IO

trait ShortUrlsClient {
  def getShortUrls(request: ShortUrlsRequest): IO[ShortUrlsResponse]
}
