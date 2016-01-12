/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey.flat.rules;

import static org.workcraft.gwt.shared.client.CollectionUtils.exists;
import net.scran24.user.client.survey.flat.Prompt;
import net.scran24.user.client.survey.flat.PromptRule;
import net.scran24.user.client.survey.flat.SelectionType;
import net.scran24.user.client.survey.prompts.MealOperation;
import net.scran24.user.client.survey.prompts.ReadyMealsPrompt;
import net.scran24.user.shared.CompoundFood;
import net.scran24.user.shared.TemplateFood;
import net.scran24.user.shared.EncodedFood;
import net.scran24.user.shared.FoodEntry;
import net.scran24.user.shared.FoodEntry.Visitor;
import net.scran24.user.shared.Meal;
import net.scran24.user.shared.MissingFood;
import net.scran24.user.shared.RawFood;
import net.scran24.user.shared.WithPriority;

import org.pcollections.client.PSet;
import org.workcraft.gwt.shared.client.Function1;
import org.workcraft.gwt.shared.client.Option;

public class ShowReadyMealsPrompt implements PromptRule<Meal, MealOperation> {
	
	@Override
	public Option<Prompt<Meal, MealOperation>> apply(final Meal meal, SelectionType selectionType, PSet<String> surveyFlags) {
		boolean hasReadyMeal = exists(meal.foods, new Function1<FoodEntry, Boolean>() {
			@Override
			public Boolean apply(FoodEntry argument) {
				return argument.accept(new Visitor<Boolean>() {
					@Override
					public Boolean visitRaw(RawFood food) {
						return false;
					}

					@Override
					public Boolean visitEncoded(EncodedFood food) {
						return !food.isDrink() && !food.link.isLinked() && food.data.askIfReadyMeal;
					}

					@Override
					public Boolean visitTemplate(TemplateFood food) {
						return false;
					}

					@Override
					public Boolean visitMissing(MissingFood food) {
						return false;
					}

					@Override
					public Boolean visitCompound(CompoundFood food) {
						return false;
					}
				});
			}
		});
		
		if (meal.isEmpty() || !meal.encodingComplete() || !meal.portionSizeComplete() || !hasReadyMeal || meal.readyMealsComplete() )
			return Option.none();
		else {
			return Option.<Prompt<Meal, MealOperation>>some(new ReadyMealsPrompt(meal)); 
		}
	}

	public static WithPriority<PromptRule<Meal, MealOperation>> withPriority(int priority) {
		return new WithPriority<PromptRule<Meal, MealOperation>>(new ShowReadyMealsPrompt(), priority);
	}
}