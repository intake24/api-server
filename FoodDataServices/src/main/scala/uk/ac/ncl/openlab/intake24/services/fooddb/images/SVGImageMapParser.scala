package uk.ac.ncl.openlab.intake24.services.fooddb.images


import org.apache.batik.anim.dom.{SAXSVGDocumentFactory, SVGOMDocument}
import org.apache.batik.parser.{AWTPathProducer, AWTTransformProducer}
import org.apache.batik.util.XMLResourceDescriptor
import org.slf4j.LoggerFactory
import org.w3c.dom.svg.SVGSVGElement
import org.w3c.dom.{Element, Node}

import java.awt.Shape
import java.awt.geom.{AffineTransform, PathIterator}
import java.io.StringReader
import scala.collection.mutable

case class AWTImageMap(navigation: Seq[Int], outlines: Map[Int, Shape], aspect: Double) {
  def getCoordsArray(objectId: Int): Seq[Double] = {
    val i = outlines(objectId).getPathIterator(null, 0.005)

    val result = mutable.Buffer[Double]()

    while (!i.isDone) {
      {
        val coords = new Array[Float](6)
        val segType = i.currentSegment(coords)
        if (segType == PathIterator.SEG_MOVETO || segType == PathIterator.SEG_LINETO) {
          result.append(coords(0))
          result.append(coords(1))
        }

      }
      i.next()
    }

    result
  }
}

case class AWTOutline(shape: Shape, aspect: Double) {
  val coordsArray: Seq[Double] = {
    val i = shape.getPathIterator(null, 0.005)

    val result = mutable.Buffer[Double]()

    while (!i.isDone) {
      {
        val coords = new Array[Float](6)
        val segType = i.currentSegment(coords)
        if (segType == PathIterator.SEG_MOVETO || segType == PathIterator.SEG_LINETO) {
          result.append(coords(0))
          result.append(coords(1))
        }

      }
      i.next()
    }

    result
  }
}

class SVGImageMapParser {

  private implicit class DocExt(doc: SVGOMDocument) {
    def getElementsByTagNameAsSeq(name: String): IndexedSeq[Element] = {
      val nl = doc.getElementsByTagName(name)
      Range(0, nl.getLength).map(index => nl.item(index).asInstanceOf[Element])
    }
  }

  private implicit class ElemExt(elem: Element) {
    def getElementsByTagNameAsSeq(name: String): IndexedSeq[Element] = {
      val nl = elem.getElementsByTagName(name)
      Range(0, nl.getLength).map(index => nl.item(index).asInstanceOf[Element])
    }
  }

  val logger = LoggerFactory.getLogger(classOf[SVGImageMapParser])

  def getOutlines(scale: Double, doc: SVGOMDocument, idParser: String => Option[Int]): Map[Int, Shape] = {

    def getOutlineFromPathElement(scale: Double, path: Element): Shape = {

      def getTransformToRootSpace(node: Node, t: AffineTransform): AffineTransform = {
        if (node == null)
          t
        else {
          val nodeTransform =
            for (attrs <- Option(node.getAttributes);
                 transformNode <- Option(attrs.getNamedItem("transform"))) yield
              AWTTransformProducer.createAffineTransform(transformNode.getTextContent)

          nodeTransform match {
            case Some(transform) => {
              val tCopy = new AffineTransform(t)
              // Normal transform concatenation order is LIFO, which makes sense going from parent to child,
              // but since we are traversing the tree from child to parent we need to reverse it
              tCopy.preConcatenate(transform)
              getTransformToRootSpace(node.getParentNode, tCopy)
            }
            case None => getTransformToRootSpace(node.getParentNode, t)
          }
        }
      }

      val shape = AWTPathProducer.createShape(new StringReader(path.getAttribute("d")), 0)

      val transform = getTransformToRootSpace(path, new AffineTransform())

      // Scale needs to also be pre-concatenated so it is applied last
      transform.preConcatenate(AffineTransform.getScaleInstance(scale, scale))

      transform.createTransformedShape(shape)
    }

    val pathElements = doc.getElementsByTagNameAsSeq("path")
    logger.debug(s"${pathElements.length} path element(s) in SVG")

    pathElements.foldLeft(Map[Int, Element]()) {
      case (acc, candidateElement) =>
        val elementId = candidateElement.getAttribute("id")
        idParser(elementId) match {
          case Some(objectId) =>
            logger.debug(s"Using $elementId")
            acc + (objectId -> candidateElement)
          case None =>
            logger.debug(s"Skipping $elementId")
            acc
        }
    }.map {
      case (objectId, element) => (objectId -> getOutlineFromPathElement(scale, element))
    }
  }

  def getNavigation(doc: SVGOMDocument): Option[Seq[Int]] = {
    doc.getElementsByTagNameAsSeq("text").find(_.getAttribute("id") == "navigation").map {
      navElement =>
        val nl = navElement.getElementsByTagName("tspan")
        Range(0, nl.getLength).foldLeft(Seq[Int]()) {
          case (acc, index) =>
            val e = nl.item(index).asInstanceOf[Element]
            acc ++ e.getTextContent.split("\\s+").map(_.toInt)
        }
    }
  }

  def parseSvg(path: String): SVGOMDocument = {
    val parser = XMLResourceDescriptor.getXMLParserClassName
    val f = new SAXSVGDocumentFactory(parser)
    val uri = "file:///" + path
    f.createDocument(uri).asInstanceOf[SVGOMDocument]
  }

  def getSVGWidth(svgElement: SVGSVGElement) = {
    if (svgElement.hasAttribute("viewBox"))
      svgElement.getViewBox.getBaseVal.getWidth
    else
      svgElement.getWidth.getBaseVal.getValue
  }

  def getSVGHeight(svgElement: SVGSVGElement) = {
    if (svgElement.hasAttribute("viewBox"))
      svgElement.getViewBox.getBaseVal.getHeight
    else
      svgElement.getHeight.getBaseVal.getValue
  }

  def parseImageMap(svgPath: String): Either[Exception, AWTImageMap] = {
    logger.debug(s"Trying to parse image map from $svgPath")

    try {
      val svgDoc = parseSvg(svgPath)

      val svgElem = svgDoc.getRootElement

      val (width, height) = (getSVGWidth(svgElem), getSVGHeight(svgElem))

      val scale = 1.0 / width

      val aspect = width / height

      val outlines = getOutlines(scale, svgDoc, s => if (s.matches("area_[0-9]+")) Some(s.substring(5).toInt) else None)

      val navigation = getNavigation(svgDoc).getOrElse(outlines.keySet.toSeq.sorted)

      Right(AWTImageMap(navigation, outlines, aspect))
    } catch {
      case e: Exception => Left(e)
    }
  }

  def parseOutline(svgPath: String): Either[Exception, AWTOutline] = {
    logger.debug(s"Trying to parse single outline from $svgPath")

    try {
      val svgDoc = parseSvg(svgPath)

      val svgElem = svgDoc.getRootElement

      val (width, height) = (getSVGWidth(svgElem), getSVGHeight(svgElem))

      val scale = 1.0 / width

      val aspect = width / height

      val outlines = getOutlines(scale, svgDoc, s => if (s == "outline") Some(0) else None)

      if (outlines.size != 1)
        Left(new RuntimeException("No path with the id 'outline' found in the SVG file"))
      else
        Right(AWTOutline(outlines(0), aspect))
    } catch {
      case e: Exception => Left(e)
    }
  }
}
