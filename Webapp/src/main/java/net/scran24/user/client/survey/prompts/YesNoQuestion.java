/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey.prompts;

import net.scran24.common.client.WidgetFactory;
import net.scran24.user.client.survey.prompts.messages.PromptMessages;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;

public class YesNoQuestion extends Composite {
	private final static PromptMessages messages = GWT.create(PromptMessages.class);
	
	public interface ResultHandler {
		public void handleYes();
		public void handleNo();
	}
	
	public YesNoQuestion(SafeHtml text, String yesLabel, String noLabel, final ResultHandler resultHandler) {
		
		Button yesButton = WidgetFactory.createButton(yesLabel, new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				resultHandler.handleYes();
			}
		});

		Button noButton = WidgetFactory.createButton(noLabel, new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				resultHandler.handleNo();
			}
		});
		
		FlowPanel contents = new FlowPanel();

		contents.add(WidgetFactory.createPromptPanel(text));
		contents.add(WidgetFactory.createButtonsPanel(yesButton, noButton));
		
		initWidget(contents);
	}
	
	public YesNoQuestion(SafeHtml text, final ResultHandler resultHandler) {
		this(text, messages.yesNoQuestion_defaultYesLabel(), messages.yesNoQuestion_defaultNoLabel(), resultHandler);				
	}
}