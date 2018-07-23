package uk.ac.ncl.openlab.intake24.services.fooddb.images

import org.scalatest.FunSuite

/**
  * Created by Tim Osadchiy on 23/07/2018.
  */
class ShapeFactoryTest extends FunSuite {

  test("Should return None for non even seq") {
    assert(ShapeFactory.getShapeFromFlatCoordinates(Seq(1,2,3)).isEmpty)
  }

  test("Should turn flat coordinates to a seq of tuples") {
    val in = Seq(0d,1d,2d,3d,4d,5d,6d,7d)
    val shaped = ShapeFactory.groupFlattenCoordinates(in)
    assert(shaped.exists(l => l.last._2 === in.last))
  }

}
