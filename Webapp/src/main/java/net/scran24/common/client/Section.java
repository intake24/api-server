/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.common.client;

import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class Section<T extends Widget> extends VerticalPanel {
	private final FlowPanel headerPanel;
	public final Widget headerWidget;
	public final T contentWidget;

	public Section(Widget headerWidget, T contentWidget) {
		this.headerWidget = headerWidget;
		this.contentWidget = contentWidget;

		headerPanel = new FlowPanel();
		headerPanel.setStyleName("scran24-full-page-section-header");
		headerPanel.add(headerWidget);

		add(headerPanel);
		add(contentWidget);
		
		//contentWidget.contentWidgetsetStyleName("scran24-full-page-section");
	}
	
	public static <T extends Widget> Section<T> withSimpleHeader (String title, T content) {
		return new Section<T>(new HTMLPanel(SafeHtmlUtils.fromSafeConstant("<h2>" + SafeHtmlUtils.htmlEscape(title) + "</h2>")), content);
	}
}