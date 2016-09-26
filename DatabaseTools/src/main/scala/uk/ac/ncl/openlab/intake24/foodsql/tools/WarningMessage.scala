package uk.ac.ncl.openlab.intake24.foodsql.tools

import java.io.BufferedReader
import java.io.InputStreamReader

trait WarningMessage {
  def displayWarningMessage(message: String) = {
    
    val len = message.length()
    
    val bar = Seq.fill(len)('=').mkString
    
    println(bar)
    println(message)
    println(bar)
    println()
    println("Are you sure you wish to continue? Type 'yes' to proceed, 'no' or Control+C to abort.")
    
    var proceed = false;

    val reader = new BufferedReader(new InputStreamReader(System.in))

    while (!proceed) {
      val input = reader.readLine()
      if (input == "yes") proceed = true;
      if (input == "no") System.exit(0);
    }
  }
}
