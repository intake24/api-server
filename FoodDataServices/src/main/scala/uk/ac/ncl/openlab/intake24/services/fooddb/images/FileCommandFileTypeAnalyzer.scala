package uk.ac.ncl.openlab.intake24.services.fooddb.images

import java.nio.file.Path
import scala.sys.process._

class FileCommandFileTypeAnalyzer extends FileTypeAnalyzer {
  def getFileMimeType(path: Path): String = {
    Seq("file","-i","-b",path.toString()).!!.trim()
  }
}