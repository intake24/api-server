/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.surveyscheme.ucljan15;

import net.scran24.common.client.WidgetFactory;
import net.scran24.user.client.survey.SimpleSurveyStageInterface;
import net.scran24.user.client.survey.SurveyStage;
import net.scran24.user.client.survey.flat.Survey;

import org.workcraft.gwt.shared.client.Callback1;
import org.workcraft.gwt.shared.client.Callback2;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HTMLPanel;

public class WelcomePage implements SurveyStage<Survey> {
	public static final String FLAG_WELCOME_PAGE_SHOWN = "welcome-page-shown";
	
	final private Survey initialData;

	public WelcomePage(Survey initialData) {
		this.initialData = initialData;
	}

	@Override
	public SimpleSurveyStageInterface getInterface(final Callback1<Survey> onComplete, Callback2<Survey, Boolean> onIntermediateStateChange) {
		final Button startButton = WidgetFactory.createGreenButton("Continue to information page", new ClickHandler() {
			public void onClick(ClickEvent event) {
				onComplete.call(initialData.withFlag(FLAG_WELCOME_PAGE_SHOWN));
			}
		});
		
		HTMLPanel welcomePage = new HTMLPanel(HtmlResources.INSTANCE.getWelcomeHtml().getText());
		welcomePage.addStyleName("intake24-survey-content-container");
		
		welcomePage.addAndReplaceElement(startButton, "startButton");

		return new SimpleSurveyStageInterface(welcomePage);
	}
}