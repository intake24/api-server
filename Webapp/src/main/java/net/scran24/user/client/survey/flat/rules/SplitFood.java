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
import net.scran24.user.client.survey.prompts.SplitFoodPrompt;
import net.scran24.user.shared.CompoundFood;
import net.scran24.user.shared.TemplateFood;
import net.scran24.user.shared.EncodedFood;
import net.scran24.user.shared.FoodEntry;
import net.scran24.user.shared.MissingFood;
import net.scran24.user.shared.RawFood;
import net.scran24.user.shared.WithPriority;

import org.pcollections.client.PSet;
import org.workcraft.gwt.shared.client.Option;

public class SplitFood implements PromptRule<FoodEntry, FoodOperation> {
	@Override
	public Option<Prompt<FoodEntry, FoodOperation>> apply(FoodEntry data, SelectionType selectionType, PSet<String> surveyFlags) {
		if (!surveyFlags.contains(Survey.FLAG_FREE_ENTRY_COMPLETE))
			return Option.none();
		else
			return data.accept(new FoodEntry.Visitor<Option<Prompt<FoodEntry, FoodOperation>>>() {
				@Override
				public Option<Prompt<FoodEntry, FoodOperation>> visitRaw(RawFood food) {
					if (food.applySplit())
						return Option.<Prompt<FoodEntry, FoodOperation>> some(new SplitFoodPrompt(food));
					else
						return Option.none();
				}

				@Override
				public Option<Prompt<FoodEntry, FoodOperation>> visitEncoded(final EncodedFood food) {
					return Option.none();
				}

				@Override
				public Option<Prompt<FoodEntry, FoodOperation>> visitTemplate(TemplateFood food) {
					return Option.none();
				}

				@Override
				public Option<Prompt<FoodEntry, FoodOperation>> visitMissing(MissingFood food) {
					return Option.none();
				}

				@Override
				public Option<Prompt<FoodEntry, FoodOperation>> visitCompound(CompoundFood food) {
					return Option.none();
				}
			});
	}

	@Override
	public String toString() {
		return "Try to split the food description";
	}

	public static WithPriority<PromptRule<FoodEntry, FoodOperation>> withPriority(int priority) {
		return new WithPriority<PromptRule<FoodEntry, FoodOperation>>(new SplitFood(), priority);
	}
}