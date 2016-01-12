/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.common.client;

import com.google.gwt.user.client.ui.HasHorizontalAlignment.HorizontalAlignmentConstant;
import com.google.gwt.user.client.ui.HasVerticalAlignment.VerticalAlignmentConstant;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class UiHelper {
	public static Widget createFullPageSection(Widget contents, HorizontalAlignmentConstant horizontalAlignment, 
			VerticalAlignmentConstant verticalAlignment) {
		VerticalPanel outerPanel = new VerticalPanel();
		outerPanel.addStyleName("scran24-full-page-section");

		HorizontalPanel alignPanel = new HorizontalPanel();
		alignPanel.setVerticalAlignment(verticalAlignment);
		alignPanel.setHorizontalAlignment(horizontalAlignment);
		alignPanel.setWidth("100%");
		alignPanel.setHeight("100%");
		
		alignPanel.add(contents);
		
		outerPanel.add(alignPanel);
		
		return outerPanel;
	}
}
