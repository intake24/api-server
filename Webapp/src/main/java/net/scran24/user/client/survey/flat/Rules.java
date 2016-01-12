/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey.flat;

import net.scran24.user.client.survey.prompts.MealOperation;
import net.scran24.user.shared.FoodEntry;
import net.scran24.user.shared.Meal;
import net.scran24.user.shared.WithPriority;

import org.pcollections.client.PVector;
import org.workcraft.gwt.shared.client.Pair;

public class Rules {
	public final PVector<WithPriority<PromptRule<Meal, MealOperation>>> mealPromptRules;
	public final PVector<WithPriority<PromptRule<FoodEntry, FoodOperation>>> foodPromptRules;
	public final PVector<WithPriority<PromptRule<Pair<FoodEntry, Meal>, MealOperation>>> extendedFoodPromptRules;
	public final PVector<WithPriority<PromptRule<Survey, SurveyOperation>>> surveyPromptRules;
	public final PVector<WithPriority<SelectionRule>> selectionRules;
	
	public Rules(
			PVector<WithPriority<PromptRule<Meal, MealOperation>>> mealPromptRules,
			PVector<WithPriority<PromptRule<FoodEntry, FoodOperation>>> foodPromptRules,
			PVector<WithPriority<PromptRule<Pair<FoodEntry, Meal>, MealOperation>>> extendedFoodPromptRules,
			PVector<WithPriority<PromptRule<Survey, SurveyOperation>>> surveyPromptRules, 
			PVector<WithPriority<SelectionRule>> selectionRules) {
		this.mealPromptRules = mealPromptRules;
		this.foodPromptRules = foodPromptRules;
		this.extendedFoodPromptRules = extendedFoodPromptRules;
		this.surveyPromptRules = surveyPromptRules;
		this.selectionRules = selectionRules;
	}
}
