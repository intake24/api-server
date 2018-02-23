package uk.ac.ncl.openlab.intake24.services.systemdb.shortUrls

import uk.ac.ncl.openlab.intake24.errors.{CreateError, LookupError}

/**
  * Created by Tim Osadchiy on 22/02/2018.
  */

trait ShortUrlDataService {

  def createShortUrl(longUrl: String, shortUrl: String): Either[CreateError, String]

  def getShortUrl(longUrl: String): Either[LookupError, String]

}
