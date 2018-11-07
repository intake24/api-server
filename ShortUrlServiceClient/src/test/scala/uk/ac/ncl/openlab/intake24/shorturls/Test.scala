package uk.ac.ncl.openlab.intake24.shorturls

import org.http4s.Uri

object Test extends App {

  override def main(args: Array[String]): Unit = {

    val client = new ShortUrlsHttpClient(ShortUrlsHttpClientConfig(Uri.unsafeFromString("http://google.com")))

    println(client.getShortUrls(ShortUrlsRequest(Seq("a", "b", "c"))).attempt.unsafeRunSync)
  }
}
