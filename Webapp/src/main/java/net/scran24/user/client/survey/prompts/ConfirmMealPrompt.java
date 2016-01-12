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

import static net.scran24.common.client.LocaleUtil.*;

public class ConfirmMealPrompt implements Prompt<Meal, MealOperation> {
	private static final PromptMessages messages = PromptMessages.Util.getInstance();
	private static final HelpMessages helpMessages = HelpMessages.Util.getInstance();
	
	private final static PVector<ShepherdTour.Step> tour = TreePVector
			.<ShepherdTour.Step> empty()
			.plus(new ShepherdTour.Step("hours", "#intake24-time-question-hours", helpMessages.timeQuestion_hoursTitle(), helpMessages.timeQuestion_hoursDescription()))
			.plus(new ShepherdTour.Step("minutes", "#intake24-time-question-minutes", helpMessages.timeQuestion_minutesTitle(), helpMessages.timeQuestion_minutesDescription()))
			.plus(new ShepherdTour.Step("skipButton", "#intake24-time-question-skip-button", helpMessages.timeQuestion_deleteMealButtonTitle(), helpMessages.timeQuestion_deleteMealButtonDescription()))
			.plus(new ShepherdTour.Step("confirmButton", "#intake24-time-question-confirm-button", helpMessages.timeQuestion_confirmButtonTitle(), helpMessages.timeQuestion_confirmButtonDescription(), "top right", "bottom right"));
	
	private final Meal meal;
	
	public ConfirmMealPrompt (Meal meal) {
		this.meal = meal;
	}
	
	public static String selectPromptMessage(String mealName) {
		if (compareCaseInsensitive(mealName, messages.predefMeal_Breakfast()))
			return messages.confirmMeal_promptText_breakfast();
		else if (compareCaseInsensitive(mealName, messages.predefMeal_EarlySnack()))
			return messages.confirmMeal_promptText_earlySnack();
		else if (compareCaseInsensitive(mealName, messages.predefMeal_Lunch()))
			return messages.confirmMeal_promptText_lunch();
		else if (compareCaseInsensitive(mealName, messages.predefMeal_MidDaySnack()))
			return messages.confirmMeal_promptText_midDaySnack();
		else if (compareCaseInsensitive(mealName, messages.predefMeal_Snack()))
			return messages.confirmMeal_promptText_snack();
		else if (compareCaseInsensitive(mealName, messages.predefMeal_Lunch()))
			return messages.confirmMeal_promptText_lunch();
		else if (compareCaseInsensitive(mealName, messages.predefMeal_Dinner()))
			return messages.confirmMeal_promptText_dinner();
		else if (compareCaseInsensitive(mealName, messages.predefMeal_EveningMeal()))
			return messages.confirmMeal_promptText_eveningMeal();
		else if (compareCaseInsensitive(mealName, messages.predefMeal_LateSnack()))
			return messages.confirmMeal_promptText_lateSnack();
		else return messages.confirmMeal_promptText_generic(SafeHtmlUtils.htmlEscape(toLowerCase(mealName)), SafeHtmlUtils.htmlEscape(capitaliseFirstCharacter(mealName)));
	}
	
	private String selectDeleteButtonMessage(String mealName) {
		if (compareCaseInsensitive(mealName, messages.predefMeal_Breakfast()))
			return messages.confirmMeal_skipButtonLabel_breakfast();
		else if (compareCaseInsensitive(mealName, messages.predefMeal_EarlySnack()))
			return messages.confirmMeal_skipButtonLabel_earlySnack();
		else if (compareCaseInsensitive(mealName, messages.predefMeal_Lunch()))
			return messages.confirmMeal_skipButtonLabel_lunch();
		else if (compareCaseInsensitive(mealName, messages.predefMeal_MidDaySnack()))
			return messages.confirmMeal_skipButtonLabel_midDaySnack();
		else if (compareCaseInsensitive(mealName, messages.predefMeal_Snack()))
			return messages.confirmMeal_skipButtonLabel_snack();
		else if (compareCaseInsensitive(mealName, messages.predefMeal_Lunch()))
			return messages.confirmMeal_skipButtonLabel_lunch();
		else if (compareCaseInsensitive(mealName, messages.predefMeal_Dinner()))
			return messages.confirmMeal_skipButtonLabel_dinner();
		else if (compareCaseInsensitive(mealName, messages.predefMeal_EveningMeal()))
			return messages.confirmMeal_skipButtonLabel_eveningMeal();
		else if (compareCaseInsensitive(mealName, messages.predefMeal_LateSnack()))
			return messages.confirmMeal_skipButtonLabel_lateSnack();		
		else return messages.confirmMeal_skipButtonLabel_generic(SafeHtmlUtils.htmlEscape(toLowerCase(mealName)));		
	}
 
	@Override
	public SurveyStageInterface getInterface(final Callback1<MealOperation> onComplete, 
			final Callback1<Function1<Meal, Meal>> onIntermediateStateChange) {		
		final SafeHtml promptText = SafeHtmlUtils.fromSafeConstant(selectPromptMessage(meal.name));
		final String skipText = SafeHtmlUtils.htmlEscape(selectDeleteButtonMessage(meal.name));
		final String acceptText = SafeHtmlUtils.htmlEscape(messages.confirmMeal_confirmButtonLabel());
		final Time initialTime = meal.guessTime();
		
		FlowPanel content = new FlowPanel();

		TimeQuestion timeQuestion = new TimeQuestion(promptText, acceptText, skipText, initialTime, new TimeQuestion.ResultHandler() {
			@Override
			public void handleSkip() {
				onComplete.call(MealOperation.deleteRequest(false));
			}
			
			@Override
			public void handleAccept(Time time) {
				onComplete.call(MealOperation.update(Meal.updateTimeFunc(time)));
			}
		}, false);
		
		Button helpButton = ShepherdTour.createTourButton(tour, ConfirmMealPrompt.class.getSimpleName());
		helpButton.getElement().addClassName("intake24-prompt-float-widget");
		timeQuestion.promptPanel.insert(helpButton, 0);
		
		content.add(timeQuestion);
		
		ShepherdTour.makeShepherdTarget(timeQuestion.promptPanel, timeQuestion.timePicker.hourCounter, timeQuestion.timePicker.minuteCounter, timeQuestion.skipButton, timeQuestion.confirmButton);
		
		return new SurveyStageInterface.Aligned(content, HasHorizontalAlignment.ALIGN_LEFT, HasVerticalAlignment.ALIGN_TOP, SurveyStageInterface.DEFAULT_OPTIONS);
	}
	
	@Override
	public String toString() {
		return "Confirm meal prompt";
	}
}