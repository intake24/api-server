/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.common.client;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

public class OverlayDiv {
	private Widget contents;
	
	private FlowPanel fade;
	private FlowPanel overlay;
	private boolean visible;
	
	public void setContents(Widget contents) {
		this.contents = contents;
	}

	public void setVisible(boolean visible) {
		if (this.visible == visible)
			return;
		
		RootPanel body = RootPanel.get();
		
		if (visible) {
			fade = new FlowPanel();
			fade.addStyleName("intake24-overlay-fade");
			body.add(fade);
			
			overlay = new FlowPanel();
			overlay.addStyleName("intake24-overlay");
			overlay.add(contents);
			
			body.add(overlay);
		} else {
			overlay.remove(contents);
			body.remove(fade);
			body.remove(overlay);
			fade = null;
			overlay = null;
		}
		
		this.visible = visible;
	}
}