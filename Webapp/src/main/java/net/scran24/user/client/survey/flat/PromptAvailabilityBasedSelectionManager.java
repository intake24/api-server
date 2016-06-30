/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey.flat;

import static org.workcraft.gwt.shared.client.CollectionUtils.chooseFirst;
import net.scran24.user.client.survey.flat.Selection.EmptySelection;
import net.scran24.user.client.survey.flat.Selection.SelectedFood;
import net.scran24.user.client.survey.flat.Selection.SelectedMeal;
import net.scran24.user.client.survey.flat.Selection.Visitor;

import org.pcollections.client.TreePVector;
import org.workcraft.gwt.shared.client.Option;

public class PromptAvailabilityBasedSelectionManager implements SelectionManager {
	private PromptManager promptManager;
	// private Logger log = Logger.getLogger("PromptAvailabilityBasedSelectionManager");

	public PromptAvailabilityBasedSelectionManager(PromptManager promptManager) {
		this.promptManager = promptManager;
	}
	
	private boolean hasPrompts (final Survey state, final int mealIndex) {
		return !promptManager.nextPromptForSelection(state.withSelection(new SelectedMeal(mealIndex, SelectionMode.AUTO_SELECTION))).isEmpty();		
	}
	
	private boolean hasPrompts (final Survey state, final int mealIndex, final int foodIndex) {
		return !promptManager.nextPromptForSelection(state.withSelection(new SelectedFood(mealIndex, foodIndex, SelectionMode.AUTO_SELECTION))).isEmpty();
	}

	private Option<Selection> tryMeal (final Survey state, final int mealIndex) {
		// log.log(Level.INFO, "Trying meal " + mealIndex);
		
		if (hasPrompts(state, mealIndex))
			return Option.<Selection>some(new SelectedMeal(mealIndex, SelectionMode.AUTO_SELECTION));
		else
			return Option.none();
	}
	
	private Option<Selection> tryAnyFood(final Survey state, final int mealIndex) {
		for (int foodIndex = 0; foodIndex < state.meals.get(mealIndex).foods.size(); foodIndex++) 
			if (hasPrompts(state, mealIndex, foodIndex))
					return Option.<Selection>some(new SelectedFood(mealIndex, foodIndex, SelectionMode.AUTO_SELECTION));
		return Option.none();
	}
	
	private Option<Selection> tryFollowUpFood(final Survey state, final int mealIndex, final int currentFoodIndex) {
		for (int foodIndex = currentFoodIndex + 1; foodIndex < state.meals.get(mealIndex).foods.size(); foodIndex++) 
			if (hasPrompts(state, mealIndex, foodIndex))
					return Option.<Selection>some(new SelectedFood(mealIndex, foodIndex, SelectionMode.AUTO_SELECTION));
		return Option.none();
	}
	
	private Option<Selection> tryFoodInFollowUpMeal(final Survey state, final int currentMealIndex) {
		for (int mealIndex = currentMealIndex + 1; mealIndex < state.meals.size(); mealIndex++) {
			Option<Selection> sel = tryAnyFood(state, mealIndex);
			if (!sel.isEmpty())
				return sel;
		}
		return Option.none();
	}
	
	private Option<Selection> tryFoodInAnyMeal(final Survey state) {
		for (int mealIndex = 0; mealIndex < state.meals.size(); mealIndex++) {
			Option<Selection> sel = tryAnyFood(state, mealIndex);
			if (!sel.isEmpty())
				return sel;
		}
		return Option.none();
	}
	
	private Option<Selection> tryAnyMeal(final Survey state) {
		// log.log(Level.INFO, "Trying any meal");
		
		for (int mealIndex = 0; mealIndex < state.meals.size(); mealIndex++) {
			Option<Selection> sel = tryMeal(state, mealIndex);
			if (!sel.isEmpty())
				return sel;
		}
		return Option.none();
	}
	
	
	@Override
	public Option<Selection> nextSelection(final Survey state) {
 		// log.log(Level.INFO, "Choosing next selection");
 		
 		return state.selectedElement.accept(new Visitor<Option<Selection>>() {
			@Override
			public Option<Selection> visitMeal(SelectedMeal selection) {
				return chooseFirst(
						TreePVector.<Option<Selection>>empty()
						.plus(tryAnyFood(state, selection.mealIndex))
						.plus(tryFoodInFollowUpMeal(state, selection.mealIndex))
						.plus(tryFoodInAnyMeal(state))
						.plus(tryAnyMeal(state))
						);
				}

			@Override
			public Option<Selection> visitFood(SelectedFood selection) {
				return chooseFirst(
						TreePVector.<Option<Selection>>empty()
						.plus(tryFollowUpFood(state, selection.mealIndex, selection.foodIndex))
						.plus(tryAnyFood(state, selection.mealIndex))
						.plus(tryMeal(state, selection.mealIndex))
						.plus(tryFoodInFollowUpMeal(state, selection.mealIndex))
						.plus(tryFoodInAnyMeal(state))
						.plus(tryAnyMeal(state))
						);
			}

			@Override
			public Option<Selection> visitNothing(EmptySelection selection) {
				return chooseFirst(
						TreePVector.<Option<Selection>>empty()
						.plus(tryFoodInAnyMeal(state))
						.plus(tryAnyMeal(state))
						);
			} 			
 		});
	}
}