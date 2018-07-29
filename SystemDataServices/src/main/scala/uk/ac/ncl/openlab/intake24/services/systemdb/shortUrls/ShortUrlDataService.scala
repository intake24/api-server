package uk.ac.ncl.openlab.intake24.services.systemdb.shortUrls

import uk.ac.ncl.openlab.intake24.errors.{CreateError, LookupError, UnexpectedDatabaseError}

/**
  * Created by Tim Osadchiy on 22/02/2018.
  */

trait ShortUrlDataService {

  def saveShortUrls(urls: Seq[(String, String)]): Either[UnexpectedDatabaseError, Seq[String]]

  def getShortUrls(longUrls: Seq[String]): Either[UnexpectedDatabaseError, Map[String, String]]

  def getLongUrl(shortUrl: String): Either[UnexpectedDatabaseError, Option[String]]
}
