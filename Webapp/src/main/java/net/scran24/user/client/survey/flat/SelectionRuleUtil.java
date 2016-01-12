/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey.flat;

import net.scran24.user.client.survey.flat.Selection.EmptySelection;
import net.scran24.user.client.survey.flat.Selection.SelectedFood;
import net.scran24.user.client.survey.flat.Selection.SelectedMeal;

public class SelectionRuleUtil {
	public static int selectedMealIndex(Survey survey) {
		return survey.selectedElement.accept(new Selection.Visitor<Integer>() {
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
	}
}
