/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.surveyscheme.ucljan15;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.TextResource;

public interface HtmlResources extends ClientBundle {
	public static final HtmlResources INSTANCE = GWT
			.create(HtmlResources.class);

	@Source("welcome.html")
	public TextResource getWelcomeHtml();
	
	@Source("consent.html")
	public TextResource getConsentHtml();
	
	@Source("final.html")
	public TextResource getFinalHtml();	
}
