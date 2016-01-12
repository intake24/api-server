/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package org.workcraft.gwt.imagemap.client;

import java.util.List;

import org.workcraft.gwt.imagemap.shared.ImageMapDefinition;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("imageMap")
public interface ImageMapService extends RemoteService {
	ImageMapDefinition getImageMap (String name);
	List<ImageMapDefinition> getMultipleImageMaps (List<String> names);
}
