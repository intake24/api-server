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

public class ConsentPage implements SurveyStage<Survey> {
	public static final String FLAG_CONSENT_GIVEN = "consent-given";
	
	final private Survey data;

	public ConsentPage(Survey data) {
		this.data = data;
	}

	@Override
	public SimpleSurveyStageInterface getInterface(final Callback1<Survey> onComplete, Callback2<Survey, Boolean> onIntermediateStateChange) {
		final Button startButton = WidgetFactory.createGreenButton("I agree to take part. Continue to part 1 of the survey.", new ClickHandler() {
			public void onClick(ClickEvent event) {
				onComplete.call(data.withFlag(FLAG_CONSENT_GIVEN));
			}
		});
		
		HTMLPanel welcomePage = new HTMLPanel(HtmlResources.INSTANCE.getConsentHtml().getText());
		welcomePage.addStyleName("intake24-survey-content-container");
		
		welcomePage.addAndReplaceElement(startButton, "startButton");

		return new SimpleSurveyStageInterface(welcomePage);
	}
}