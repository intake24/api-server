package uk.ac.ncl.openlab.intake24.systemsql.shortUrl

import javax.inject.Named
import javax.sql.DataSource

import anorm.{BatchSql, NamedParameter, SQL, SqlParser, ~}
import com.google.inject.Inject
import uk.ac.ncl.openlab.intake24.errors.{CreateError, UnexpectedDatabaseError}
import uk.ac.ncl.openlab.intake24.services.systemdb.shortUrls.ShortUrlDataService
import uk.ac.ncl.openlab.intake24.sql.{SqlDataService, SqlResourceLoader}

class ShortUrlDataServiceImpl @Inject()(@Named("intake24_system") val dataSource: DataSource) extends ShortUrlDataService with SqlDataService with SqlResourceLoader {

  override def getShortUrls(longUrls: Seq[String]): Either[UnexpectedDatabaseError, Map[String, String]] = tryWithConnection {
    implicit conn =>
      Right(SQL("SELECT long_url, short_url FROM short_urls WHERE long_url IN ({long_urls})")
        .on('long_urls -> longUrls)
        .executeQuery()
        .as((SqlParser.str(1) ~ SqlParser.str(2)).*)
        .foldLeft(Map[String, String]()) {
          case (m, longUrl ~ shortUrl) => m + (longUrl -> shortUrl)
        })
  }

  override def saveShortUrls(urls: Seq[(String, String)]): Either[UnexpectedDatabaseError, Seq[String]] = tryWithConnection {
    implicit conn =>
      if (urls.nonEmpty) {
        val params = urls.map {
          case (longUrl, shortUrl) => Seq[NamedParameter]('long_url -> longUrl, 'short_url -> shortUrl)
        }

        val updateCount = BatchSql("INSERT INTO short_urls VALUES({long_url},{short_url}) ON CONFLICT ON CONSTRAINT short_urls_unique DO NOTHING",
          params.head, params.tail: _*).execute()

        Right(urls.map(_._1).zip(updateCount).filter(_._2 == 0).map(_._1))
      } else
        Right(Seq())
  }

  override def getLongUrl(shortUrl: String): Either[UnexpectedDatabaseError, Option[String]] = tryWithConnection {
    implicit conn =>
      Right(SQL("SELECT long_url FROM short_urls WHERE short_url={short_url}").on('short_url -> shortUrl).as(SqlParser.str(1).singleOpt))
  }
}
