package uk.ac.ncl.openlab.intake24.services.fooddb.images

case class AsServedImageSettings(width: Int, height: Int, thumbnailWidth: Int)

case class SelectionImageSettings(width: Int, height: Int)

case class SourceImageSettings(thumbnailWidth: Int, thumbnailHeight: Int)

case class ImageMapSettings(baseImageWidth: Int, outlineStrokeWidth: Double, outlineColor: (Double, Double, Double), outlineBlurStrength: Double)

case class SlidingScaleSettings(baseImageWidth: Int, fillColor: (Double, Double, Double), blurStrength: Double)

case class ImageProcessorSettings(cmdSearchPath: Option[String], command: Seq[String], source: SourceImageSettings, selection: SelectionImageSettings,
                                  asServed: AsServedImageSettings, imageMap: ImageMapSettings, slidingScale: SlidingScaleSettings)

object ImageProcessorSettings {
  def testSettings() = ImageProcessorSettings(
    Some("/usr/local/bin"),
    Seq("magick", "convert"),
    SourceImageSettings(768, 432),
    SelectionImageSettings(300, 200),
    AsServedImageSettings(654, 436, 80),
    ImageMapSettings(654, 3.0, (0.125, 0.25, 0.5), 6.0),
    SlidingScaleSettings(654, (0.125, 0.25, 0.5), 6.0)
  )
}
