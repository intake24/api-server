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
import net.scran24.user.client.survey.prompts.MealCompletePrompt;
import net.scran24.user.client.survey.prompts.MealOperation;
import net.scran24.user.shared.Meal;
import net.scran24.user.shared.WithPriority;

import org.pcollections.client.PSet;
import org.workcraft.gwt.shared.client.Option;

public class InformMealComplete implements PromptRule<Meal, MealOperation> {
	@Override
	public Option<Prompt<Meal, MealOperation>> apply(final Meal state, SelectionMode selectionType, PSet<String> surveyFlag) {
		if (state.freeEntryComplete() && selectionType == SelectionMode.MANUAL_SELECTION)
			return new Option.Some<Prompt<Meal, MealOperation>>(new MealCompletePrompt(state));
		else
			return new Option.None<Prompt<Meal, MealOperation>>();
	}

	@Override
	public String toString() {
		return "Meal complete";
	}

	public static WithPriority<PromptRule<Meal, MealOperation>> withPriority(int priority) {
		return new WithPriority<PromptRule<Meal, MealOperation>>(new InformMealComplete(), priority);
	}
}