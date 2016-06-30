/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey.flat.rules;

import net.scran24.user.client.survey.flat.Prompt;
import net.scran24.user.client.survey.flat.PromptRule;
import net.scran24.user.client.survey.flat.SelectionMode;
import net.scran24.user.client.survey.prompts.DrinkReminderPrompt;
import net.scran24.user.client.survey.prompts.MealOperation;
import net.scran24.user.shared.Meal;
import net.scran24.user.shared.WithPriority;

import org.pcollections.client.PSet;
import org.workcraft.gwt.shared.client.Option;

public class ShowDrinkReminderPrompt implements PromptRule<Meal, MealOperation> {
	
	@Override
	public Option<Prompt<Meal, MealOperation>> apply(final Meal meal, SelectionMode selectionType, PSet<String> surveyFlags) {
		if (!meal.encodingComplete() || meal.hasDrinks() || meal.confirmedNoDrinks())
			return Option.none();
		else {
			return Option.<Prompt<Meal, MealOperation>>some(new DrinkReminderPrompt(meal)); 
		}
	}

	public static WithPriority<PromptRule<Meal, MealOperation>> withPriority(int priority) {
		return new WithPriority<PromptRule<Meal, MealOperation>>(new ShowDrinkReminderPrompt(), priority);
	}
}