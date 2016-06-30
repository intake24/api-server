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

package net.scran24.user.client.surveyscheme;

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import net.scran24.common.client.CurrentUser;
import net.scran24.datastore.shared.Time;
import net.scran24.user.client.LogRecorder;
import net.scran24.user.client.PredefinedMeals;
import net.scran24.user.client.ProcessMilkInHotDrinks;
import net.scran24.user.client.SurveyInterfaceManager;
import net.scran24.user.client.survey.CompoundFoodTemplateManager;
import net.scran24.user.client.survey.FoodTemplates;
import net.scran24.user.client.survey.RecipeManager;
import net.scran24.user.client.survey.flat.FoodOperation;
import net.scran24.user.client.survey.flat.PromptAvailabilityBasedSelectionManager;
import net.scran24.user.client.survey.flat.PromptManager;
import net.scran24.user.client.survey.flat.PromptRule;
import net.scran24.user.client.survey.flat.RuleBasedPromptManager;
import net.scran24.user.client.survey.flat.Rules;
import net.scran24.user.client.survey.flat.Selection;
import net.scran24.user.client.survey.flat.SelectionManager;
import net.scran24.user.client.survey.flat.SelectionRule;
import net.scran24.user.client.survey.flat.SelectionMode;
import net.scran24.user.client.survey.flat.StateManager;
import net.scran24.user.client.survey.flat.StateManagerUtil;
import net.scran24.user.client.survey.flat.Survey;
import net.scran24.user.client.survey.flat.SurveyOperation;
import net.scran24.user.client.survey.flat.rules.AskForMealTime;
import net.scran24.user.client.survey.flat.rules.AskForMissingFoodDescription;
import net.scran24.user.client.survey.flat.rules.AskToLookupFood;
import net.scran24.user.client.survey.flat.rules.ChoosePortionSizeMethod;
import net.scran24.user.client.survey.flat.rules.ConfirmCompletion;
import net.scran24.user.client.survey.flat.rules.InformFoodComplete;
import net.scran24.user.client.survey.flat.rules.SelectFoodForAssociatedPrompts;
import net.scran24.user.client.survey.flat.rules.SelectForPortionSize;
import net.scran24.user.client.survey.flat.rules.SelectIncompleteFreeEntryMeal;
import net.scran24.user.client.survey.flat.rules.SelectMealForReadyMeals;
import net.scran24.user.client.survey.flat.rules.SelectMealWithNoDrink;
import net.scran24.user.client.survey.flat.rules.SelectRawFood;
import net.scran24.user.client.survey.flat.rules.SelectUnconfirmedMeal;
import net.scran24.user.client.survey.flat.rules.ShowAssociatedFoodPrompt;
import net.scran24.user.client.survey.flat.rules.ShowBrandNamePrompt;
import net.scran24.user.client.survey.flat.rules.ShowCompoundFoodPrompt;
import net.scran24.user.client.survey.flat.rules.ShowDrinkReminderPrompt;
import net.scran24.user.client.survey.flat.rules.ShowEditIngredientsPrompt;
import net.scran24.user.client.survey.flat.rules.ShowEditMeal;
import net.scran24.user.client.survey.flat.rules.ShowEmptySurveyPrompt;
import net.scran24.user.client.survey.flat.rules.ShowEnergyValidationPrompt;
import net.scran24.user.client.survey.flat.rules.AskIfHomeRecipe;
import net.scran24.user.client.survey.flat.rules.ShowTemplateRecipeSavePrompt;
import net.scran24.user.client.survey.flat.rules.ShowHomeRecipeServingsPrompt;
import net.scran24.user.client.survey.flat.rules.ShowReadyMealsPrompt;
import net.scran24.user.client.survey.flat.rules.ShowSameAsBeforePrompt;
import net.scran24.user.client.survey.flat.rules.ShowSimpleHomeRecipePrompt;
import net.scran24.user.client.survey.flat.rules.ShowTimeGapPrompt;
import net.scran24.user.client.survey.flat.rules.SplitFood;
import net.scran24.user.client.survey.flat.rules.experimental.ShowNextPortionSizeStep;
import net.scran24.user.client.survey.portionsize.experimental.DefaultPortionSizeScripts;
import net.scran24.user.client.survey.portionsize.experimental.PortionSizeScriptManager;
import net.scran24.user.client.survey.prompts.MealOperation;
import net.scran24.user.shared.TemplateFoodData;
import net.scran24.user.shared.FoodEntry;
import net.scran24.user.shared.Meal;
import net.scran24.user.shared.WithPriority;

import org.pcollections.client.HashTreePMap;
import org.pcollections.client.HashTreePSet;
import org.pcollections.client.PVector;
import org.pcollections.client.TreePVector;
import org.workcraft.gwt.shared.client.Callback;
import org.workcraft.gwt.shared.client.Callback1;
import org.workcraft.gwt.shared.client.Callback2;
import org.workcraft.gwt.shared.client.Function1;
import org.workcraft.gwt.shared.client.Option;
import org.workcraft.gwt.shared.client.Pair;

import com.google.gwt.user.client.ui.Anchor;

/**
 * Basic implementation of a survey scheme.
 * Uses the default prompt rules (see <b>defaultRules</b>), 
 * and starting meals (see <b>startingSurveyData</b>).
 */
public abstract class BasicScheme implements SurveyScheme {
	final static double MAX_AGE_HOURS = 8.0;
	
	final protected SurveyInterfaceManager interfaceManager;
	final protected StateManager stateManager;
	final protected PromptManager defaultPromptManager;
	final protected SelectionManager defaultSelectionManager;
	final protected PortionSizeScriptManager defaultScriptManager;
	final protected CompoundFoodTemplateManager defaultTemplateManager;
	final protected RecipeManager recipeManager;
	final protected LogRecorder log;
	
	final protected TreePVector<Function1<Survey, Survey>> basicPostProcess = TreePVector.<Function1<Survey, Survey>> empty().plus(new ProcessMilkInHotDrinks());
	
	private static Logger logger = Logger.getLogger("BasicScheme"); 
	
	protected Survey startingSurveyData() {
		return new Survey(PredefinedMeals.startingMeals, new Selection.EmptySelection(
				SelectionMode.AUTO_SELECTION), System.currentTimeMillis(), HashTreePSet.<String>empty(), HashTreePMap.<String, String>empty());
	}
	
	protected Survey postProcess(Survey data, PVector<Function1<Survey, Survey>> functions) {
		Survey postProcessed = data;

		for (Function1<Survey, Survey> f : functions)
			postProcessed = f.apply(postProcessed);
		
		return postProcessed;		
	}
	
	protected Rules defaultRules(PortionSizeScriptManager scriptManager, CompoundFoodTemplateManager templateManager, RecipeManager recipeManager) {
		return new Rules(
				// meal prompts
				TreePVector.<WithPriority<PromptRule<Meal, MealOperation>>> empty()
				.plus(AskForMealTime.withPriority(3))
				.plus(ShowEditMeal.withPriority(2))
				.plus(ShowDrinkReminderPrompt.withPriority(1))
				.plus(ShowReadyMealsPrompt.withPriority(0)),

				// food prompts
				TreePVector.<WithPriority<PromptRule<FoodEntry, FoodOperation>>> empty()
				.plus(ShowBrandNamePrompt.withPriority(-1))
				.plus(ShowNextPortionSizeStep.withPriority(scriptManager, 0))
				.plus(ChoosePortionSizeMethod.withPriority(1))
				.plus(AskForMissingFoodDescription.withPriority(2))
				.plus(ShowSimpleHomeRecipePrompt.withPriority(2))
				.plus(AskIfHomeRecipe.withPriority(3))
				.plus(SplitFood.withPriority(4))
				.plus(InformFoodComplete.withPriority(-100)),

				// extended food propmts
				TreePVector.<WithPriority<PromptRule<Pair<FoodEntry, Meal>, MealOperation>>> empty()
				.plus(ShowEditIngredientsPrompt.withPriority(3))
				.plus(AskToLookupFood.withPriority(3, recipeManager))
				.plus(ShowSameAsBeforePrompt.withPriority(3, getSchemeId(), getDataVersion(), scriptManager, templateManager))
				.plus(ShowHomeRecipeServingsPrompt.withPriority(2))				
				.plus(ShowTemplateRecipeSavePrompt.withPriority(1, recipeManager))
				.plus(ShowCompoundFoodPrompt.withPriority(0))
				.plus(ShowAssociatedFoodPrompt.withPriority(0))
				
				,
				// global prompts

				TreePVector.<WithPriority<PromptRule<Survey, SurveyOperation>>> empty()
				.plus(ConfirmCompletion.withPriority(0))
				.plus(ShowEnergyValidationPrompt.withPriority(1, 500.0))
				.plus(ShowEmptySurveyPrompt.withPriority(1))
				.plus(ShowTimeGapPrompt.withPriority(2, 180, new Time(9,0), new Time(21,0)))
				
				,

				// selection rules
				TreePVector.<WithPriority<SelectionRule>> empty()
				.plus(SelectForPortionSize.withPriority(3))
				.plus(SelectRawFood.withPriority(2))
				.plus(SelectFoodForAssociatedPrompts.withPriority(1))
				.plus(SelectIncompleteFreeEntryMeal.withPriority(1))
				.plus(SelectMealWithNoDrink.withPriority(1))
				.plus(SelectUnconfirmedMeal.withPriority(1))
		    .plus(SelectMealForReadyMeals.withPriority(1)));
	}



	public BasicScheme(final SurveyInterfaceManager interfaceManager) {
		this.log = new LogRecorder();
		this.interfaceManager = interfaceManager;
			
		interfaceManager.setCallbacks(new Callback1<Survey>() {
			@Override
			public void call(Survey updatedState) {
				if (updatedState.flags.contains(Survey.FLAG_SKIP_HISTORY))
					stateManager.updateState(updatedState.clearFlag(Survey.FLAG_SKIP_HISTORY), false);
				else
					stateManager.updateState(updatedState, true);
				showNextPage();
			}
		}, new Callback2<Survey, Boolean>() {
			@Override
			public void call(Survey updatedState, Boolean makeHistoryEntry) {
				stateManager.updateState(updatedState, makeHistoryEntry);
			}
		});
		
		defaultScriptManager = new PortionSizeScriptManager(DefaultPortionSizeScripts.getCtors());
		
		defaultTemplateManager = new CompoundFoodTemplateManager(HashTreePMap.<String, TemplateFoodData> empty()
				.plus("sandwich", FoodTemplates.sandwich).plus("salad", FoodTemplates.salad));
		
		recipeManager = new RecipeManager(getSchemeId(), getDataVersion(), defaultScriptManager, defaultTemplateManager);
	
		final Rules rules = defaultRules(defaultScriptManager, defaultTemplateManager, recipeManager);
		
		defaultPromptManager = new RuleBasedPromptManager(rules);
		
		// final SelectionManager selectionManager = new RuleBasedSelectionManager(rules.selectionRules);
		
		defaultSelectionManager = new PromptAvailabilityBasedSelectionManager(defaultPromptManager);

		Survey initialState = StateManagerUtil.getLatestState(CurrentUser.getUserInfo().userName, getSchemeId(), getDataVersion(), defaultScriptManager, defaultTemplateManager).accept(new Option.Visitor<Survey, Survey>() {
			@Override
			public Survey visitSome(Survey data) {
				double age = (System.currentTimeMillis() - data.startTime) / 3600000.0;
				logger.info("Saved state is " + age + " hours old.");
				
				if (age > MAX_AGE_HOURS) {
					logger.info("Saved state is older than " + MAX_AGE_HOURS + " hours and has expired.");
					return startingSurveyData();
				}
				else {
					return data.clearCompletionConfirmed().clearEnergyValueConfirmed();
				}
			}

			@Override
			public Survey visitNone() {
				//log.info("No saved state, starting new survey.");
				return startingSurveyData();
			}
		});
		
		stateManager = new StateManager(initialState, getSchemeId(), getDataVersion(), new Callback() {
			@Override
			public void call() {
				showNextPage();
			}
		}, defaultScriptManager);
	}
	
	@Override
	public abstract void showNextPage();
	
	@Override
	public abstract String getDataVersion();

	@Override
	public abstract String getSchemeId();
	
	@Override
	public List<Anchor> navBarLinks() {
		return Collections.emptyList();		
	}
}