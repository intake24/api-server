package uk.ac.ncl.openlab.intake24.sql.tools.system

import java.io.{File, PrintWriter}

import scala.util.Random

object GenerateRandomUsers extends App {

  private val random = new Random()
  private val alphabet = ('0' to '9').mkString + ('A' to 'Z').mkString + ('a' to 'z').mkString

  def generateRandomString(length: Int): String = {
    val sb = new StringBuffer()

    1.to(length).foreach(_ => sb.append(alphabet.charAt(random.nextInt(alphabet.length))))

    sb.toString
  }

  if (args.length != 1)
    println("Number of user records required")
  else {

    val file = File.createTempFile("intake24", ".csv")

    val pw = new PrintWriter(file)


    pw.println("\"User name\",\"Password\"")


    (1 to args(0).toInt).foreach {
      userId =>
        pw.println(s""""$userId","${generateRandomString(12)}"""")
    }

    pw.close()

    println(file.getAbsolutePath)
  }
}
