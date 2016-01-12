/*
This file is part of Intake24.

Copyright 2015, 2016 Newcastle University.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

This file is based on Intake24 v1.0.

© Crown copyright, 2012, 2013, 2014

Licensed under the Open Government Licence 3.0: 

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.common.client;

import net.scran24.common.client.survey.TutorialVideo;
import net.scran24.user.client.survey.SurveyMessages;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;

public class WidgetFactory {
	private static final CommonMessages messages = CommonMessages.Util.getInstance();

	public static Button createButton(String label) {
		Button result = new Button(label);
		result.addStyleName("intake24-button");
		return result;
	}

	public static Button createButton(String label, ClickHandler handler) {
		Button result = new Button(label);
		result.addStyleName("intake24-button");
		result.addClickHandler(handler);
		return result;
	}
	
	public static Button createHelpButton(ClickHandler handler) {
		Button button = createButton(messages.helpButtonLabel(), handler);
		return button;		
	}

	public static Button createGreenButton(String label, ClickHandler handler) {
		Button result = createButton(label, handler);
		result.addStyleName("intake24-button");
		result.addStyleName("intake24-green-button");
		return result;
	}

	public static Button createRedButton(String label, ClickHandler handler) {
		Button result = createButton(label, handler);
		result.addStyleName("intake24-button");
		result.addStyleName("intake24-red-button");
		return result;
	}

	public static Panel createButtonsPanel(Button... buttons) {
		Panel result = new FlowPanel();
		result.addStyleName("intake24-buttons-panel");
		for (Button b : buttons)
			result.add(b);
		return result;
	}

	public static FlowPanel createPromptPanel(SafeHtml promptText) {
		FlowPanel result = new FlowPanel(); 
		HTMLPanel promptPanel = new HTMLPanel(promptText);
		promptPanel.addStyleName("intake24-prompt-text");
		result.add(promptPanel);
		return result;
	}
	
	public static FlowPanel createPromptPanel(SafeHtml prompt, Widget floatWidget) {
		FlowPanel result = createPromptPanel(prompt);
		result.insert(floatWidget, 0);
		floatWidget.addStyleName("intake24-prompt-float-widget");
		return result;
	}
	
	
	public static Panel createLoadingPanelText(SafeHtml text) {
		HTMLPanel result = new HTMLPanel(text);
		result.addStyleName("intake24-loading-panel-text");
		return result;
	}

	public static Panel createTopPanel(Widget[] widgets) {
		final FlowPanel topPanel = new FlowPanel();
		topPanel.getElement().getStyle().setMarginTop(5, Unit.PX);

		for (Widget w : widgets)
			topPanel.add(w);

		return topPanel;
	}

	public static Panel createFoodPlaceholder(SafeHtml text) {
		HTMLPanel result = new HTMLPanel(text);
		result.addStyleName("scran24-food-list-text");
		return result;
	}

	public static Panel createDefaultErrorMessage() {
		FlowPanel result = new FlowPanel();
		result.add(new HTMLPanel(SafeHtmlUtils.fromSafeConstant(messages.serverError())));
		return result;
	}

	public static Anchor createBackLink() {
		Anchor back = new Anchor(SafeHtmlUtils.fromSafeConstant(messages.backLink()));
		back.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				History.back();
			}
		});

		return back;
	}
	
	public static Label createLabel(String text) {
		Label result = new Label(text);
		result.getElement().setClassName("intake24-label");
		return result;
	}
	
	public static Anchor createTutorialVideoLink() {
		Anchor videoLink = new Anchor();
		videoLink.setHref(TutorialVideo.url);
		videoLink.setTarget("_blank");

		Label videoLabel = new Label(messages.loginForm_watchVideo());
		videoLabel.getElement().addClassName("intake24-login-video-label");
				
		Image videoIcon = new Image("/images/video_icon.png");
		videoIcon.getElement().addClassName("intake24-video-icon");
		
		videoLink.getElement().appendChild(videoIcon.getElement());
		videoLink.getElement().appendChild(videoLabel.getElement());
		
		return videoLink;
	}
}
