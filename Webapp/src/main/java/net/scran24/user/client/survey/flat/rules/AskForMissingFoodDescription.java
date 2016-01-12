/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey.flat.rules;

import net.scran24.user.client.survey.flat.FoodOperation;
import net.scran24.user.client.survey.flat.Prompt;
import net.scran24.user.client.survey.flat.PromptRule;
import net.scran24.user.client.survey.flat.SelectionType;
import net.scran24.user.client.survey.flat.Survey;
import net.scran24.user.client.survey.prompts.MissingFoodDescriptionPrompt;
import net.scran24.user.shared.FoodEntry;
import net.scran24.user.shared.MissingFood;
import net.scran24.user.shared.WithPriority;

import org.pcollections.client.PSet;
import org.workcraft.gwt.shared.client.Option;

public class AskForMissingFoodDescription implements PromptRule<FoodEntry, FoodOperation> {
	@Override
	public Option<Prompt<FoodEntry, FoodOperation>> apply(FoodEntry data, SelectionType selectionType, PSet<String> surveyFlags) {
		if (!surveyFlags.contains(Survey.FLAG_FREE_ENTRY_COMPLETE))
			return new Option.None<Prompt<FoodEntry, FoodOperation>>();
		else if (data.isMissing() && data.flags.contains(MissingFood.NOT_HOME_RECIPE_FLAG) && !((MissingFood)data).isDescriptionComplete())
			return new Option.Some<Prompt<FoodEntry, FoodOperation>>(new MissingFoodDescriptionPrompt((MissingFood)data));
		else
			return new Option.None<Prompt<FoodEntry, FoodOperation>>();
	}

	@Override
	public String toString() {
		return "Ask for missing food description";
	}

	public static WithPriority<PromptRule<FoodEntry, FoodOperation>> withPriority(int priority) {
		return new WithPriority<PromptRule<FoodEntry, FoodOperation>>(new AskForMissingFoodDescription(), priority);
	}
}