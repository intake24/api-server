package uk.ac.ncl.openlab.intake24.services.fooddb.images


import java.awt.Shape
import java.awt.geom.AffineTransform
import java.io.StringReader

import org.apache.batik.dom.svg.{SAXSVGDocumentFactory, SVGOMDocument}
import org.apache.batik.parser.{AWTPathProducer, AWTTransformProducer}
import org.apache.batik.util.XMLResourceDescriptor
import org.slf4j.LoggerFactory
import org.w3c.dom.svg.SVGSVGElement
import org.w3c.dom.{Element, Node}

case class AWTImageMap(navigation: Seq[Int], outlines: Map[Int, Shape], aspect: Double)

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
              val concat = new AffineTransform(t)
              concat.concatenate(transform)
              getTransformToRootSpace(node.getParentNode, concat)
            }
            case None => getTransformToRootSpace(node.getParentNode, t)
          }
        }
      }

      val transform = getTransformToRootSpace(path, new AffineTransform())
      transform.concatenate(AffineTransform.getScaleInstance(scale, scale))

      val shape = AWTPathProducer.createShape(new StringReader(path.getAttribute("d")), 0)

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

  def parseImageMap(svgPath: String): AWTImageMap = {
    logger.debug(s"Trying to parse image map from $svgPath")

    val svgDoc = parseSvg(svgPath)

    val svgElem = svgDoc.getRootElement

    val (width, height) = (getSVGWidth(svgElem), getSVGHeight(svgElem))

    val scale = 1.0 / width

    val aspect = width / height

    val outlines = getOutlines(scale, svgDoc, s => if (s.matches("area_[0-9]+")) Some(s.substring(5).toInt) else None)

    val navigation = getNavigation(svgDoc).getOrElse(outlines.keySet.toSeq.sorted)

    AWTImageMap(navigation, outlines, aspect)
  }

}
