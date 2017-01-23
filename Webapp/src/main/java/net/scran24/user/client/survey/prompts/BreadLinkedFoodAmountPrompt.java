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

import net.scran24.common.client.GoogleAnalytics;
import net.scran24.common.client.WidgetFactory;
import net.scran24.datastore.shared.Time;

import net.scran24.user.client.ShepherdTour;
import net.scran24.user.client.survey.SurveyStageInterface;
import net.scran24.user.client.survey.flat.Prompt;
import net.scran24.user.client.survey.flat.PromptUtil;
import net.scran24.user.client.survey.prompts.messages.HelpMessages;
import net.scran24.user.client.survey.prompts.messages.PromptMessages;
import net.scran24.user.shared.EncodedFood;
import net.scran24.user.shared.FoodEntry;
import net.scran24.user.shared.FoodPrompt;
import net.scran24.user.shared.Meal;

import org.pcollections.client.PVector;
import org.pcollections.client.TreePVector;
import org.workcraft.gwt.shared.client.Callback1;
import org.workcraft.gwt.shared.client.Function1;
import org.workcraft.gwt.shared.client.Pair;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Panel;

import static net.scran24.common.client.LocaleUtil.*;

public class BreadLinkedFoodAmountPrompt implements Prompt<Pair<FoodEntry, Meal>, MealOperation> {
	private static final PromptMessages messages = PromptMessages.Util.getInstance();
	private static final HelpMessages helpMessages = HelpMessages.Util.getInstance();
	
/*	private final static PVector<ShepherdTour.Step> tour = TreePVector
			.<ShepherdTour.Step> empty()
			.plus(new ShepherdTour.Step("hours", "#intake24-time-question-hours", helpMessages.timeQuestion_hoursTitle(), helpMessages.timeQuestion_hoursDescription()))
			.plus(new ShepherdTour.Step("minutes", "#intake24-time-question-minutes", helpMessages.timeQuestion_minutesTitle(), helpMessages.timeQuestion_minutesDescription()))
			.plus(new ShepherdTour.Step("skipButton", "#intake24-time-question-skip-button", helpMessages.timeQuestion_deleteMealButtonTitle(), helpMessages.timeQuestion_deleteMealButtonDescription()))
			.plus(new ShepherdTour.Step("confirmButton", "#intake24-time-question-confirm-button", helpMessages.timeQuestion_confirmButtonTitle(), helpMessages.timeQuestion_confirmButtonDescription(), "top right", "bottom right"));*/
	
	private final Pair<FoodEntry, Meal> pair;
  private final int foodIndex;
	
	public BreadLinkedFoodAmountPrompt (Pair<FoodEntry, Meal> meal, int foodIndex, double quantity) {
		this.meal = meal;
	}
	
	@Override
	public SurveyStageInterface getInterface(Callback1<MealOperation> onComplete, Callback1<Function1<Pair<FoodEntry, Meal>, Pair<FoodEntry, Meal>>> updateIntermediateState) {
		FlowPanel content = new FlowPanel();
		
    final EncodedFood food = (EncodedFood) pair.left;
    final FoodPrompt prompt = food.enabledPrompts.get(promptIndex);

    final FlowPanel content = new FlowPanel();
    PromptUtil.addBackLink(content);
    final Panel promptPanel = WidgetFactory.createPromptPanel(
        SafeHtmlUtils.fromSafeConstant("<p>" + SafeHtmlUtils.htmlEscape(prompt.text) + "</p>"),
        WidgetFactory.createHelpButton(new ClickHandler() {
          @Override
          public void onClick(ClickEvent arg0) {
            String promptType = AssociatedFoodPrompt.class.getSimpleName();
            GoogleAnalytics.trackHelpButtonClicked(promptType);
            ShepherdTour.startTour(getShepherdTourSteps(), promptType);
          }
        }));
    content.add(promptPanel);
		
		return new SurveyStageInterface.Aligned(content, HasHorizontalAlignment.ALIGN_LEFT, HasVerticalAlignment.ALIGN_TOP, SurveyStageInterface.DEFAULT_OPTIONS);
	}
	
	@Override
	public String toString() {
		return "Confirm meal prompt";
	}
}