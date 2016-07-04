/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey.flat.rules;

import static org.workcraft.gwt.shared.client.CollectionUtils.exists;
import static org.workcraft.gwt.shared.client.CollectionUtils.indexOf;

import java.util.logging.Logger;

import net.scran24.user.client.survey.flat.Prompt;
import net.scran24.user.client.survey.flat.PromptRule;
import net.scran24.user.client.survey.flat.SelectionMode;
import net.scran24.user.client.survey.prompts.AssociatedFoodPrompt;
import net.scran24.user.client.survey.prompts.MealOperation;
import net.scran24.user.shared.CompoundFood;
import net.scran24.user.shared.EncodedFood;
import net.scran24.user.shared.FoodEntry;
import net.scran24.user.shared.FoodPrompt;
import net.scran24.user.shared.Meal;
import net.scran24.user.shared.MissingFood;
import net.scran24.user.shared.RawFood;
import net.scran24.user.shared.TemplateFood;
import net.scran24.user.shared.WithPriority;

import org.pcollections.client.PSet;
import org.pcollections.client.PVector;
import org.workcraft.gwt.shared.client.Function1;
import org.workcraft.gwt.shared.client.Option;
import org.workcraft.gwt.shared.client.Pair;

public class ShowAssociatedFoodPrompt implements PromptRule<Pair<FoodEntry, Meal>, MealOperation> {
	
	private final Logger log = Logger.getLogger("ShowAssociatedFoodPrompt");
	
	private final String locale;
	
	public ShowAssociatedFoodPrompt(final String locale) {
		this.locale = locale;
	}
	
	public static int applicablePromptIndex(final PVector<FoodEntry> foods, final EncodedFood food) {
		return indexOf(food.enabledPrompts, new Function1<FoodPrompt, Boolean>() {
			@Override
			public Boolean apply(final FoodPrompt prompt) {
				return !exists(Meal.linkedFoods(foods, food), new Function1<FoodEntry, Boolean>() {
					@Override
					public Boolean apply(FoodEntry argument) {
						return argument.accept(new FoodEntry.Visitor<Boolean>(){
							@Override
							public Boolean visitRaw(RawFood food) {
								return false;
							}

							@Override
							public Boolean visitEncoded(EncodedFood food) {
								return food.isInCategory(prompt.code) || food.data.code.equals(prompt.code);
							}

							@Override
							public Boolean visitTemplate(TemplateFood food) {
								return false;
							}

							@Override
							public Boolean visitMissing(MissingFood food) {
								return prompt.code.equals(food.customData.get(MissingFood.KEY_ASSOC_FOOD_CATEGORY));
							}

							@Override
							public Boolean visitCompound(CompoundFood food) {
								return false;
							}							
						});
					}
				});
			}
		});
	}

	@Override
	public Option<Prompt<Pair<FoodEntry, Meal>, MealOperation>> apply(final Pair<FoodEntry, Meal> pair, SelectionMode selectionType, PSet<String> surveyFlag) {
		
		if (!pair.left.isEncoded() || !pair.left.isPortionSizeComplete() || pair.left.link.isLinked() || !pair.right.encodingComplete())
			return Option.none();
		else {
			EncodedFood food = (EncodedFood) pair.left;
			Meal meal = pair.right;

			int index = applicablePromptIndex(meal.foods, food);

			if (index == -1)
				return Option.none();
			else
				return Option.<Prompt<Pair<FoodEntry, Meal>, MealOperation>> some(new AssociatedFoodPrompt(locale, pair, meal.foodIndex(food), index));
		}
	}

	public static WithPriority<PromptRule<Pair<FoodEntry, Meal>, MealOperation>> withPriority(int priority, String locale) {
		return new WithPriority<PromptRule<Pair<FoodEntry, Meal>, MealOperation>>(new ShowAssociatedFoodPrompt(locale), priority);
	}
}