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
import static org.workcraft.gwt.shared.client.CollectionUtils.zipWithIndex;
import net.scran24.user.client.survey.flat.Selection;
import net.scran24.user.client.survey.flat.Selection.SelectedFood;
import net.scran24.user.client.survey.flat.SelectionRule;
import net.scran24.user.client.survey.flat.SelectionRuleUtil;
import net.scran24.user.client.survey.flat.SelectionMode;
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

public class SelectIncompleteTemplateFood implements SelectionRule {

	public PVector<Integer> incompleteTemplateFoods(final PVector<FoodEntry> foods) {
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
						return Option.none();
					}

					@Override
					public Option<Integer> visitTemplate(TemplateFood food) {
						if (food.isTemplateComplete())
							return Option.none();
						else
							return Option.some(argument.index);
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
			PVector<Integer> incomplete = incompleteTemplateFoods(state.meals.get(selectedMealIndex).foods);
			if (!incomplete.isEmpty())
				return Option.<Selection> some(new SelectedFood(selectedMealIndex, incomplete.get(0), SelectionMode.AUTO_SELECTION));
		}

		PVector<PVector<Integer>> incomplete = map(state.mealsSortedByTime, new Function1<WithIndex<Meal>, PVector<Integer>>() {
			@Override
			public PVector<Integer> apply(WithIndex<Meal> argument) {
				return incompleteTemplateFoods(argument.value.foods);
			}
		});

		int index = indexOf(incomplete, new Function1<PVector<Integer>, Boolean>() {
			@Override
			public Boolean apply(PVector<Integer> argument) {
				return !argument.isEmpty();
			}
		});

		if (index == -1)
			return new Option.None<Selection>();
		else
			return new Option.Some<Selection>(new Selection.SelectedFood(state.mealsSortedByTime.get(index).index, incomplete.get(index).get(0),
					SelectionMode.AUTO_SELECTION));
	}

	@Override
	public String toString() {
		return "Select incomplete template food";
	}

	public static WithPriority<SelectionRule> withPriority(int priority) {
		return new WithPriority<SelectionRule>(new SelectIncompleteTemplateFood(), priority);
	}
}