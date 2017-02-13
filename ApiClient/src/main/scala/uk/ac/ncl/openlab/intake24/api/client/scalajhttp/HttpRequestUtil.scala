package uk.ac.ncl.openlab.intake24.api.client.scalajhttp

import java.nio.file.{Files, Path}

import upickle.default._

import scalaj.http.{Http, MultiPart, StringBodyConnectFunc}

trait HttpRequestUtil {

  protected val connectionTimeoutMs = 1000
  protected val responseTimeoutMs = 30000

  protected def getHttpRequest[T](url: String, method: String, authToken: Option[String], body: T)(implicit writer: Writer[T]) = {
    val req = Http(url).timeout(connectionTimeoutMs, responseTimeoutMs).method(method).charset("utf-8").header("Content-Type", "application/json").copy(connectFunc = StringBodyConnectFunc(writer.write(body).toString))
    authToken match {
      case Some(token) => req.header("X-Auth-Token", token)
      case None => req
    }
  }

  protected def getHttpRequestNoBody(url: String, method: String, authToken: Option[String]) = {
    val req = Http(url).timeout(connectionTimeoutMs, responseTimeoutMs).method(method).charset("utf-8")
    authToken match {
      case Some(token) => req.header("X-Auth-Token", token)
      case None => req
    }
  }

  protected def getHttpFormRequest(url: String, method: String, authToken: Option[String], data: Seq[(String, String)], files: Seq[(String, Path)]) = {
    val req = Http(url).timeout(connectionTimeoutMs, responseTimeoutMs).method(method).charset("utf-8").header("Content-Type", "multipart/form-data")

    val fileParts = files.map {
      case (id, path) =>
        MultiPart(id, path.getFileName.toString, "", Files.readAllBytes(path))
    }

    val reqWithData = req.params(data).postMulti(fileParts: _*)

    authToken match {
      case Some(token) => reqWithData.header("X-Auth-Token", token)
      case None => reqWithData
    }
  }

  protected def getAuthGetRequest[T](url: String, authToken: String, body: T)(implicit writer: Writer[T]) =
    getHttpRequest(url, "GET", Some(authToken), body)

  protected def getAuthGetRequestNoBody(url: String, authToken: String) =
    getHttpRequestNoBody(url, "GET", Some(authToken))

  protected def getAuthDeleteRequest[T](url: String, authToken: String, body: T)(implicit writer: Writer[T]) =
    getHttpRequest(url, "DELETE", Some(authToken), body)

  protected def getAuthDeleteRequestNoBody(url: String, authToken: String) =
    getHttpRequestNoBody(url, "DELETE", Some(authToken))

  protected def getPostRequest[T](url: String, body: T)(implicit writer: Writer[T]) =
    getHttpRequest(url, "POST", None, body)

  protected def getAuthPostRequest[T](url: String, authToken: String, body: T)(implicit writer: Writer[T]) =
    getHttpRequest(url, "POST", Some(authToken), body)

  protected def getAuthPostRequestNoBody(url: String, authToken: String) =
    getHttpRequestNoBody(url, "POST", Some(authToken))

  protected def getAuthPostRequestForm(url: String, authToken: String, data: Seq[(String, String)], files: Seq[(String, Path)]) =
    getHttpFormRequest(url, "POST", Some(authToken), data, files)

}
