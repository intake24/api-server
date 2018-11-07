package uk.ac.ncl.openlab.intake24.shorturls

case class ShortUrlsRequest(fullUrls: Seq[String])

case class ShortUrlsResponse(shortUrls: Seq[String])
