package uk.ac.ncl.openlab.intake24.api.client

import org.rogach.scallop.ScallopConf

case class ApiConfiguration(baseUrl: String, userName: String, password: String)

trait ApiConfigurationOptions extends ScallopConf {

  val apiConfigDir = opt[String](required = true)
}