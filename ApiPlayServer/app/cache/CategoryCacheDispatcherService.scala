package cache

trait CategoryCacheDispatcherService {
  def onMainCategoryRecordChanged(action: String => Unit)
  def onLocalCategoryRecordChanged(action: (String, String) => Unit)
  def onCategoryDeleted(action: String => Unit)
  def onCategoryCreated(action: String => Unit)
  
  def onSubcategoryAdded(subcategoryCode: String, categoryCode: String)
  def onFoodAdded(foodCode: String, categoryCode: String)
  def onSubcategoryRemoved(subcategoryCode: String, categoryCode: String)
  def onFoodRemoved(foodCode: String, categoryCode: String)  
}
