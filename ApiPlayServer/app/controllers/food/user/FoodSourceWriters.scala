package controllers.food.user

import uk.ac.ncl.openlab.intake24.services.fooddb.user.{InheritableAttributeSource, SourceLocale, SourceRecord}
import upickle.Js

/**
  * Created by nip13 on 10/03/2017.
  */
object FoodSourceWriters {
  implicit val sourceLocaleWriter = upickle.default.Writer[SourceLocale] {
    case t => t match {
      case SourceLocale.Current(locale) => Js.Obj(("source", Js.Str("current")), ("id", Js.Str(locale)))
      case SourceLocale.Prototype(locale) => Js.Obj(("source", Js.Str("prototype")), ("id", Js.Str(locale)))
    }
  }

  implicit val sourceRecordWriter = upickle.default.Writer[SourceRecord] {
    case t => t match {
      case SourceRecord.CategoryRecord(code) => Js.Obj(("source", Js.Str("category")), ("code", Js.Str(code)))
      case SourceRecord.FoodRecord(code) => Js.Obj(("source", Js.Str("food")), ("code", Js.Str(code)))
      case SourceRecord.NoRecord => Js.Obj(("source", Js.Str("none")))
    }
  }

  implicit val inheritableAttributeSourceWriter = upickle.default.Writer[InheritableAttributeSource] {
    case t => t match {
      case InheritableAttributeSource.FoodRecord(code: String) => Js.Obj(("source", Js.Str("food")), ("code", Js.Str(code)))
      case InheritableAttributeSource.CategoryRecord(code: String) => Js.Obj(("source", Js.Str("category")), ("code", Js.Str(code)))
      case InheritableAttributeSource.Default => Js.Obj(("source", Js.Str("default")))
    }
  }
}
