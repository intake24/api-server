package uk.ac.ncl.openlab.intake24.sql

trait SqlResourceLoader {
  def sqlFromResource(path: String) = io.Source.fromInputStream(getClass.getResourceAsStream(s"/sql/$path")).mkString
}