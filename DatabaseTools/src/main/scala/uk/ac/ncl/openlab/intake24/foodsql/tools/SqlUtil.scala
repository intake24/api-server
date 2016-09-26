package uk.ac.ncl.openlab.intake24.foodsql.tools

trait SqlUtil {
  def separateSqlStatements(sql: String) =
    // Regex matches on semicolons that neither precede nor follow other semicolons
    sql.split("(?<!;);(?!;)").map(_.trim.replace(";;", ";")).filterNot(_.isEmpty)

  def stripComments(s: String) = """(?m)/\*(\*(?!/)|[^*])*\*/""".r.replaceAllIn(s, "")
}
