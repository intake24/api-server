/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey.prompts;


import net.scran24.common.client.WidgetFactory;
import net.scran24.user.client.survey.RecipeManager;
import net.scran24.user.client.survey.SurveyStageInterface;
import net.scran24.user.client.survey.flat.Prompt;
import net.scran24.user.client.survey.prompts.messages.PromptMessages;
import net.scran24.user.shared.TemplateFood;
import net.scran24.user.shared.FoodEntry;
import net.scran24.user.shared.Meal;
import net.scran24.user.shared.Recipe;

import org.workcraft.gwt.shared.client.Callback1;
import org.workcraft.gwt.shared.client.Function1;
import org.workcraft.gwt.shared.client.Pair;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;

public class SaveHomeRecipePrompt implements Prompt<Pair<FoodEntry, Meal>, MealOperation> {
	PromptMessages messages = GWT.create(PromptMessages.class);
	
	private final FoodEntry food;
	
	private final Function1<Meal, Meal> saveRecipeFunction;
	
	private final Function1<Meal, Meal> dontSaveRecipeFunction;
		
	private final TextBox recipeName;
			
	public SaveHomeRecipePrompt(final RecipeManager recipeManager, final Pair<FoodEntry, Meal> pair) {
		this.food = pair.left;	
		this.recipeName = new TextBox();
		
		recipeName.setText(pair.left.description());
		
		this.saveRecipeFunction =  new Function1<Meal, Meal>() {
			@Override
			public Meal apply(Meal argument) {
				
				TemplateFood mainFood = (TemplateFood)pair.left;
				
				Recipe recipe = new Recipe(mainFood.withFlag(Recipe.IS_SAVED_FLAG).minusCustomDataField(Recipe.SERVINGS_NUMBER_KEY).withDescription(recipeName.getText()), Meal.linkedFoods(pair.right.foods, pair.left));
				recipeManager.saveRecipe(recipe);
				
				return argument.updateFood(argument.foodIndex(food), mainFood.withFlag(Recipe.IS_SAVED_FLAG).withDescription(recipeName.getText()));				
			}
		};	
		
		this.dontSaveRecipeFunction = new Function1<Meal, Meal>() {
			@Override
			public Meal apply(Meal argument) {
				return argument.updateFood(argument.foodIndex(food), food.withFlag(Recipe.IS_SAVED_FLAG));
			}
		};
	}

	@Override
	public SurveyStageInterface getInterface(final Callback1<MealOperation> onComplete, final Callback1<Function1<Pair<FoodEntry, Meal>, Pair<FoodEntry, Meal>>> onIntermediateStateChange) {
		SafeHtml promptText = SafeHtmlUtils.fromSafeConstant(messages.homeRecipe_savePromptText(SafeHtmlUtils.htmlEscape(food.description())));
		
		HorizontalPanel recipeNamePanel = new HorizontalPanel();
		recipeNamePanel.setSpacing(5);
		recipeNamePanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
		
		recipeName.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				recipeName.selectAll();				
			}
		});
		
		recipeName.addKeyUpHandler(new KeyUpHandler() {
			@Override
			public void onKeyUp(KeyUpEvent event) {
				if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
					if (!recipeName.getText().isEmpty())
						onComplete.call(MealOperation.update(saveRecipeFunction));
				}
			}
		});
		
		recipeNamePanel.add(new Label (messages.homeRecipe_recipeNameLabel()));
		recipeNamePanel.add(recipeName);
		
		Button yesButton = WidgetFactory.createGreenButton(messages.yesNoQuestion_defaultYesLabel(), new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				if (!recipeName.getText().isEmpty())
					onComplete.call(MealOperation.update(saveRecipeFunction));
			}
		});
		
		Button noButton = WidgetFactory.createButton(messages.yesNoQuestion_defaultNoLabel(), new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				onComplete.call(MealOperation.update(dontSaveRecipeFunction));
			}
		});
		
		
		FlowPanel contents = new FlowPanel();
		contents.add(WidgetFactory.createPromptPanel(promptText));
		contents.add(recipeNamePanel);
		contents.add(WidgetFactory.createButtonsPanel(yesButton, noButton));
			
		return new SurveyStageInterface.Aligned(contents, HasHorizontalAlignment.ALIGN_LEFT, HasVerticalAlignment.ALIGN_TOP, SurveyStageInterface.DEFAULT_OPTIONS);
	}

	
}