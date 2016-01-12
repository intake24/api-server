/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.common.client;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;

public class LoadingWidget extends Composite {
	public LoadingWidget() {
		FlowPanel panel = new FlowPanel();
		panel.setStyleName("intake24-loading-widget");
		initWidget(panel);
	}
}
