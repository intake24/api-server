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
import net.scran24.user.client.survey.flat.SelectionMode;
import net.scran24.user.client.survey.flat.Survey;
import net.scran24.user.client.survey.prompts.ChoosePortionSizeMethodPrompt;
import net.scran24.user.client.survey.prompts.UnknownPortionSizeMethodPrompt;
import net.scran24.user.shared.EncodedFood;
import net.scran24.user.shared.FoodEntry;
import net.scran24.user.shared.WithPriority;

import org.pcollections.client.PSet;
import org.workcraft.gwt.shared.client.Option;

public class ChoosePortionSizeMethod implements PromptRule<FoodEntry, FoodOperation> {
	@Override
	public Option<Prompt<FoodEntry, FoodOperation>> apply(final FoodEntry state, SelectionMode selectionType, PSet<String> surveyFlags) {
		if (!surveyFlags.contains(Survey.FLAG_FREE_ENTRY_COMPLETE) || !state.isEncoded())
			return new Option.None<Prompt<FoodEntry, FoodOperation>>();
		else {
			EncodedFood f = (EncodedFood)state;
			
			if (!f.portionSize.isEmpty())
				return Option.none();
			else if (!f.portionSizeMethodIndex.isEmpty())
				return Option.none();
			else if (f.data.portionSizeMethods.size() == 0)
				return Option.<Prompt<FoodEntry, FoodOperation>>some(new UnknownPortionSizeMethodPrompt(f.description()));
			else if (f.data.portionSizeMethods.size() == 1)
				return Option.none();
			else
				return Option.<Prompt<FoodEntry, FoodOperation>>some(new ChoosePortionSizeMethodPrompt(f));
		}
	}

	@Override
	public String toString() {
		return "Choose portion size method";
	}
	
	public static WithPriority<PromptRule<FoodEntry, FoodOperation>> withPriority(int priority) {
		return new WithPriority<PromptRule<FoodEntry, FoodOperation>>(new ChoosePortionSizeMethod(), priority);		
	}
}