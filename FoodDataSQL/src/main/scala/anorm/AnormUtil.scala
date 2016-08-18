package anorm

object AnormUtil {
  
  def isNull(row: Row, columnName: String): Boolean = row.get(columnName).get._1 == null
}
