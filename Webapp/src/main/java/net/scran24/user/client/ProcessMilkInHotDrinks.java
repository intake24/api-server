/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client;

import static org.workcraft.gwt.shared.client.CollectionUtils.foldl;
import static org.workcraft.gwt.shared.client.CollectionUtils.map;

import java.util.HashMap;

import net.scran24.datastore.shared.CompletedPortionSize;
import net.scran24.user.client.survey.flat.Survey;
import net.scran24.user.client.survey.portionsize.experimental.MilkInHotDrinkPortionSizeScript;
import net.scran24.user.client.survey.portionsize.experimental.PortionSize;
import net.scran24.user.shared.CompoundFood;
import net.scran24.user.shared.TemplateFood;
import net.scran24.user.shared.EncodedFood;
import net.scran24.user.shared.FoodEntry;
import net.scran24.user.shared.FoodEntry.Visitor;
import net.scran24.user.shared.Meal;
import net.scran24.user.shared.MissingFood;
import net.scran24.user.shared.RawFood;
import net.scran24.user.shared.SpecialData;
import net.scran24.user.shared.UUID;

import org.pcollections.client.PVector;
import org.pcollections.client.TreePVector;
import org.workcraft.gwt.shared.client.Function1;
import org.workcraft.gwt.shared.client.Function2;
import org.workcraft.gwt.shared.client.Pair;

public class ProcessMilkInHotDrinks implements Function1<Survey, Survey> {

	public PVector<Pair<Integer, Integer>> findPairs(final Meal meal) {
		return foldl(meal.foods, TreePVector.<Pair<Integer, Integer>> empty(),
				new Function2<PVector<Pair<Integer, Integer>>, FoodEntry, PVector<Pair<Integer, Integer>>>() {
					@Override
					public PVector<Pair<Integer, Integer>> apply(final PVector<Pair<Integer, Integer>> pairs, final FoodEntry next) {
						return next.accept(new Visitor<PVector<Pair<Integer, Integer>>>() {
							@Override
							public PVector<Pair<Integer, Integer>> visitRaw(RawFood food) {
								return pairs;
							}

							@Override
							public PVector<Pair<Integer, Integer>> visitEncoded(EncodedFood food) {
								if (food.isInCategory(SpecialData.FOOD_CODE_MILK_IN_HOT_DRINK)) {
									UUID drink_id = food.link.linkedTo.getOrDie("Milk from this category must be linked to a hot drink!");
									return pairs.plus(Pair.create(meal.foodIndex(next), meal.foodIndex(drink_id)));
								} else
									return pairs;
							}

							@Override
							public PVector<Pair<Integer, Integer>> visitTemplate(TemplateFood food) {
								return pairs;
							}

							@Override
							public PVector<Pair<Integer, Integer>> visitMissing(MissingFood food) {
								return pairs;
							}

							@Override
							public PVector<Pair<Integer, Integer>> visitCompound(
									CompoundFood food) {
								return pairs;
							}
						});
					}
				});
	}

	public Meal processPair(Meal meal, Pair<Integer, Integer> pair) {
		EncodedFood milk = meal.foods.get(pair.left).asEncoded();
		EncodedFood drink = meal.foods.get(pair.right).asEncoded();

		CompletedPortionSize milk_ps = milk.completedPortionSize();
		CompletedPortionSize drink_ps = drink.completedPortionSize();

		int milkPartIndex = Integer.parseInt(milk_ps.data.get("milkPartIndex"));
		double milkPart = MilkInHotDrinkPortionSizeScript.amounts.get(milkPartIndex).weight;
		
		double drinkVolume = Double.parseDouble(drink_ps.data.get("servingWeight"));
		double drinkLeftoverVolume = Double.parseDouble(drink_ps.data.get("leftoversWeight"));

		double finalDrinkVolume = drinkVolume * (1 - milkPart);
		double finalDrinkLeftoverVolume = drinkLeftoverVolume * (1 - milkPart);

		double finalMilkVolume = drinkVolume * milkPart;
		double finalMilkLeftoverVolume = drinkLeftoverVolume * milkPart;

		HashMap<String, String> finalMilkData = new HashMap<String, String>(milk_ps.data);
		finalMilkData.put("servingWeight", Double.toString(finalMilkVolume));
		finalMilkData.put("leftoversWeight", Double.toString(finalMilkLeftoverVolume));

		HashMap<String, String> finalDrinkData = new HashMap<String, String>(drink_ps.data);
		finalDrinkData.put("servingWeight", Double.toString(finalDrinkVolume));
		finalDrinkData.put("leftoversWeight", Double.toString(finalDrinkLeftoverVolume));

		CompletedPortionSize finalMilkPs = new CompletedPortionSize(milk_ps.scriptName, finalMilkData);
		CompletedPortionSize finalDrinkPs = new CompletedPortionSize(milk_ps.scriptName, finalDrinkData);

		return 
				meal.updateFood(pair.left, milk.withPortionSize(PortionSize.complete(finalMilkPs)))
				    .updateFood(pair.right, drink.withPortionSize(PortionSize.complete(finalDrinkPs)));
	}

	public Meal processMeal(Meal meal) {
		return foldl(findPairs(meal), meal, new Function2<Meal, Pair<Integer, Integer>, Meal>() {
			@Override
			public Meal apply(Meal arg1, Pair<Integer, Integer> arg2) {
				return processPair(arg1, arg2);
			}
		});
	}

	@Override
	public Survey apply(Survey survey) {
		return survey.withMeals(map(survey.meals, new Function1<Meal, Meal>() {
			@Override
			public Meal apply(Meal argument) {
				return processMeal(argument);
			}
		}));
	}
}
