/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
 */

package net.scran24.imagecompiler;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import org.apache.batik.parser.AWTPathProducer;
import org.apache.batik.parser.AWTTransformProducer;
import org.apache.batik.util.XMLResourceDescriptor;
import org.im4java.core.ConvertCmd;
import org.im4java.core.IMOperation;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.svg.SVGAnimatedRect;
import org.w3c.dom.svg.SVGLength;
import org.w3c.dom.svg.SVGSVGElement;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.apache.batik.anim.dom.SVGOMDocument;
import org.apache.batik.anim.dom.SAXSVGDocumentFactory;

public class ImageCompiler {
	public class Outline {
		public final Shape shape;
		public final AffineTransform transformToRootSpace;
		public final int id;

		public Outline(Shape shape, AffineTransform transform, int id) {
			this.shape = shape;
			this.transformToRootSpace = transform;
			this.id = id;
		}

		@Override
		public String toString() {
			return String.format("(%s, %s, %s)", shape.toString(), transformToRootSpace.toString(), id);
		}
	}

	private final ObjectMapper mapper = new ObjectMapper();

	private Outline fromPathElement(double scale, Element path, IdParser parser) {
		try {
			Shape shape = AWTPathProducer.createShape(new StringReader(path.getAttribute("d")), 0);

			Node n = path;

			AffineTransform transform = AffineTransform.getScaleInstance(scale, scale);

			while (n != null) {
				if (n.getAttributes() != null && n.getAttributes().getNamedItem("transform") != null) {
					String tt = n.getAttributes().getNamedItem("transform").getTextContent();
					AffineTransform t = AWTTransformProducer.createAffineTransform(tt);
					transform.concatenate(t);

				}
				n = n.getParentNode();
			}

			return new Outline(shape, transform, parser.parse(path.getAttribute("id")));
		} catch (Throwable e) {
			throw new ImageCompilerException(e);
		}
	}

	private SVGOMDocument parseSvg(File svg) {
		String parser = XMLResourceDescriptor.getXMLParserClassName();
		SAXSVGDocumentFactory f = new SAXSVGDocumentFactory(parser);
		String uri = "file:///" + svg.getAbsolutePath();
		SVGOMDocument doc;

		try {
			doc = (SVGOMDocument) f.createDocument(uri);
			return doc;
		} catch (Throwable e) {
			throw new ImageCompilerException(e);
		}
	}

	private List<Outline> getOutlines(double scale, SVGOMDocument doc, IdParser idParser) {
		ArrayList<Element> matchingPathElements = new ArrayList<Element>();

		NodeList nl = doc.getElementsByTagName("path");
		System.out.println(nl.getLength() + " path element(s) in SVG");

		for (int i = 0; i < nl.getLength(); i++) {
			Element e = (Element) nl.item(i);

			if (e.getAttribute("id").matches(idParser.pattern())) {
				System.out.println("Using " + e.getAttribute("id"));
				matchingPathElements.add(e);
			} else {
				System.out.println("Skipping " + e.getAttribute("id"));
			}
		}

		ArrayList<Outline> result = new ArrayList<Outline>();

		for (Element e : matchingPathElements) {
			Outline outline = fromPathElement(scale, e, idParser);
			result.add(outline);
		}

		return result;
	}

	private int[][] getNavigation(SVGOMDocument doc) {
		NodeList nl = doc.getElementsByTagName("text");

		Element navElement = null;

		for (int i = 0; i < nl.getLength(); i++) {
			Element e = (Element) nl.item(i);
			if (e.getAttribute("id").equals("navigation")) {
				navElement = e;
				break;
			}
		}

		if (navElement == null)
			return null;

		nl = navElement.getElementsByTagName("tspan");

		int[][] result = new int[nl.getLength()][];

		for (int i = 0; i < nl.getLength(); i++) {
			Element e = (Element) nl.item(i);
			String s[] = e.getTextContent().split("\\s+");
			result[i] = new int[s.length];

			for (int j = 0; j < s.length; j++)
				result[i][j] = Integer.parseInt(s[j]);
		}

		return result;
	}

	private int[][] generateNavigation(List<Outline> outlines) {
		int[][] result = new int[1][outlines.size()];

		for (int i = 0; i < outlines.size(); i++) {
			result[0][i] = outlines.get(i).id;
		}

		return result;
	}

	public void ensureDirectoryExists(File dir) {
		if (!dir.exists())
			dir.mkdirs();
		else if (!dir.isDirectory())
			throw new ImageCompilerException(dir.getAbsolutePath() + " points to a file rather than a directory.");
	}

	private void generateOverlays(int width, int height, float outlineWidth, Color outlineColor, boolean fill, double blur, List<Outline> outlines,
			File outlineDir, NameFunc f) {
		BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

		Graphics2D g = img.createGraphics();

		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setStroke(new BasicStroke(outlineWidth));
		g.setColor(outlineColor);

		try {
			for (Outline o : outlines) {
				g.setBackground(new Color(0, 0, 0, 0));
				g.clearRect(0, 0, img.getWidth(), img.getHeight());
				AffineTransform t = g.getTransform();
				g.setTransform(o.transformToRootSpace);
				g.draw(o.shape);
				if (fill)
					g.fill(o.shape);
				g.setTransform(t);

				File unblurred = File.createTempFile("img", ".png");
				ImageIO.write(img, "png", unblurred);

				ConvertCmd cmd = new ConvertCmd();

				IMOperation op = new IMOperation();
				op.addImage(unblurred.getAbsolutePath());
				op.channel("RBGA");
				op.blur(0.0, blur);

				String name = f.mkName(o);

				op.addImage(outlineDir.getAbsolutePath() + File.separator + name + ".png");

				cmd.run(op);

				unblurred.delete();

				Logger.getLogger("ImageCompiler").info("Wrote outline image: " + outlineDir.getAbsolutePath() + File.separator + name + ".png");
			}
		} catch (Throwable e) {
			throw new ImageCompilerException(e);
		}
	}

	private JsonNode serializeOutline(Outline o) {
		ObjectNode outlineObject = mapper.createObjectNode();
		outlineObject.put("id", o.id);

		ArrayNode outline = mapper.createArrayNode();

		PathIterator i = o.shape.getPathIterator(o.transformToRootSpace, 2);

		// i.next();

		while (!i.isDone()) {
			float coords[] = new float[6];
			int type = i.currentSegment(coords);

			if (type == PathIterator.SEG_MOVETO || type == PathIterator.SEG_LINETO) {
				outline.add(coords[0]);
				outline.add(coords[1]);
			}

			i.next();
		}

		outlineObject.put("coords", outline);

		return outlineObject;
	}

	private void writeDefinition(List<Outline> outlines, int[][] navigation, File file) {
		ObjectNode definition = mapper.createObjectNode();

		ArrayNode navObj = mapper.createArrayNode();

		for (int i = 0; i < navigation.length; i++) {
			ArrayNode line = mapper.createArrayNode();
			for (int j = 0; j < navigation[i].length; j++) {
				line.add(navigation[i][j]);
			}
			navObj.add(line);
		}

		ArrayNode areas = mapper.createArrayNode();

		for (Outline o : outlines)
			areas.add(serializeOutline(o));

		definition.put("navigation", navObj);
		definition.put("areas", areas);

		try {
			mapper.writerWithDefaultPrettyPrinter().writeValue(file, definition);
		} catch (Throwable e) {
			throw new ImageCompilerException(e);
		}

		Logger.getLogger("ImageCompiler").info("Wrote definition: " + file.getAbsolutePath());
	}

	public void compileGuideImages(File srcDir, File dstDir, File defDir, Color outlineColor, float outlineWidth, double blur, int width) {
		if (!srcDir.exists())
			throw new ImageCompilerException("Source directory " + srcDir.getAbsolutePath() + " does not exist.");
		if (!srcDir.isDirectory())
			throw new ImageCompilerException("Source directory path " + srcDir.getAbsolutePath() + " points to a file rather than a directory.");

		ensureDirectoryExists(dstDir);
		ensureDirectoryExists(defDir);

		File[] srcFiles = srcDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".svg");
			}
		});

		Logger log = Logger.getLogger("ImageCompiler");

		for (File f : srcFiles) {
			log.info("Processing " + f.getAbsolutePath());

			long svgModified = f.lastModified();

			String baseName = f.getName().substring(0, f.getName().length() - 4);

			File def = new File(defDir.getAbsolutePath() + File.separator + baseName + ".imagemap");

			if (def.exists() && svgModified < def.lastModified()) {
				log.info("Definition already exists, skipping");
				continue;
			} else {
				log.info("Compiling");
			}

			SVGOMDocument doc = parseSvg(f);

			SVGSVGElement svgElem = (SVGSVGElement) doc.getDocumentElement();
			
			SVGAnimatedRect viewBox = svgElem.getViewBox();
			
			float viewBoxWidth = viewBox.getBaseVal().getWidth();
			
			float viewBoxHeight = viewBox.getBaseVal().getHeight();
			
			double scale = width / (double) viewBox.getBaseVal().getWidth();

			List<Outline> outlines = getOutlines(scale, doc, new IdParser() {
				@Override
				public int parse(String id) {
					return Integer.parseInt(id.substring(5));
				}

				@Override
				public String pattern() {
					return "area_[0-9]+";
				}
			});

			int[][] navigation = getNavigation(doc);

			if (navigation == null)
				navigation = generateNavigation(outlines);

			File outlineDir = new File(dstDir.getAbsolutePath() + File.separator + baseName);
			outlineDir.mkdir();

			generateOverlays((int) (viewBoxWidth * scale), (int) (viewBoxHeight * scale), (float) (outlineWidth / scale), outlineColor, false, blur, outlines,
					outlineDir, new NameFunc() {
						@Override
						public String mkName(Outline outline) {
							return Integer.toString(outline.id);
						}
					});

			writeDefinition(outlines, navigation, def);
		}
	}

	public void compileDrinkScaleImages(File srcDir, File dstDir, Color outlineColor, float outlineWidth, double blur, int width) {
		if (!srcDir.exists())
			throw new ImageCompilerException("Source directory " + srcDir.getAbsolutePath() + " does not exist.");
		if (!srcDir.isDirectory())
			throw new ImageCompilerException("Source directory path " + srcDir.getAbsolutePath() + " points to a file rather than a directory.");

		ensureDirectoryExists(dstDir);

		File[] srcFiles = srcDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".svg");
			}
		});

		Logger log = Logger.getLogger("ImageCompiler");

		Map<String, ContainerDef> contdef;

		try {
			contdef = ContainerDef.parseContainerDef(new File(srcDir.getAbsolutePath() + File.separator + "def.csv"));
		} catch (Throwable e) {
			log.warning("Could not parse def.csv: " + e.getMessage());
			return;
		}

		File def = new File(dstDir.getAbsolutePath() + File.separator + "def.xml.partial");
		PrintWriter pw;
		try {
			pw = new PrintWriter(def);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}

		pw.println("<drinkware id=$id description=$description>");
		pw.println("  <choice guide-id=$guide_id/>");

		for (final File f : srcFiles) {
			log.info("Processing " + f.getAbsolutePath());
			String photo_id = f.getName().replace(".svg", "");
			log.info("photo_id = " + photo_id);

			File baseImage = new File(f.getParentFile().getAbsolutePath() + File.separator + photo_id + ".jpg");

			if (!baseImage.exists()) {
				log.warning("Base image file " + baseImage.getAbsolutePath() + " does not exist, skipping");
				continue;
			}

			ConvertCmd cmd = new ConvertCmd();

			IMOperation op = new IMOperation();
			op.resize(width);
			op.addImage(baseImage.getAbsolutePath());
			op.addImage(dstDir.getAbsolutePath() + File.separator + baseImage.getName());

			try {
				cmd.run(op);
			} catch (Throwable e) {
				log.warning("ImageMagick resize failed on " + baseImage.getAbsolutePath() + ", skipping");
				continue;
			}

			SVGOMDocument doc = parseSvg(f);

			SVGSVGElement svgElem = (SVGSVGElement) doc.getDocumentElement();

			SVGLength orig_width = svgElem.getWidth().getBaseVal();
			orig_width.convertToSpecifiedUnits(SVGLength.SVG_LENGTHTYPE_PX);
			SVGLength orig_height = svgElem.getHeight().getBaseVal();
			orig_height.convertToSpecifiedUnits(SVGLength.SVG_LENGTHTYPE_PX);

			int orig_w = (int) orig_width.getValueInSpecifiedUnits();
			int orig_h = (int) orig_height.getValueInSpecifiedUnits();

			double scale = width / (double) orig_w;

			List<Outline> outlines = getOutlines(scale, doc, new IdParser() {
				@Override
				public int parse(String id) {
					return 0;
				}

				@Override
				public String pattern() {
					return ".*?";
				}
			});

			if (outlines.size() == 0) {
				log.warning(f.getAbsolutePath() + " contains no paths, skipping");
				continue;
			}

			if (outlines.size() > 1)
				log.warning(f.getAbsolutePath() + " contains more than 1 outline, will only use the first one");

			int newWidth = (int) (orig_w * scale);
			int newHeight = (int) (orig_h * scale);

			generateOverlays(newWidth, newHeight, (float) (outlineWidth / scale), outlineColor, true, blur, outlines, dstDir, new NameFunc() {
				@Override
				public String mkName(Outline outline) {
					return f.getName().replace(".svg", "") + "_fill";
				}
			});

			Outline outline = outlines.get(0);
			Shape shapeInRootSpace = outline.transformToRootSpace.createTransformedShape(outline.shape);

			int emptyHeight = newHeight - (int) (shapeInRootSpace.getBounds2D().getMaxY());
			int fullHeight = newHeight - (int) (shapeInRootSpace.getBounds2D().getMinY());

			ContainerDef cont = contdef.get(photo_id);

			if (cont == null) {
				log.warning("No definition for photo_id " + photo_id + " skipping");
				continue;
			}

			pw.println("  <scale choice-id=\"" + cont.choice_id + "\">");
			pw.println("    <dimensions width=\"" + newWidth + "\" height=\"" + newHeight + "\" emptyLevel=\"" + emptyHeight + "\" fullLevel=\""
					+ fullHeight + "\"/>");
			pw.println("    <volume-function>");

			for (VolumeSample s : cont.volumeSamples)
				pw.println("      <value fill=\"" + s.fill + "\" volume=\"" + s.volume + "\"/>");

			pw.println("    </volume-function>");
			pw.println("    <baseImage>$url_base/" + baseImage.getName() + "</baseImage>");
			pw.println("    <overlayImage>$overlay_url_base/" + photo_id + "_fill.png" + "</overlayImage>");

			pw.println("  </scale>");

		}

		pw.println("</drinkware>");
		pw.close();
	}

	public void compileAsServedImages(File srcDir, File dstDir, File thumbDir, int imageWidth, int imageHeight, int thumbWidth) {
		if (!srcDir.exists())
			throw new ImageCompilerException("Source directory " + srcDir.getAbsolutePath() + " does not exist.");
		if (!srcDir.isDirectory())
			throw new ImageCompilerException("Source directory path " + srcDir.getAbsolutePath() + " points to a file rather than a directory.");

		ensureDirectoryExists(dstDir);
		ensureDirectoryExists(thumbDir);

		File[] srcFiles = srcDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".jpg");
			}
		});

		int counter = 0;

		try {
			for (File f : srcFiles) {
				// Logger.getLogger("ImageCompiler").info("Processing " +
				// f.getAbsolutePath());
				if (counter % 100 == 0)
					Logger.getLogger("ImageCompiler").info(counter + " of " + srcFiles.length);
				counter++;

				ConvertCmd cmd = new ConvertCmd();

				IMOperation op = new IMOperation();
				op.resize(imageWidth);
				op.background("white");
				op.gravity("Center");
				op.extent(imageWidth, imageHeight);
				op.addImage(f.getAbsolutePath());
				op.addImage(dstDir.getAbsolutePath() + File.separator + f.getName());
				cmd.run(op);

				IMOperation op2 = new IMOperation();
				op2.resize((int) (thumbWidth / 0.7));
				op2.gravity("Center");
				op2.addRawArgs("-crop", "70%x80%+0+0");
				op2.addImage(f.getAbsolutePath());
				op2.addImage(thumbDir.getAbsolutePath() + File.separator + f.getName());
				cmd.run(op2);
			}
		} catch (Throwable e) {
			throw new ImageCompilerException(e);
		}
	}

	private static void showUsage() {
		System.out.println("Intake24 image compiler tool\n");

		System.out.println("Usage: java -jar imc.jar -guide source_dir dest_dir def_dir width");
		System.out.println("  where");
		System.out.println("    source_dir is the path to directory containing source SVG files with area definitions");
		System.out.println("    dest_dir is the path to directory where the outline images will be produced");
		System.out.println("    def_dir is the path to directory where the image map definitions will be produced");
		System.out.println("    width");
		System.out.println("OR java -jar imc.jar -as source_dir dest_dir thumb_dir width height thumb_width");
		System.out.println("  where");
		System.out.println("    source_dir is the path to the directory containing source JPG images");
		System.out.println("    dest_dir is the path to the directory where the resized images will be produced");
		System.out.println("    thumb_dir is the path to the directory where the resized & cropped thumbnails will be produced");
		System.out.println("    width is the width of the resulting images");
		System.out.println("    height is the height of the resulting images (images will be resized based on the width\n"
				+ "    parameter so as to preserve the aspect ratio, and then cropped or padded with white to\n"
				+ "    fit exactly to the given height))");
		System.out.println("    thumb_width is the width of the resulting thumbnails (height will be adjusted automatically");
		System.out.println("\n");

		System.out.println("Note: this tool uses ImageMagick which must be installed separately. If the ImageMagick's");
		System.out.println("convert tool is not on the system path please set the IM4JAVA_TOOLPATH environment variable");
		System.out.println("to point to the directory containing ImageMagick binaries.");
		System.out.println("On Windows systems please set the environment variable in any case since the name of the convert");
		System.out.println("tool conflicts with a different Microsoft tool.");
	}

	public static void main(String[] args) {
		if (args.length == 0
				|| !((args[0].equals("-as") && args.length == 7) || (args[0].equals("-guide") && args.length == 5) || (args[0].equals("-drinkscale") && args.length == 5))) {
			showUsage();
			return;
		}

		ImageCompiler compiler = new ImageCompiler();

		if (args[0].equals("-guide"))
			compiler.compileGuideImages(new File(args[1]), new File(args[2]), new File(args[3]), new Color(32, 64, 128), 3.0f, 6.0,
					Integer.parseInt(args[4]));
		else if (args[0].equals("-as"))
			compiler.compileAsServedImages(new File(args[1]), new File(args[2]), new File(args[3]), Integer.parseInt(args[4]),
					Integer.parseInt(args[5]), Integer.parseInt(args[6]));
		else if (args[0].equals("-drinkscale")) {
			String high = args[3].substring(0, 4);
			String low = args[3].substring(4);
			int colv = (Integer.parseInt(high, 16) << 16) | Integer.parseInt(low, 16);

			compiler.compileDrinkScaleImages(new File(args[1]), new File(args[2]), new Color(colv, true), 1.0f, 1.0, Integer.parseInt(args[4]));
		}
	}
}