/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey.prompts;


import net.scran24.common.client.WidgetFactory;
import net.scran24.user.client.survey.SurveyStageInterface;
import net.scran24.user.client.survey.flat.FoodOperation;
import net.scran24.user.client.survey.flat.Prompt;
import net.scran24.user.client.survey.prompts.messages.PromptMessages;
import net.scran24.user.shared.CompoundFood;
import net.scran24.user.shared.FoodEntry;

import org.workcraft.gwt.shared.client.Callback1;
import org.workcraft.gwt.shared.client.Function1;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;


public class FoodCompletePrompt implements Prompt<FoodEntry, FoodOperation> {
	private final FoodEntry food;
	private final PromptMessages messages = GWT.create(PromptMessages.class);
	
	public FoodCompletePrompt(final FoodEntry food) {
		this.food = food;
	}
	
	@Override
	public SurveyStageInterface getInterface(final Callback1<FoodOperation> onComplete, final Callback1<Function1<FoodEntry, FoodEntry>> onIntermediateStateChange) {
		
		final SafeHtml promptText = SafeHtmlUtils.fromSafeConstant(messages.foodComplete_promptText(SafeHtmlUtils.htmlEscape(food.description().toLowerCase())));
		
		Button contButton = WidgetFactory.createButton(messages.foodComplete_continueButtonLabel(), new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				onComplete.call(FoodOperation.noChange);
			}
		});
		
		Button addButton = WidgetFactory.createButton(messages.foodComplete_editMealButtonLabel(), new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				onComplete.call(FoodOperation.editFoodsRequest);
			}
		});
		
		Button editIngredients = WidgetFactory.createButton(messages.foodComplete_editIngredients(), new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				onComplete.call(FoodOperation.update(new Function1<FoodEntry, FoodEntry>(){
					@Override
					public FoodEntry apply(FoodEntry argument) {
						return argument.withFlags(argument.flags.minus(CompoundFood.FLAG_INGREDIENTS_COMPLETE));
					}
				}));
			}
		});
		
		Button deleteButton = WidgetFactory.createRedButton(messages.foodComplete_deleteFoodButtonLabel(), new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				onComplete.call(FoodOperation.deleteRequest);				
			}
		});
		
		FlowPanel contents = new FlowPanel();
		
		contents.add(WidgetFactory.createPromptPanel(promptText));
		if (food.isCompound() && food.flags.contains(CompoundFood.FLAG_INGREDIENTS_COMPLETE))
			contents.add(WidgetFactory.createButtonsPanel(contButton, editIngredients, deleteButton));
		else
			contents.add(WidgetFactory.createButtonsPanel(contButton, addButton, deleteButton));
	
		return new SurveyStageInterface.Aligned(contents, HasHorizontalAlignment.ALIGN_LEFT, HasVerticalAlignment.ALIGN_TOP, SurveyStageInterface.DEFAULT_OPTIONS);
	}
	
	@Override
	public String toString() {
		return "Food complete prompt";
	}
}