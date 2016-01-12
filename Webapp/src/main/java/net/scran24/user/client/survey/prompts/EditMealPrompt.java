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

import java.util.List;

import net.scran24.common.client.LocaleUtil;
import net.scran24.common.client.WidgetFactory;
import net.scran24.datastore.shared.Time;
import net.scran24.user.client.ShepherdTour;
import net.scran24.user.client.survey.SurveyStageInterface;
import net.scran24.user.client.survey.flat.Prompt;
import net.scran24.user.client.survey.foodlist.EditableFoodList;
import net.scran24.user.client.survey.prompts.messages.HelpMessages;
import net.scran24.user.client.survey.prompts.messages.PromptMessages;
import net.scran24.user.shared.FoodEntry;
import net.scran24.user.shared.Meal;

import org.pcollections.client.PVector;
import org.pcollections.client.TreePVector;
import org.workcraft.gwt.shared.client.Callback;
import org.workcraft.gwt.shared.client.Callback1;
import org.workcraft.gwt.shared.client.Function1;
import org.workcraft.gwt.shared.client.Option;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Panel;

public class EditMealPrompt implements Prompt<Meal, MealOperation> {
	private final Meal meal;
	private EditableFoodList foodList;
	private EditableFoodList drinkList;
	
	private final static PromptMessages messages = PromptMessages.Util.getInstance();
	
	private final static HelpMessages helpMessages = HelpMessages.Util.getInstance();

	private final static PVector<ShepherdTour.Step> tour = TreePVector.<ShepherdTour.Step>empty()
			.plus(new ShepherdTour.Step("mealName", "#intake24-meal-name", helpMessages.editMeal_mealNameTitle(), helpMessages.editMeal_mealNameDescription()))
			.plus(new ShepherdTour.Step("foodList", "#intake24-food-list", helpMessages.editMeal_foodListTitle(), helpMessages.editMeal_foodListDescription()))
			.plus(new ShepherdTour.Step("drinkList", "#intake24-drink-list", helpMessages.editMeal_drinkListTitle(), helpMessages.editMeal_drinkListDescription()))
			.plus(new ShepherdTour.Step("changeTimeButton", "#intake24-change-time-button", helpMessages.editMeal_changeTimeButtonTitle(), helpMessages.editMeal_changeTimeButtonDescription()))
			.plus(new ShepherdTour.Step("deleteMealButton", "#intake24-delete-button", helpMessages.editMeal_deleteMealButtonTitle(), helpMessages.editMeal_deleteMealButtonDescription()))
			.plus(new ShepherdTour.Step("continueButton", "#intake24-done-button", helpMessages.editMeal_continueButtonTitle(), helpMessages.editMeal_continueButtonDescription(), "top right", "bottom right"));
	
	private final boolean addDrink;
	
	public EditMealPrompt(final Meal meal, boolean addDrink) {
		this.meal = meal;
		this.addDrink = addDrink;
	}

	@Override
	public SurveyStageInterface getInterface(final Callback1<MealOperation> onComplete, final Callback1<Function1<Meal, Meal>> onIntermediateStateChange) {
		final SafeHtml promptText = SafeHtmlUtils.fromSafeConstant(messages.editMeal_promptText(SafeHtmlUtils.htmlEscape(meal.name.toLowerCase())));

		final Function1<FoodEntry, Boolean> foodFilter = new Function1<FoodEntry, Boolean>() {
			@Override
			public Boolean apply(FoodEntry argument) {
				return !argument.isDrink();
			}
		};

		final Function1<FoodEntry, Boolean> drinkFilter = new Function1<FoodEntry, Boolean>() {
			@Override
			public Boolean apply(FoodEntry argument) {
				return argument.isDrink();
			}
		};
		
		final Button done = WidgetFactory.createGreenButton(messages.editMeal_finishButtonLabel(), new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				onComplete.call(new MealOperation() {
					@Override
					public <R> R accept(Visitor<R> visitor) {
						return visitor.visitUpdate(new Function1<Meal, Meal>() {
							@Override
							public Meal apply (Meal argument) {
								return argument.withFoods(
											TreePVector.<FoodEntry> empty()
											.plusAll(foodList.getEnteredItems())
											.plusAll(drinkList.getEnteredItems()))
										.markFreeEntryComplete();

							}
						});
					}
				});
			}
		});
		
		done.getElement().setId("intake24-done-button");
		

		final Callback1<List<FoodEntry>> onChange = new Callback1<List<FoodEntry>>() {
			@Override
			public void call(List<FoodEntry> arg1) {
				TreePVector<FoodEntry> newItems = TreePVector.<FoodEntry> empty().plusAll(foodList.getEnteredItems())
						.plusAll(drinkList.getEnteredItems());
				onIntermediateStateChange.call(Meal.updateFoodsFunc(newItems));
			}
		};
		
		// Food list

		foodList = new EditableFoodList(meal.foods, foodFilter, messages.editMeal_addFoodButtonLabel(), false, onChange);		
		final HTMLPanel foodHeader = new HTMLPanel("h2", SafeHtmlUtils.htmlEscape(messages.editMeal_foodHeader()));		
		final FlowPanel foodListContainer = new FlowPanel();
		foodListContainer.getElement().setId("intake24-food-list");
		foodListContainer.add(foodHeader);
		foodListContainer.add(foodList);
		ShepherdTour.makeShepherdTarget(foodListContainer);
		
		// Drinks list

		drinkList = new EditableFoodList(meal.foods, drinkFilter, messages.editMeal_addDrinkButtonLabel(), true, onChange);
		final HTMLPanel drinksHeader = new HTMLPanel("h2", SafeHtmlUtils.htmlEscape(messages.editMeal_drinksHeader()));
		drinkList.getElement().setId("intake24-drink-list");
		final FlowPanel drinkListContainer = new FlowPanel();
		drinkListContainer.getElement().setId("intake24-drink-list");
		drinkListContainer.add(drinksHeader);
		drinkListContainer.add(drinkList);
		ShepherdTour.makeShepherdTarget(drinkListContainer);		
		
		Button changeTime = WidgetFactory.createButton(messages.editMeal_changeTimeButtonLabel(), new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				onComplete.call(MealOperation.editTimeRequest);
			}
		});
		
		changeTime.getElement().setId("intake24-change-time-button");
		
		Button delete = WidgetFactory.createButton(messages.editMeal_deleteMealButtonLabel(), new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				onComplete.call(MealOperation.deleteRequest(true));				
			}
		});
		
		delete.getElement().setId("intake24-delete-button");
		
		final HTMLPanel header = new HTMLPanel("h1", LocaleUtil.capitaliseFirstCharacter(meal.safeNameWithTime()));
		header.getElement().setId("intake24-meal-name");
		ShepherdTour.makeShepherdTarget(header);
		
		FlowPanel contents = new FlowPanel();
		contents.addStyleName("intake24-edit-meal-prompt");
		contents.add(header);
		Panel promptPanel = WidgetFactory.createPromptPanel(promptText, ShepherdTour.createTourButton(tour, EditMealPrompt.class.getSimpleName()));
		contents.add(promptPanel);
		ShepherdTour.makeShepherdTarget(promptPanel);				
			
		contents.add(foodListContainer);
		contents.add(drinkListContainer);
		contents.add(WidgetFactory.createButtonsPanel(changeTime, delete, done));
		
		ShepherdTour.makeShepherdTarget(changeTime, delete, done);
		
		return new SurveyStageInterface.Aligned(contents, HasHorizontalAlignment.ALIGN_LEFT, HasVerticalAlignment.ALIGN_TOP, SurveyStageInterface.DEFAULT_OPTIONS, Option.<Callback>some(new Callback() {
			@Override
			public void call() {
				if (addDrink)
					drinkList.focusNew();
			}
		}));
	}

	@Override
	public String toString() {
		return "Edit meal prompt";
	}
}