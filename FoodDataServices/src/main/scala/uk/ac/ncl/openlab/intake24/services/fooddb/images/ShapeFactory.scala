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

  def getShapeFromFlatCoordinates(coords: Seq[Double]): Shape =
    getShapeFromCoordinates(groupFlattenCoordinates(coords))

  def groupFlattenCoordinates(coords: Seq[Double]): Seq[(Double, Double)] =
    coords.grouped(2).filter(_.size == 2).map(i => i(0) -> i(1)).toSeq
}
