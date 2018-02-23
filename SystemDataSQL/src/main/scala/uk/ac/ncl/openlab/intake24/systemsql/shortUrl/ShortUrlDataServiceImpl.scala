package uk.ac.ncl.openlab.intake24.systemsql.shortUrl

import anorm.{SQL, SqlParser}
import com.google.inject.Inject
import javax.inject.Named
import javax.sql.DataSource
import uk.ac.ncl.openlab.intake24.errors.{CreateError, LookupError, RecordNotFound}
import uk.ac.ncl.openlab.intake24.services.systemdb.shortUrls.ShortUrlDataService
import uk.ac.ncl.openlab.intake24.sql.{SqlDataService, SqlResourceLoader}

/**
  * Created by Tim Osadchiy on 22/02/2018.
  */
class ShortUrlDataServiceImpl @Inject()(@Named("intake24_system") val dataSource: DataSource) extends ShortUrlDataService with SqlDataService with SqlResourceLoader {

  private val tableName = "short_urls"

  private val insertQ =
    s"""
       |INSERT INTO $tableName (long_url, short_url)
       |VALUES ({long_url}, {short_url})
    """.stripMargin

  private val selectQ = "SELECT short_url FROM short_urls WHERE long_url = {long_url}"

  override def createShortUrl(longUrl: String, shortUrl: String): Either[CreateError, String] = tryWithConnection {
    implicit conn =>
      SQL(insertQ).on(
        'long_url -> longUrl,
        'short_url -> shortUrl
      ).execute()
      Right(shortUrl)
  }

  override def getShortUrl(longUrl: String): Either[LookupError, String] = tryWithConnection {
    implicit conn =>
      SQL(selectQ).on('long_url -> longUrl).executeQuery().as(SqlParser.str("short_url").singleOpt) match {
        case Some(r) => Right(r)
        case None => Left(RecordNotFound(new Exception(s"Short url for $longUrl was not found")))
      }
  }

}
