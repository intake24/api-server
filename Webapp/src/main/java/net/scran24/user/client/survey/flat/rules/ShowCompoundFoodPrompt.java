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
import net.scran24.user.client.survey.flat.Survey;
import net.scran24.user.client.survey.prompts.CompoundFoodPrompt;
import net.scran24.user.client.survey.prompts.MealOperation;
import net.scran24.user.shared.TemplateFood;
import net.scran24.user.shared.TemplateFoodData.ComponentDef;
import net.scran24.user.shared.TemplateFoodData.ComponentOccurence;
import net.scran24.user.shared.TemplateFoodData.ComponentType;
import net.scran24.user.shared.FoodEntry;
import net.scran24.user.shared.Meal;
import net.scran24.user.shared.WithPriority;

import org.pcollections.client.PSet;
import org.workcraft.gwt.shared.client.Function1;
import org.workcraft.gwt.shared.client.Option;
import org.workcraft.gwt.shared.client.Pair;

public class ShowCompoundFoodPrompt implements PromptRule<Pair<FoodEntry, Meal>, MealOperation> {
	@Override
	public Option<Prompt<Pair<FoodEntry, Meal>, MealOperation>> apply(final Pair<FoodEntry, Meal> pair, SelectionType selectionType, PSet<String> surveyFlags) {
		if (!pair.left.isTemplate() || !surveyFlags.contains(Survey.FLAG_FREE_ENTRY_COMPLETE))
			return Option.none();
		else {
			final TemplateFood food = (TemplateFood) pair.left;
			final Meal meal = pair.right;

			return food.nextComponentIndex().map(new Function1<Integer, Prompt<Pair<FoodEntry, Meal>, MealOperation>>() {
				@Override
				public Prompt<Pair<FoodEntry, Meal>, MealOperation> apply(Integer componentIndex) {
					ComponentDef def  = food.data.template.get(componentIndex);
							
					boolean isFirst = food.components.get(componentIndex).isEmpty();
					boolean allowSkip = (def.type == ComponentType.Optional) || (def.occurence == ComponentOccurence.Multiple && !isFirst); 
					
					return new CompoundFoodPrompt(meal, meal.foodIndex(food), componentIndex, isFirst, allowSkip);
				}
			});
		}
	}

	public static WithPriority<PromptRule<Pair<FoodEntry, Meal>, MealOperation>> withPriority(int priority) {
		return new WithPriority<PromptRule<Pair<FoodEntry, Meal>, MealOperation>>(new ShowCompoundFoodPrompt(), priority);
	}
}