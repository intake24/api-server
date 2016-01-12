/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey.flat.rules;

import static org.workcraft.gwt.shared.client.CollectionUtils.flattenOption;
import static org.workcraft.gwt.shared.client.CollectionUtils.indexOf;
import static org.workcraft.gwt.shared.client.CollectionUtils.map;
import static org.workcraft.gwt.shared.client.CollectionUtils.filter;
import static org.workcraft.gwt.shared.client.CollectionUtils.zipWithIndex;
import net.scran24.user.client.survey.flat.Selection;
import net.scran24.user.client.survey.flat.Selection.SelectedFood;
import net.scran24.user.client.survey.flat.SelectionRule;
import net.scran24.user.client.survey.flat.SelectionRuleUtil;
import net.scran24.user.client.survey.flat.SelectionType;
import net.scran24.user.client.survey.flat.Survey;
import net.scran24.user.shared.CompoundFood;
import net.scran24.user.shared.TemplateFood;
import net.scran24.user.shared.EncodedFood;
import net.scran24.user.shared.FoodEntry;
import net.scran24.user.shared.Meal;
import net.scran24.user.shared.MissingFood;
import net.scran24.user.shared.RawFood;
import net.scran24.user.shared.WithPriority;

import org.pcollections.client.PVector;
import org.workcraft.gwt.shared.client.CollectionUtils.WithIndex;
import org.workcraft.gwt.shared.client.Function1;
import org.workcraft.gwt.shared.client.Option;

public class SelectFoodForAssociatedPrompts implements SelectionRule {

	public PVector<Integer> foodsWithPrompts(final PVector<FoodEntry> foods) {
		return flattenOption(map(zipWithIndex(foods), new Function1<WithIndex<FoodEntry>, Option<Integer>>() {
			@Override
			public Option<Integer> apply(final WithIndex<FoodEntry> argument) {
				return argument.value.accept(new FoodEntry.Visitor<Option<Integer>>() {
					@Override
					public Option<Integer> visitRaw(RawFood food) {
						return Option.none();
					}

					@Override
					public Option<Integer> visitEncoded(EncodedFood food) {
						if (food.link.isLinked() || (ShowAssociatedFoodPrompt.applicablePromptIndex(foods, food) == -1))
							return Option.none();
						else
							return Option.some(argument.index);
					}

					@Override
					public Option<Integer> visitTemplate(TemplateFood food) {
						return Option.none();
					}

					@Override
					public Option<Integer> visitMissing(MissingFood food) {
						return Option.none();
					}

					@Override
					public Option<Integer> visitCompound(CompoundFood food) {
						return Option.none();
					}
				});
			}
		}));
	}

	@Override
	public Option<Selection> apply(final Survey state) {
		int selectedMealIndex = SelectionRuleUtil.selectedMealIndex(state);

		if (selectedMealIndex != -1) {
			if (!state.meals.get(selectedMealIndex).encodingComplete())
				return Option.none();
			else {
				PVector<Integer> foodsWithPrompts = foodsWithPrompts(state.meals.get(selectedMealIndex).foods);
				if (!foodsWithPrompts.isEmpty())
					return Option.<Selection> some(new SelectedFood(selectedMealIndex, foodsWithPrompts.get(0), SelectionType.AUTO_SELECTION));
			}
		}

		PVector<PVector<Integer>> prompts = map(filter(state.mealsSortedByTime, new Function1<WithIndex<Meal>, Boolean>() {
			@Override
			public Boolean apply(WithIndex<Meal> argument) {
				return argument.value.encodingComplete();
			}
		}), new Function1<WithIndex<Meal>, PVector<Integer>>() {
			@Override
			public PVector<Integer> apply(WithIndex<Meal> argument) {
				return foodsWithPrompts(argument.value.foods);
			}
		});

		int index = indexOf(prompts, new Function1<PVector<Integer>, Boolean>() {
			@Override
			public Boolean apply(PVector<Integer> argument) {
				return !argument.isEmpty();
			}
		});

		if (index == -1)
			return new Option.None<Selection>();
		else
			return new Option.Some<Selection>(new Selection.SelectedFood(state.mealsSortedByTime.get(index).index, prompts.get(index).get(0),
					SelectionType.AUTO_SELECTION));
	}

	@Override
	public String toString() {
		return "Select food for associated prompts";
	}

	public static WithPriority<SelectionRule> withPriority(int priority) {
		return new WithPriority<SelectionRule>(new SelectFoodForAssociatedPrompts(), priority);
	}
}