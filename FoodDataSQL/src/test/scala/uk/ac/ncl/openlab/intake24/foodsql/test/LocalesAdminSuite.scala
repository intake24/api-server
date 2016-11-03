package uk.ac.ncl.openlab.intake24.foodsql.test


import org.scalatest.FunSuite

import uk.ac.ncl.openlab.intake24.Locale
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.LocalesAdminService
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DuplicateCode
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.RecordNotFound

class LocalesAdminSuite (service: LocalesAdminService) extends FunSuite {
  
  /*   def allLocales(): Either[DatabaseError, Seq[Locale]]
  def locale(id: String): Either[LocaleError, Locale]
  def createLocale(data: Locale): Either[DatabaseError, Unit]
  def updateLocale(id: String, data: Locale): Either[LocaleError, Unit]
  def deleteLocale(id: String): Either[LocaleError, Unit]*/
  
  val locale1 = Locale("locale1", "Locale 1", "Великая локаль 1", "en", "en", "gb", None)
  val locale2 = Locale("locale2", "Locale 2", "Великая локаль 2", "en", "en", "gb", Some("locale1"))
  val locale3 = Locale("locale3", "Locale 3", "Великая локаль 3", "en", "en", "gb", None)
  
  val allLocales = Map(locale1.id -> locale1, locale2.id -> locale2, locale3.id -> locale3)
  
  test("Create locales") {
    assert(service.createLocale(locale1).isRight)
    assert(service.createLocale(locale2).isRight)
    assert(service.createLocale(locale3).isRight)
  }
  
  test("Attempt to create a locale with duplicate id") {
    assert(service.createLocale(locale1) === Left(DuplicateCode))
  }
  
  test("Attempt to create a locale with undefined prototype locale") {
    assert(service.createLocale(locale3.copy(prototypeLocale=Some("no_such_locale"))).isLeft)
  }
  
  test("List locales") {
    assert(service.listLocales() === Right(allLocales))
  }
  
  test("Get locale") {
    assert(service.getLocale(locale1.id) === Right(locale1))
    assert(service.getLocale(locale2.id) === Right(locale2))
  }
  
  test("Get undefined locale") {
    assert(service.getLocale("no_such_locale") === Left(RecordNotFound))
  }
  
  test("Attempt to delete undefined locale") {
    assert(service.deleteLocale("no_such_locale") === Left(RecordNotFound))
  }
  
  test("Delete locales") {
    assert(service.deleteLocale(locale1.id).isRight)     
    // locale2 will get cascade deleted
    assert(service.deleteLocale(locale3.id).isRight) 
    
    assert(service.listLocales() === Right(Map()))
  }
  
}
