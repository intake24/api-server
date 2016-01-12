/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey.flat.rules;


import net.scran24.user.client.survey.flat.Prompt;
import net.scran24.user.client.survey.flat.PromptRule;
import net.scran24.user.client.survey.flat.SelectionType;
import net.scran24.user.client.survey.prompts.EditRecipeIngredientsPrompt;
import net.scran24.user.client.survey.prompts.MealOperation;
import net.scran24.user.shared.CompoundFood;
import net.scran24.user.shared.FoodEntry;
import net.scran24.user.shared.Meal;
import net.scran24.user.shared.WithPriority;

import org.pcollections.client.PSet;
import org.workcraft.gwt.shared.client.Option;
import org.workcraft.gwt.shared.client.Pair;

public class ShowEditIngredientsPrompt implements PromptRule<Pair<FoodEntry, Meal>, MealOperation> {
	@Override
	public Option<Prompt<Pair<FoodEntry, Meal>, MealOperation>> apply(Pair<FoodEntry, Meal> data, SelectionType selectionType, PSet<String> surveyFlags) {
		
		boolean markedAsComplete = data.left.flags.contains(CompoundFood.FLAG_INGREDIENTS_COMPLETE);
		
		// Make this prompt show up if all ingredients were deleted after completion
		boolean noLinkedFoods = Meal.linkedFoods(data.right.foods, data.left).isEmpty();
		
		if (data.left.isCompound() && (noLinkedFoods || !markedAsComplete)) {
			return new Option.Some<Prompt<Pair<FoodEntry, Meal>, MealOperation>>(new EditRecipeIngredientsPrompt(data.right, data.right.foodIndex(data.left)));
		} else {
			return new Option.None<Prompt<Pair<FoodEntry, Meal>, MealOperation>>();
		}
	}

	@Override
	public String toString() {
		return "Show edit recipe ingredients prompt";
	}

	public static WithPriority<PromptRule<Pair<FoodEntry, Meal>, MealOperation>> withPriority(int priority) {
		return new WithPriority<PromptRule<Pair<FoodEntry, Meal>, MealOperation>>(new ShowEditIngredientsPrompt(), priority);
	}
}