package uk.ac.ncl.openlab.intake24.api.client.scalajhttp

import java.nio.file.{Files, Path}

import scalaj.http.{Http, MultiPart}
import upickle.default._

trait HttpRequestUtil {

  def getSimpleHttpAuthGetRequest(url: String, authToken: String) =
    Http(url).timeout(1000, 30000).method("GET").charset("utf-8").header("X-Auth-Token", authToken)

  def getHttpRequest[T](url: String, method: String, authToken: Option[String], body: T)(implicit writer: Writer[T]) = {
    val req = Http(url).timeout(1000, 30000).method(method).charset("utf-8").header("Content-Type", "application/json").postData(writer.write(body).toString)
    authToken match {
      case Some(token) => req.header("X-Auth-Token", token)
      case None => req
    }
  }

  def getHttpFormRequest(url: String, method: String, authToken: Option[String], data: Seq[(String, String)], files: Seq[(String, Path)]) = {
    val req = Http(url).timeout(1000, 30000).method(method).charset("utf-8").header("Content-Type", "multipart/form-data")

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

  def getAuthPostRequest[T](url: String, authToken: String, body: T)(implicit writer: Writer[T]) = getHttpRequest(url, "POST", Some(authToken), body)

  def getAuthFormPostRequest(url: String, authToken: String, data: Seq[(String, String)], files: Seq[(String, Path)]) =
    getHttpFormRequest(url, "POST", Some(authToken), data, files)

  def getPostRequest[T](url: String, body: T)(implicit writer: Writer[T]) = getHttpRequest(url, "POST", None, body)
}
