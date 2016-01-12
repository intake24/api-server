/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package org.workcraft.gwt.imagemap.server;

import java.io.File;
import java.io.IOException;

import org.workcraft.gwt.imagemap.shared.ImageMapDefinition;
import org.workcraft.gwt.imagemap.shared.ImageMapDefinition.Area;
import org.workcraft.gwt.imagemap.shared.Polygon;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ImageMapParser {
	private interface UrlGenerator {
		String makeUrl (int id);
	}
	
	ObjectMapper mapper = new ObjectMapper();
	
	public int[][] parseNavigation (JsonNode navNode) {
		ArrayNode node = (ArrayNode) navNode;
		
		int[][] result = new int[node.size()][];
		
		for (int i = 0; i < node.size(); i++) {
			ArrayNode line = (ArrayNode) node.get(i);
			result[i] = new int[line.size()];
			
			for (int j=0; j<line.size(); j++)
				result[i][j] = line.get(j).asInt();
		}
		
		return result;
	}
	
	public Polygon parsePolygon (JsonNode coordsNode) {
		ArrayNode node = (ArrayNode) coordsNode;
		double[] coords = new double[node.size()];
		
		for (int i=0; i<node.size(); i++)
			coords[i] = node.get(i).asDouble();
		
		return new Polygon(coords);
	}
	
	public Area parseArea(JsonNode areaNode, UrlGenerator gen) {
		ObjectNode node = (ObjectNode) areaNode;
		
		int id = node.get("id").asInt();
		Polygon outline = parsePolygon (node.get("coords"));
		String overlayUrl = gen.makeUrl(id);
		
		return new Area(outline, overlayUrl, id);
	}
	
	public Area[] parseAreas (JsonNode areasNode, UrlGenerator gen) {
		ArrayNode node = (ArrayNode) areasNode;
		
		Area[] result = new Area[node.size()];
		
		for (int i=0; i<node.size(); i++)
			result[i] = parseArea(node.get(i), gen);
		
		return result;
	}
	
	public ImageMapDefinition parse(File file, final String name, String imageBaseUrl, final String overlayBaseUrl) throws JsonProcessingException, IOException {
		JsonNode n = mapper.readTree(file);
		
		UrlGenerator gen = new UrlGenerator() {
			@Override
			public String makeUrl(int id) {
				return overlayBaseUrl + "/" + name + "/" + Integer.toString(id) + ".png";
			}
		};
		
		int[][] navigation = parseNavigation (n.get("navigation"));
		Area[] areas = parseAreas(n.get("areas"), gen);
		String baseUrl = imageBaseUrl + "/" + name + ".jpg";
		
		return new ImageMapDefinition(baseUrl, areas, navigation);
	}
}
