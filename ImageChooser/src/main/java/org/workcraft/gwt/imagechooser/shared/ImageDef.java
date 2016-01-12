/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package org.workcraft.gwt.imagechooser.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

public class ImageDef implements IsSerializable {
	public String url;
	public String thumbnailUrl;
	public String label;
	
	@Deprecated
	public ImageDef() {
		
	}
	
	public ImageDef(String url, String thumbnailUrl, String label) {
		this.url = url;
		this.thumbnailUrl = thumbnailUrl;
		this.label = label;
	}
}
