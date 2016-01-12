/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey.flat;

import net.scran24.user.client.survey.flat.Selection.EmptySelection;
import net.scran24.user.client.survey.flat.Selection.SelectedFood;
import net.scran24.user.client.survey.flat.Selection.SelectedMeal;
import net.scran24.user.client.survey.prompts.MealOperation;
import net.scran24.user.shared.FoodEntry;
import net.scran24.user.shared.Meal;
import net.scran24.user.shared.WithPriority;

import org.workcraft.gwt.shared.client.Function1;
import org.workcraft.gwt.shared.client.Option;
import org.workcraft.gwt.shared.client.Pair;

public class RuleBasedPromptManager implements PromptManager {
	private final PromptGenerator<Meal, MealOperation> mealPromptGenerator;
	private final PromptGenerator<FoodEntry, FoodOperation> foodPromptGenerator;
	private final PromptGenerator<Pair<FoodEntry, Meal>, MealOperation> extendedFoodPromptGenerator;
	private final PromptGenerator<Survey, SurveyOperation> surveyPromptGenerator;
	
	private final Function1<WithPriority<Prompt<Survey, SurveyOperation>>, Prompt<Survey, SurveyOperation>> stripPriority = new Function1<WithPriority<Prompt<Survey, SurveyOperation>>, Prompt<Survey, SurveyOperation>>() {
		@Override
		public Prompt<Survey, SurveyOperation> apply(WithPriority<Prompt<Survey, SurveyOperation>> argument) {
			return argument.value;
		}
	};

	public RuleBasedPromptManager(Rules rules) {
		mealPromptGenerator = new DefaultPromptGenerator<Meal, MealOperation>(rules.mealPromptRules);
		foodPromptGenerator = new DefaultPromptGenerator<FoodEntry, FoodOperation>(rules.foodPromptRules);
		surveyPromptGenerator = new DefaultPromptGenerator<Survey, SurveyOperation>(rules.surveyPromptRules);
		extendedFoodPromptGenerator = new DefaultPromptGenerator<Pair<FoodEntry, Meal>, MealOperation>(rules.extendedFoodPromptRules);
	
	}
	
	public Option<Prompt<Survey, SurveyOperation>> nextPromptForSelection(final Survey state) {
		return state.selectedElement.accept(new Selection.Visitor<Option<Prompt<Survey, SurveyOperation>>>() {
			@Override
			public Option<Prompt<Survey, SurveyOperation>> visitMeal(final SelectedMeal meal) {
				return mealPromptGenerator.nextPrompt(state.meals.get(meal.mealIndex), state.selectedElement, state.flags).map(
						new Function1<WithPriority<Prompt<Meal, MealOperation>>, Prompt<Survey, SurveyOperation>>() {
							@Override
							public Prompt<Survey, SurveyOperation> apply(WithPriority<Prompt<Meal, MealOperation>> argument) {
								return new PromptAdapter.ForMeal(meal.mealIndex, argument.value);
							}
						});
			}

			@Override
			public Option<Prompt<Survey, SurveyOperation>> visitFood(final SelectedFood food) {
				Meal selectedMeal = state.meals.get(food.mealIndex);
				FoodEntry selectedFood = selectedMeal.foods.get(food.foodIndex);

				Option<WithPriority<Prompt<Survey, SurveyOperation>>> nextSimplePrompt = foodPromptGenerator.nextPrompt(selectedFood, state.selectedElement, state.flags)
						.map(new Function1<WithPriority<Prompt<FoodEntry, FoodOperation>>, WithPriority<Prompt<Survey, SurveyOperation>>>() {
					@Override
					public WithPriority<Prompt<Survey, SurveyOperation>> apply(WithPriority<Prompt<FoodEntry, FoodOperation>> argument) {
						return new WithPriority<Prompt<Survey, SurveyOperation>>(new PromptAdapter.ForFood(food.mealIndex, food.foodIndex, argument.value),
								argument.priority);
					}
				});

				Option<WithPriority<Prompt<Survey, SurveyOperation>>> nextExtendedPrompt = extendedFoodPromptGenerator.nextPrompt(
						Pair.create(selectedFood, selectedMeal), state.selectedElement, state.flags).map(
						new Function1<WithPriority<Prompt<Pair<FoodEntry, Meal>, MealOperation>>, WithPriority<Prompt<Survey, SurveyOperation>>>() {
							@Override
							public WithPriority<Prompt<Survey, SurveyOperation>> apply(WithPriority<Prompt<Pair<FoodEntry, Meal>, MealOperation>> argument) {
								return new WithPriority<Prompt<Survey, SurveyOperation>>(new PromptAdapter.ForFoodExtended(food.mealIndex, argument.value),
										argument.priority);
							}
						});

				if (nextSimplePrompt.isEmpty()) {
					return nextExtendedPrompt.map(stripPriority);
				} else if (nextExtendedPrompt.isEmpty()) {
					return nextSimplePrompt.map(stripPriority);
				} else {
					WithPriority<Prompt<Survey, SurveyOperation>> p1 = nextSimplePrompt.getOrDie();
					WithPriority<Prompt<Survey, SurveyOperation>> p2 = nextExtendedPrompt.getOrDie();

					if (p1.priority > p2.priority)
						return nextSimplePrompt.map(stripPriority);
					else
						return nextExtendedPrompt.map(stripPriority);
				}
			}

			@Override
			public Option<Prompt<Survey, SurveyOperation>> visitNothing(final EmptySelection selection) {
				return new Option.None<Prompt<Survey, SurveyOperation>>();
			}
		});
	}

	public Option<Prompt<Survey, SurveyOperation>> nextGlobalPrompt(final Survey state) {
		return surveyPromptGenerator.nextPrompt(state, state.selectedElement, state.flags).map(stripPriority);
	}
}