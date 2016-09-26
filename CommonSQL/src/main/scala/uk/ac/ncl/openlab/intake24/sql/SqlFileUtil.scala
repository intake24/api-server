package uk.ac.ncl.openlab.intake24.sql

trait SqlFileUtil {

  def separateSqlStatements(sql: String) =
    // Regex matches on semicolons that neither precede nor follow other semicolons
    sql.split("(?<!;);(?!;)").map(_.trim.replace(";;", ";")).filterNot(_.isEmpty)

  def stripComments(s: String) = """(?m)/\*(\*(?!/)|[^*])*\*/""".r.replaceAllIn(s, "")

  def loadStatementsFromResource(path: String): Seq[String] =
    separateSqlStatements(stripComments(scala.io.Source.fromInputStream(getClass.getResourceAsStream(path), "utf-8").mkString))

}