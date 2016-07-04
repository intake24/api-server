/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey.flat.rules;

import net.scran24.user.client.survey.RecipeManager;
import net.scran24.user.client.survey.flat.Prompt;
import net.scran24.user.client.survey.flat.PromptRule;
import net.scran24.user.client.survey.flat.SelectionMode;
import net.scran24.user.client.survey.flat.Survey;
import net.scran24.user.client.survey.prompts.FoodLookupPrompt;
import net.scran24.user.client.survey.prompts.MealOperation;
import net.scran24.user.shared.FoodEntry;
import net.scran24.user.shared.Meal;
import net.scran24.user.shared.WithPriority;

import org.pcollections.client.PSet;
import org.workcraft.gwt.shared.client.Option;
import org.workcraft.gwt.shared.client.Pair;

public class AskToLookupFood implements PromptRule<Pair<FoodEntry, Meal>, MealOperation> {
	
	final private RecipeManager recipeManager;
	final private String locale;
	
	public AskToLookupFood(String locale, RecipeManager recipeManager) {
		this.locale = locale;
		this.recipeManager = recipeManager;
	}

	@Override
	public Option<Prompt<Pair<FoodEntry, Meal>, MealOperation>> apply(Pair<FoodEntry, Meal> data, SelectionMode selectionType, PSet<String> surveyFlags) {
		if (!surveyFlags.contains(Survey.FLAG_FREE_ENTRY_COMPLETE) || data.left.isTemplate() || data.left.isCompound() || data.left.isMissing())
			return Option.none();
		else if (!data.left.isEncoded())
			return Option.<Prompt<Pair<FoodEntry, Meal>, MealOperation>>some(new FoodLookupPrompt(locale, data.left, data.right, recipeManager));
		else
			return Option.none();
	}

	@Override
	public String toString() {
		return "Ask to look up food";
	}

	public static WithPriority<PromptRule<Pair<FoodEntry, Meal>, MealOperation>> withPriority(int priority, String locale, RecipeManager recipeManager) {
		return new WithPriority<PromptRule<Pair<FoodEntry, Meal>, MealOperation>>(new AskToLookupFood(locale, recipeManager), priority);
	}
}