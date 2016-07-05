package uk.ac.ncl.openlab.intake24.services

import uk.ac.ncl.openlab.intake24.NutrientTable

trait NutrientTableManagementService {
  def list: Seq[NutrientTable]
  def get(id: String): Option[NutrientTable]
  def create(data: NutrientTable)
  def update(id: String, data: NutrientTable)
  def delete(id: String)
}
