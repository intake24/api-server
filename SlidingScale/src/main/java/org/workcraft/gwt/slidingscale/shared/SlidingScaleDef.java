/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package org.workcraft.gwt.slidingscale.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

public class SlidingScaleDef implements IsSerializable {
	public String baseImageUrl;
	public String overlayUrl;
	public int imageWidth;
	public int imageHeight;
	public int emptyLevel;
	public int fullLevel;
	
	/**
	 * @deprecated This constructor is only for GWT serialisation support, don't use it in your code
	 */
	@Deprecated
	public SlidingScaleDef() { }

	public SlidingScaleDef(String baseImageUrl, String overlayUrl, int imageWidth, int imageHeight, int emptyLevel, int fullLevel) {
		this.baseImageUrl = baseImageUrl;
		this.overlayUrl = overlayUrl;
		this.imageWidth = imageWidth;
		this.imageHeight = imageHeight;
		this.emptyLevel = emptyLevel;
		this.fullLevel = fullLevel;
	}
}