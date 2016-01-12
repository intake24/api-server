/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package org.workcraft.gwt.imagemap.server;


import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.workcraft.gwt.imagemap.client.ImageMapService;
import org.workcraft.gwt.imagemap.shared.ImageMapDefinition;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public class ImageMapServiceImpl extends RemoteServiceServlet implements ImageMapService {
	private static final long serialVersionUID = 2837289909589177855L;

	private String imageUrlBase;
	private String overlayUrlBase;
	private String definitionPath;
	
	private ImageMapParser parser = new ImageMapParser();
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		
		imageUrlBase = config.getServletContext().getInitParameter("image-url-base");
		overlayUrlBase = config.getInitParameter("overlay-url-base");
		definitionPath = config.getInitParameter("definition-path");
	}

	@Override
	public ImageMapDefinition getImageMap(String forName) {
		File definition = new File (definitionPath + "/" + forName + ".imagemap");
		try {
			return parser.parse(definition, forName, imageUrlBase, overlayUrlBase);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<ImageMapDefinition> getMultipleImageMaps(List<String> names) {
		ArrayList<ImageMapDefinition> result = new ArrayList<ImageMapDefinition>();
		
		for (String name: names) 
			result.add(getImageMap(name));			
		
		return result;
	}
}
