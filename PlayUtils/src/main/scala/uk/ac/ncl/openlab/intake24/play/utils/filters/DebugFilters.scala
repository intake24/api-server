package uk.ac.ncl.openlab.intake24.play.utils.filters

import javax.inject.Inject
import play.api.http.{DefaultHttpFilters, EnabledFilters}

class DebugFilters @Inject()(defaultFilters: EnabledFilters, log: OptionsLog) extends DefaultHttpFilters(defaultFilters.filters :+ log: _ *)