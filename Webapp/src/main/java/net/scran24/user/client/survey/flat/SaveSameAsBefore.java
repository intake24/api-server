/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey.flat;

import static org.workcraft.gwt.shared.client.CollectionUtils.forall;
import net.scran24.common.client.CurrentUser;
import net.scran24.user.client.survey.flat.rules.ShowAssociatedFoodPrompt;
import net.scran24.user.shared.EncodedFood;
import net.scran24.user.shared.FoodEntry;
import net.scran24.user.shared.Meal;
import net.scran24.user.shared.UUID;

import org.workcraft.gwt.shared.client.Function1;
import org.workcraft.gwt.shared.client.Function2;
import org.workcraft.gwt.shared.client.Option;
import org.workcraft.gwt.shared.client.Pair;

public class SaveSameAsBefore implements Function2<Survey, Survey, Survey> {
	
	public final String scheme_id;
	public final String version_id;
	
	public SaveSameAsBefore(String scheme_id, String version_id) {
		this.scheme_id = scheme_id;
		this.version_id = version_id;
	}

	private Option<Pair<Meal, FoodEntry>> findById(Survey survey, UUID id) {
		for (Meal m : survey.meals) {
			int i = m.foodIndex(id);
			if (i != -1)
				return Option.some(Pair.create(m, m.foods.get(i)));
		}

		return Option.none();
	}

	private boolean portionSizeComplete(Meal meal, EncodedFood food) {
		if (food.data.prompts.isEmpty())
			return food.isPortionSizeComplete();
		else {
			boolean promptsLeft = ShowAssociatedFoodPrompt.applicablePromptIndex(meal.foods, food) != -1;
			boolean linkedPortionSizeComplete = forall(Meal.linkedFoods(meal.foods, food), new Function1<FoodEntry, Boolean>() {
				@Override
				public Boolean apply(FoodEntry argument) {
					return argument.isPortionSizeComplete();
				}
			});

			return !promptsLeft && linkedPortionSizeComplete;
		}
	}

	@Override
	public Survey apply(Survey s0, final Survey s1) {
		for (final Meal m : s1.meals)
			for (int i = 0; i < m.foods.size(); i++) {
				final FoodEntry newState = m.foods.get(i);
				if (newState.isEncoded()) {
					final EncodedFood encNew = newState.asEncoded();

					if (encNew.data.sameAsBeforeOption) {
						findById(s0, newState.link.id).accept(new Option.SideEffectVisitor<Pair<Meal, FoodEntry>>() {
							@Override
							public void visitSome(Pair<Meal, FoodEntry> oldState) {
								if (oldState.right.isEncoded()) {
									EncodedFood encOld = oldState.right.asEncoded();
									if (!portionSizeComplete(oldState.left, encOld) && portionSizeComplete(m, encNew)) {
										StateManagerUtil.saveSameAsBefore(CurrentUser.getUserInfo().userName, m, encNew, scheme_id, version_id);
									}
								}
							}

							@Override
							public void visitNone() {

							}
						});
					}
				}
			}

		return s1;
	}
}
