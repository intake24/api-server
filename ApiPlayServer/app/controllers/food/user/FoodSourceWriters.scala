package controllers.food.user

import io.circe.{Encoder, Json}
import uk.ac.ncl.openlab.intake24.services.fooddb.user.{InheritableAttributeSource, SourceLocale, SourceRecord}

object FoodSourceWriters {
  implicit val sourceLocaleWriter = new Encoder[SourceLocale] {
    def apply(a: SourceLocale): Json = a match {
      case SourceLocale.Current(locale) => Json.obj(("source", Json.fromString("current")), ("id", Json.fromString(locale)))
      case SourceLocale.Prototype(locale) => Json.obj(("source", Json.fromString("prototype")), ("id", Json.fromString(locale)))
    }
  }

  implicit val sourceRecordWriter = new Encoder[SourceRecord] {
    def apply(a: SourceRecord): Json = a match {
      case SourceRecord.CategoryRecord(code) => Json.obj(("source", Json.fromString("category")), ("code", Json.fromString(code)))
      case SourceRecord.FoodRecord(code) => Json.obj(("source", Json.fromString("food")), ("code", Json.fromString(code)))
      case SourceRecord.NoRecord => Json.obj(("source", Json.fromString("none")))
    }
  }

  implicit val inheritableAttributeSourceWriter = new Encoder[InheritableAttributeSource] {
    def apply(a: InheritableAttributeSource): Json = a match {

      case InheritableAttributeSource.FoodRecord(code: String) => Json.obj(("source", Json.fromString("food")), ("code", Json.fromString(code)))
      case InheritableAttributeSource.CategoryRecord(code: String) => Json.obj(("source", Json.fromString("category")), ("code", Json.fromString(code)))
      case InheritableAttributeSource.Default => Json.obj(("source", Json.fromString("default")))

    }
  }
}
