/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey.flat.rules;

import org.pcollections.client.PSet;
import org.workcraft.gwt.shared.client.Option;

import net.scran24.user.client.survey.flat.FoodOperation;
import net.scran24.user.client.survey.flat.Prompt;
import net.scran24.user.client.survey.flat.PromptRule;
import net.scran24.user.client.survey.flat.SelectionMode;
import net.scran24.user.client.survey.flat.Survey;
import net.scran24.user.client.survey.prompts.FoodCompletePrompt;
import net.scran24.user.shared.CompoundFood;
import net.scran24.user.shared.TemplateFood;
import net.scran24.user.shared.FoodEntry;
import net.scran24.user.shared.MissingFood;
import net.scran24.user.shared.WithPriority;

public class InformFoodComplete implements PromptRule<FoodEntry, FoodOperation> {
	@Override
	public Option<Prompt<FoodEntry, FoodOperation>> apply(final FoodEntry state, SelectionMode selectionType, PSet<String> surveyFlags) {
		if (
				(!surveyFlags.contains(Survey.FLAG_FREE_ENTRY_COMPLETE) && selectionType == SelectionMode.MANUAL_SELECTION) ||
				(surveyFlags.contains(Survey.FLAG_FREE_ENTRY_COMPLETE) && state.isEncoded() && selectionType == SelectionMode.MANUAL_SELECTION) ||
				(surveyFlags.contains(Survey.FLAG_FREE_ENTRY_COMPLETE) && state.isTemplate() && ((TemplateFood)state).isTemplateComplete() && selectionType == SelectionMode.MANUAL_SELECTION) ||
				(surveyFlags.contains(Survey.FLAG_FREE_ENTRY_COMPLETE) && state.isCompound() && ((CompoundFood)state).flags.contains(CompoundFood.FLAG_INGREDIENTS_COMPLETE) && selectionType == SelectionMode.MANUAL_SELECTION) ||
				(surveyFlags.contains(Survey.FLAG_FREE_ENTRY_COMPLETE) && state.isMissing() && ((MissingFood)state).isDescriptionComplete() && selectionType == SelectionMode.MANUAL_SELECTION)
			)
			return new Option.Some<Prompt<FoodEntry, FoodOperation>>(new FoodCompletePrompt(state));
		else
			return new Option.None<Prompt<FoodEntry, FoodOperation>>();
	}

	@Override
	public String toString() {
		return "Food complete";
	}
	
	public static WithPriority<PromptRule<FoodEntry, FoodOperation>> withPriority(int priority) {
		return new WithPriority<PromptRule<FoodEntry, FoodOperation>>(new InformFoodComplete(), priority);		
	}
}