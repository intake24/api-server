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


import net.scran24.common.client.WidgetFactory;
import net.scran24.user.client.survey.SurveyStageInterface;
import net.scran24.user.client.survey.flat.Prompt;
import net.scran24.user.client.survey.flat.Selection;
import net.scran24.user.client.survey.flat.SelectionMode;
import net.scran24.user.client.survey.flat.Survey;
import net.scran24.user.client.survey.flat.SurveyOperation;
import net.scran24.user.client.survey.prompts.messages.PromptMessages;
import net.scran24.user.shared.Meal;

import org.workcraft.gwt.shared.client.Callback1;
import org.workcraft.gwt.shared.client.Function1;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;

public class DeleteMealPrompt implements Prompt<Survey, SurveyOperation> {
	private final int mealIndex;
	private final Meal meal;
	private final PromptMessages messages = GWT.create(PromptMessages.class);

	public DeleteMealPrompt(final int mealIndex, final Meal meal) {
		this.mealIndex = mealIndex;
		this.meal = meal;
	}
	
	@Override
	public SurveyStageInterface getInterface(final Callback1<SurveyOperation> onComplete, final Callback1<Function1<Survey, Survey>> onIntermediateStateChange) {
		SafeHtml promptText = SafeHtmlUtils.fromSafeConstant(messages.deleteMeal_promptText(SafeHtmlUtils.htmlEscape(meal.name.toLowerCase())));
		
		Button deleteButton = WidgetFactory.createRedButton(messages.deleteMeal_deleteButtonLabel(), new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				onComplete.call(SurveyOperation.update(new Function1<Survey, Survey>() {
					@Override
					public Survey apply(Survey argument) {
						return argument.minusMeal(mealIndex).withSelection(new Selection.EmptySelection(SelectionMode.AUTO_SELECTION));
					}
				}));
			}
		});
		
		Button cancelButton = WidgetFactory.createButton(messages.deleteMeal_keepButtonLabel(), new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				onComplete.call(SurveyOperation.noChange);
			}
		});
		
		FlowPanel contents = new FlowPanel();
		contents.add(new HTMLPanel("h1", meal.safeNameWithTimeCapitalised()));
		contents.add(WidgetFactory.createPromptPanel(promptText));
		contents.add(WidgetFactory.createButtonsPanel(deleteButton, cancelButton));
			
		return new SurveyStageInterface.Aligned(contents, HasHorizontalAlignment.ALIGN_LEFT, HasVerticalAlignment.ALIGN_TOP, SurveyStageInterface.DEFAULT_OPTIONS);
	}
}