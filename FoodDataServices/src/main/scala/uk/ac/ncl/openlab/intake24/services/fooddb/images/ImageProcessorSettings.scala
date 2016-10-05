package uk.ac.ncl.openlab.intake24.services.fooddb.images


case class AsServedImageSettings(width: Int, height: Int, thumbnailWidth: Int, thumbnailHeight: Int) 

case class ImageProcessorSettings(asServed: AsServedImageSettings)