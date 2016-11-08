package cache

import com.google.inject.Inject
import com.google.inject.Singleton
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.LocalesAdminService
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DeleteError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.CreateError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.UpdateError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LookupError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.UnexpectedDatabaseError
import uk.ac.ncl.openlab.intake24.Locale
import modules.BasicImpl

trait LocalesAdminObserver {
  def onLocaleUpdated(id: String): Unit
  def onLocaleCreated(id: String): Unit
  def onLocaleDeleted(id: String): Unit  
}

trait ObservableLocalesAdminService extends LocalesAdminService {
  
  def addObserver(observer: LocalesAdminObserver): Unit
  
}

@Singleton
class ObservableLocalesAdminServiceImpl @Inject() (@BasicImpl service: LocalesAdminService) extends ObservableLocalesAdminService {
  
  private var observers = List[LocalesAdminObserver]()
  
  def addObserver(observer: LocalesAdminObserver) = observers ::= observer
  
  def listLocales(): Either[UnexpectedDatabaseError, Map[String, Locale]] = service.listLocales()
  def getLocale(id: String): Either[LookupError, Locale] = service.getLocale(id)
  def isTranslationRequired(id: String): Either[LookupError, Boolean] = service.isTranslationRequired(id)
  
  def createLocale(data: Locale): Either[CreateError, Unit] = service.createLocale(data).right.map {
    _ => observers.foreach(_.onLocaleCreated(data.id))
  }
  
  def updateLocale(id: String, data: Locale): Either[UpdateError, Unit] = service.updateLocale(id, data).right.map {
    _ => observers.foreach(_.onLocaleUpdated(id))
  }
  def deleteLocale(id: String): Either[DeleteError, Unit] = service.deleteLocale(id).right.map {
    _ => observers.foreach(_.onLocaleDeleted(id))
  }
}