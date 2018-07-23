package uk.ac.ncl.openlab.intake24.services.fooddb.images

import java.awt.Shape
import java.awt.geom.Path2D

/**
  * Created by Tim Osadchiy on 14/10/2017.
  */
object ShapeFactory {

  def getShapeFromCoordinates(coords: Seq[(Double, Double)]): Shape = {
    val path = new Path2D.Double()
    path.moveTo(coords.head._1, coords.head._2)
    coords.drop(1).foreach { c => path.lineTo(c._1, c._2) }
    path.closePath()
    path
  }

  def getShapeFromFlatCoordinates(coords: Seq[Double]): Option[Shape] =
    groupFlattenCoordinates(coords).map(getShapeFromCoordinates)

  def groupFlattenCoordinates(coords: Seq[Double]): Option[Seq[(Double, Double)]] =
    if (coords.size % 2 != 0) {
      None
    } else {
      Some(coords.grouped(2).map(i => i.head -> i(1)).toSeq)
    }

}
