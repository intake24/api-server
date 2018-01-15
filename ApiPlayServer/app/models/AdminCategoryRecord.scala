package models

import uk.ac.ncl.openlab.intake24.api.data.admin.{LocalCategoryRecord, MainCategoryRecord}

case class AdminCategoryRecord (main: MainCategoryRecord, local: LocalCategoryRecord)
