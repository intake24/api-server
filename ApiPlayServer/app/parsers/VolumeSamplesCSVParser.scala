package parsers

import au.com.bytecode.opencsv.CSVReader
import uk.ac.ncl.openlab.intake24.VolumeSample

import java.io.{File, FileReader}
import scala.collection.JavaConverters._

object VolumeSamplesCSVParser {

  private case class VolumeSampleRow(scaleId: Int, height: Double, volume: Double)

  private def parseRow(rowIndex: Int, row: Array[String]): Either[String, VolumeSampleRow] =
    if (row.length < 3)
      Left(s"""Too few columns in row ${rowIndex + 1}: 3 required, only ${row.length} found""")
    else {
      val objectIdString = row(0)
      val heightString = row(1)
      val volumeString = row(2)

      try {
        val objectId = Integer.parseInt(objectIdString)
        val height = java.lang.Double.parseDouble(heightString)
        val volume = java.lang.Double.parseDouble(volumeString)

        Right(VolumeSampleRow(objectId, height, volume))
      } catch {
        case e: NumberFormatException => Left(s"Invalid number in row ${rowIndex + 1}: ${e.getMessage}")
      }
    }

  private def parseRows(rows: Seq[Array[String]]): Either[String, Seq[VolumeSampleRow]] = {
    val z: Either[String, Seq[VolumeSampleRow]] = Right(Seq())
    rows.zipWithIndex.foldLeft(z) {
      case (acc, (row, rowIndex)) =>
        for (parsedRows <- acc;
             parsedRow <- parseRow(rowIndex, row))
        yield parsedRow +: parsedRows
    }
  }

  def parseFile(file: File): Either[String, Map[Int, Seq[VolumeSample]]] = {
    val reader = new CSVReader(new FileReader(file))

    try {

      val header = reader.readNext()
      val rows = reader.readAll().asScala

      if (rows.size < 1)
        Left("File is empty or in incorrect format")
      else {
        parseRows(rows).map {
          records =>

            records.groupBy(_.scaleId).map {
              case (scaleId, row) =>
                val samples = row.map(sample => VolumeSample(sample.height, sample.volume))
                val max = samples.map(_.fl).max
                if (max > 0.0001) {
                  val normalised = samples.map(sample =>VolumeSample(sample.fl / max, sample.v))
                  (scaleId, normalised)
                } else {
                  (scaleId, samples)
                }
            }
        }
      }
    } catch {
      case e: Exception => Left(s"""${e.getClass.getSimpleName}: ${e.getMessage}""")
    } finally {
      reader.close()
    }
  }
}
