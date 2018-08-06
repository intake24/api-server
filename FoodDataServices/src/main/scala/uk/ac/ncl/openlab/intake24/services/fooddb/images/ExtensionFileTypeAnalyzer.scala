package uk.ac.ncl.openlab.intake24.services.fooddb.images

import java.nio.file.Path

class ExtensionFileTypeAnalyzer extends FileTypeAnalyzer {
  def getFileMimeType(path: Path, originalName: String): String = {
    val lc = originalName.toLowerCase()

    if (lc.endsWith(".jpg") || lc.endsWith(".jpeg"))
      "image/jpeg"
    else if (lc.endsWith(".png"))
      "image/png"
    else if (lc.endsWith(".svg"))
      "image/svg+xml"
    else
      "application/octet-stream"
  }
}