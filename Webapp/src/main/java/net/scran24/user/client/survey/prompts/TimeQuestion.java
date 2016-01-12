/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey.prompts;

import net.scran24.common.client.WidgetFactory;
import net.scran24.common.client.widgets.TimePicker;
import net.scran24.datastore.shared.Time;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;

public class TimeQuestion extends Composite {
	
	public final FlowPanel promptPanel;
	public final TimePicker timePicker;
	public final Button confirmButton;
	public final Button skipButton;
	
	public interface ResultHandler {
		public void handleAccept(Time time);
		public void handleSkip();
	}
	
	public TimeQuestion (final SafeHtml questionText, final String acceptLabel, final String skipLabel, 
			final Time initialTime, final ResultHandler handler, boolean scarySkipButton) {
		
		promptPanel = WidgetFactory.createPromptPanel(questionText);
		promptPanel.getElement().setId("intake24-time-question-prompt");
		
		timePicker = new TimePicker(initialTime);
		
		timePicker.hourCounter.getElement().setId("intake24-time-question-hours");
		timePicker.minuteCounter.getElement().setId("intake24-time-question-minutes");
		
		confirmButton = WidgetFactory.createGreenButton(acceptLabel, new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				handler.handleAccept(timePicker.getTime());
			}
		});
		
		confirmButton.getElement().setId("intake24-time-question-confirm-button");
		
		ClickHandler skipHandler = new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				handler.handleSkip();
			}
		};
		
		if (scarySkipButton)
			skipButton = WidgetFactory.createRedButton(skipLabel, skipHandler);
		else
			skipButton = WidgetFactory.createButton(skipLabel, skipHandler);
		
		skipButton.getElement().setId("intake24-time-question-skip-button");
		
		FlowPanel contents = new FlowPanel();

		contents.add(promptPanel);
		contents.add(timePicker);
		contents.add(WidgetFactory.createButtonsPanel(skipButton, confirmButton));
		
		initWidget(contents);
	}
}