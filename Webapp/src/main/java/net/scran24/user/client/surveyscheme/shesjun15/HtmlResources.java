/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.surveyscheme.shesjun15;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.TextResource;

public interface HtmlResources extends ClientBundle {
	public static final HtmlResources INSTANCE = GWT
			.create(HtmlResources.class);

	@Source("final.html")
	public TextResource getFinalHtml();	
}
