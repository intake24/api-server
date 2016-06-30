/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey.flat.rules;

import static org.workcraft.gwt.shared.client.CollectionUtils.indexOf;



import org.workcraft.gwt.shared.client.Function1;
import org.workcraft.gwt.shared.client.Option;
import org.workcraft.gwt.shared.client.CollectionUtils.WithIndex;

import net.scran24.user.client.survey.flat.Selection;
import net.scran24.user.client.survey.flat.SelectionRule;
import net.scran24.user.client.survey.flat.SelectionMode;
import net.scran24.user.client.survey.flat.Survey;
import net.scran24.user.client.survey.flat.Selection.EmptySelection;
import net.scran24.user.client.survey.flat.Selection.SelectedFood;
import net.scran24.user.client.survey.flat.Selection.SelectedMeal;
import net.scran24.user.shared.FoodEntry;
import net.scran24.user.shared.Meal;
import net.scran24.user.shared.WithPriority;

public class SelectForPortionSize implements SelectionRule {
	@Override
	public Option<Selection> apply(Survey state) {
		if (!state.freeEntryComplete()) {
			return new Option.None<Selection>();
		}
		
		int foodIndex = -1;
		int mealIndex = -1;
		
		int currentMealIndex = state.selectedElement.accept(new Selection.Visitor<Integer>() {
			@Override
			public Integer visitMeal(SelectedMeal meal) {
				return meal.mealIndex;
			}

			@Override
			public Integer visitFood(SelectedFood food) {
				return food.mealIndex;
			}

			@Override
			public Integer visitNothing(EmptySelection selection) {
				return -1;
			}
		});
		
		if (currentMealIndex != -1) // a food or a meal selected
			foodIndex = indexOf(state.meals.get(currentMealIndex).foods, FoodEntry.isPortionSizeUnknown);
		
		if (foodIndex == -1) // no unencoded food in current selection context
		{
			
			// find chronologically first unencoded meal
			int sortedMealIndex = indexOf(state.mealsSortedByTime, new Function1<WithIndex<Meal>, Boolean>() {
				@Override
				public Boolean apply(WithIndex<Meal> argument) {
					return !argument.value.portionSizeComplete();
				}
			}); // TODO: Fix selection for linked foods
			
			if (sortedMealIndex != -1) { // there is an incompletely encoded meal 
				mealIndex = state.mealsSortedByTime.get(sortedMealIndex).index;
				foodIndex = indexOf(state.meals.get(mealIndex).foods, FoodEntry.isPortionSizeUnknown);
			}
		} else {
			mealIndex = currentMealIndex;
		}

		if (foodIndex == -1)
			return new Option.None<Selection>();
		else
			return new Option.Some<Selection>(new Selection.SelectedFood(mealIndex, foodIndex, SelectionMode.AUTO_SELECTION));
	}

	@Override
	public String toString() {
		return "Select food with unknown portion size";
	}
	
	public static WithPriority<SelectionRule> withPriority(int priority) {
		return new WithPriority<SelectionRule>(new SelectForPortionSize(), priority);
	}
}
