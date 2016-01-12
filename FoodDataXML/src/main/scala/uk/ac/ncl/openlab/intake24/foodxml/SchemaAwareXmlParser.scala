/*
This file is part of Intake24.

Copyright 2015, 2016 Newcastle University.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package uk.ac.ncl.openlab.intake24.foodxml

// Code from http://stackoverflow.com/questions/1627111/how-does-one-validate-the-schema-of-an-xml-file-using-scala

import org.xml.sax.InputSource
import scala.xml.parsing.NoBindingFactoryAdapter
import scala.xml.{ TopScope, Elem }
import javax.xml.parsers.{ SAXParserFactory, SAXParser }
import javax.xml.validation.Schema
import javax.xml.validation.SchemaFactory
import javax.xml.XMLConstants
import javax.xml.transform.stream.StreamSource
import java.io.FileInputStream

class SchemaAwareFactoryAdapter(schema: Schema) extends NoBindingFactoryAdapter {
  override def loadXML(source: InputSource, parser: SAXParser) = {
    val reader = parser.getXMLReader()
    val handler = schema.newValidatorHandler()
    handler.setContentHandler(this)
    reader.setContentHandler(handler)

    scopeStack.push(TopScope)
    reader.parse(source)
    scopeStack.pop
    rootElem.asInstanceOf[Elem]
  }

  override def parser: SAXParser = {
    val factory = SAXParserFactory.newInstance()
    factory.setNamespaceAware(true)
    factory.setFeature("http://xml.org/sax/features/namespace-prefixes", true)
    factory.newSAXParser()
  }
}

object XmlParser {
  val factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)

  def loadXmlWithSchema(xmlPath: String, schemaPath: String) = {
    val xsdStream = new FileInputStream (schemaPath)
    val schema = factory.newSchema(new StreamSource(xsdStream))
    val source = new FileInputStream (xmlPath)
    
    new SchemaAwareFactoryAdapter(schema).load(source)
  }
}