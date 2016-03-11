package uk.ac.ncl.openlab.intake24.services

import net.scran24.fooddef.Locale

trait LocaleManagementService {
  def list: Seq[Locale]
  def get(id: String): Option[Locale]
  def create(data: Locale)
  def update(id: String, data: Locale)
  def delete(id: String)
}
