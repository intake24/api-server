/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.Widget;

public class MainWindow extends DockLayoutPanel{
	private Widget currentContent = null;
	
	public MainWindow (Widget header, int headerHeight, Widget footer, int footerHeight) {
		super(Unit.PX);
		addNorth(header, headerHeight);
		addSouth(footer, footerHeight);
	}
	
	public void setContent (Widget content) {
		if (currentContent != null) {
			remove (currentContent);
		}
		
		add (content);
		currentContent = content;
	}
}
