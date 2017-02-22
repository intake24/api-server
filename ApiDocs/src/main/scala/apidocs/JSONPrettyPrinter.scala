package apidocs

import com.google.gson.{GsonBuilder, JsonParser}
import upickle.default._

object JSONPrettyPrinter {
  val gsonBuilder = new GsonBuilder().setPrettyPrinting().create()
  val gsonParser = new JsonParser()

  def asPrettyJSON[T](obj: T)(implicit writer: Writer[T]): String = {
    val je = gsonParser.parse(write(obj))
    gsonBuilder.toJson(je)
  }
}
