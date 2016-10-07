package uk.ac.ncl.openlab.intake24.services.fooddb.images

import java.nio.file.Path

trait FileTypeAnalyzer {
  def getFileMimeType(path: Path): String
}