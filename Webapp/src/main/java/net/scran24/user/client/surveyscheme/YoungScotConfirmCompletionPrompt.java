/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.surveyscheme;


import net.scran24.common.client.WidgetFactory;
import net.scran24.user.client.survey.SurveyStageInterface;
import net.scran24.user.client.survey.flat.Prompt;
import net.scran24.user.client.survey.flat.Survey;
import net.scran24.user.client.survey.flat.SurveyOperation;

import org.workcraft.gwt.shared.client.Callback1;
import org.workcraft.gwt.shared.client.Function1;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;

public class YoungScotConfirmCompletionPrompt implements Prompt<Survey, SurveyOperation> {

	@Override
	public SurveyStageInterface getInterface(final Callback1<SurveyOperation> onComplete, 
			final Callback1<Function1<Survey, Survey>> onIntermediateStateChange) {
		final SafeHtml promptText = SafeHtmlUtils.fromSafeConstant("<p>We now have all the information we need concerning the foods you have entered.</p>" +
				"<p>Please review all the meals and foods listed in the panel on the left side of this page.</p>" +
				"<p>You can add or remove foods from your meals by clicking on a meal, or you can add another meal using the \"Add meal\" button if you had something else.</p>" +
				"<p>If you are sure that you have listed everything you have eaten during the previous 24 hours, please press the button below.");
		
		FlowPanel content = new FlowPanel();
		
		content.add (WidgetFactory.createPromptPanel(promptText));
		
		Button confirm = WidgetFactory.createGreenButton("Continue", new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				onComplete.call(SurveyOperation.update(new Function1<Survey, Survey>() {
					@Override
					public Survey apply(Survey argument) {
						return argument.markCompletionConfirmed();
					}
				}));
			}
		}); 
		
		content.add (WidgetFactory.createButtonsPanel(confirm));
		
		return new SurveyStageInterface.Aligned(content, HasHorizontalAlignment.ALIGN_LEFT, HasVerticalAlignment.ALIGN_TOP, SurveyStageInterface.DEFAULT_OPTIONS);
	}
	
	@Override
	public String toString() {
		return "Confirm survey completion prompt";
	}
}