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
import net.scran24.user.client.survey.prompts.ConfirmMealPrompt;
import net.scran24.user.client.survey.prompts.MealOperation;
import net.scran24.user.shared.Meal;
import net.scran24.user.shared.WithPriority;

import org.pcollections.client.PSet;
import org.workcraft.gwt.shared.client.Option;

public class AskForMealTime implements PromptRule<Meal, MealOperation> {
	@Override
	public Option<Prompt<Meal, MealOperation>> apply(Meal data, SelectionType selectionType, PSet<String> surveyFlags) {
		if (data.time.isEmpty()) {
			return new Option.Some<Prompt<Meal, MealOperation>>(new ConfirmMealPrompt(data));
		} else {
			return new Option.None<Prompt<Meal, MealOperation>>();
		}
	}

	@Override
	public String toString() {
		return "Ask for meal time";
	}
	
	public static WithPriority<PromptRule<Meal, MealOperation>> withPriority(int priority) {
		return new WithPriority<PromptRule<Meal,MealOperation>>(new AskForMealTime(), priority);		
	}
}