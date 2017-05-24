package uk.ac.ncl.openlab.intake24.sql.tools.food.localisation.newzealand

import org.rogach.scallop.ScallopConf

object GenerateStandardUnitsList extends App {

  val options = new ScallopConf(args) {
    val srcFile = opt[String](required = true)
  }

  options.verify()

  val lines = scala.io.Source.fromFile(options.srcFile()).getLines().toSeq

  val buf = scala.collection.mutable.Buffer[String]()

  lines.filter(_.trim.nonEmpty).foreach {
    line =>
      val name = line.split("=")(0).trim
      if (name.endsWith("_estimate_in"))
        buf.append(name.replace("_estimate_in", ""))
  }

  def q(s: String) = "\"" + s + "\""

  println(buf.toSet.map(q))

  buf.sorted.foreach(println)

}
