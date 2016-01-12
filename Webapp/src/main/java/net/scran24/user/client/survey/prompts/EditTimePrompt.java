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

package net.scran24.user.client.survey.prompts;



import net.scran24.datastore.shared.Time;
import net.scran24.user.client.ShepherdTour;
import net.scran24.user.client.survey.SurveyStageInterface;
import net.scran24.user.client.survey.flat.Prompt;
import net.scran24.user.client.survey.prompts.messages.HelpMessages;
import net.scran24.user.client.survey.prompts.messages.PromptMessages;
import net.scran24.user.shared.Meal;

import org.pcollections.client.PVector;
import org.pcollections.client.TreePVector;
import org.workcraft.gwt.shared.client.Callback1;
import org.workcraft.gwt.shared.client.Function1;

import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;

public class EditTimePrompt implements Prompt<Meal, MealOperation> {
	private static final PromptMessages messages = PromptMessages.Util.getInstance();
	private static final HelpMessages helpMessages = HelpMessages.Util.getInstance();
	
	private final static PVector<ShepherdTour.Step> tour = TreePVector
			.<ShepherdTour.Step> empty()
			.plus(new ShepherdTour.Step("hours", "#intake24-time-question-hours", helpMessages.timeQuestion_hoursTitle(), helpMessages.timeQuestion_hoursDescription()))
			.plus(new ShepherdTour.Step("minutes", "#intake24-time-question-minutes", helpMessages.timeQuestion_minutesTitle(), helpMessages.timeQuestion_minutesDescription()))
			.plus(new ShepherdTour.Step("skipButton", "#intake24-time-question-skip-button", helpMessages.timeQuestion_cancelButtonTitle(), helpMessages.timeQuestion_cancelButtonDescription()))
			.plus(new ShepherdTour.Step("confirmButton", "#intake24-time-question-confirm-button", helpMessages.timeQuestion_confirmButtonTitle(), helpMessages.timeQuestion_confirmButtonDescription(), "top right", "bottom right"));
	
	private final Time currentTime;
	private final String mealName;
	
	public EditTimePrompt (final String mealName, final Time currentTime) {
		this.mealName = mealName;
		this.currentTime = currentTime;
	}
 
	@Override
	public SurveyStageInterface getInterface(final Callback1<MealOperation> onComplete, 
			final Callback1<Function1<Meal, Meal>> onIntermediateStateChange) {
		final SafeHtml question = SafeHtmlUtils.fromSafeConstant(ConfirmMealPrompt.selectPromptMessage(mealName));
		final String skipText = messages.editTime_cancelButtonLabel();
		final String acceptText = messages.editTime_confirmButtonLabel(); 
		
		FlowPanel content = new FlowPanel();

		TimeQuestion timeQuestion = new TimeQuestion(question, acceptText, skipText, currentTime, new TimeQuestion.ResultHandler() {
			@Override
			public void handleSkip() {
				onComplete.call(MealOperation.noChange);
			}
			
			@Override
			public void handleAccept(Time time) {
				onComplete.call(MealOperation.update(Meal.updateTimeFunc(time)));
			}
		}, false);
		
		Button helpButton = ShepherdTour.createTourButton(tour, EditTimePrompt.class.getSimpleName());
		helpButton.getElement().getStyle().setFloat(com.google.gwt.dom.client.Style.Float.RIGHT);
		timeQuestion.promptPanel.insert(helpButton, 0);
		
		content.add(timeQuestion);
		
		ShepherdTour.makeShepherdTarget(timeQuestion.promptPanel, timeQuestion.timePicker.hourCounter, timeQuestion.timePicker.minuteCounter, timeQuestion.skipButton, timeQuestion.confirmButton);
		
		return new SurveyStageInterface.Aligned(content, HasHorizontalAlignment.ALIGN_LEFT, HasVerticalAlignment.ALIGN_TOP, SurveyStageInterface.DEFAULT_OPTIONS);
	}
	
	@Override
	public String toString() {
		return "Edit meal time prompt";
	}
}