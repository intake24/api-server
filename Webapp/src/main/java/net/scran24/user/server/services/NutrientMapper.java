/*
This file is part of Intake24.

Copyright 2015, 2016 Newcastle University.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

This file is based on Intake24 v1.0.

© Crown copyright, 2012, 2013, 2014

Licensed under the Open Government Licence 3.0: 

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.server.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.scran24.datastore.NutritionMappedFood;
import net.scran24.datastore.NutritionMappedMeal;
import net.scran24.datastore.NutritionMappedSurvey;
import net.scran24.fooddef.FoodData;
import net.scran24.fooddef.FoodGroup;
import uk.ac.ncl.openlab.intake24.services.FoodDataService;
import uk.ac.ncl.openlab.intake24.services.nutrition.NutrientMappingService;
import net.scran24.user.shared.CompletedFood;
import net.scran24.user.shared.CompletedMeal;
import net.scran24.user.shared.CompletedSurvey;

import org.workcraft.gwt.shared.client.Option;
import org.workcraft.gwt.shared.client.Pair;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class NutrientMapper {
	private final List<Pair<String, ? extends NutrientMappingService>> nutrientTables;
	private final FoodDataService foodDataService;

	@Inject
	public NutrientMapper(List<Pair<String, ? extends NutrientMappingService>> nutrientTables, FoodDataService foodDataService) {
		this.nutrientTables = nutrientTables;
		this.foodDataService = foodDataService;
	}

	private NutritionMappedFood mapFood(CompletedFood food, String locale) {
		final double weight = food.portionSize.servingWeight() - food.portionSize.leftoversWeight();
		
		FoodData foodData = foodDataService.foodData(food.code, locale);

		Map<String, Double> nutrients = null;
		String nutrientTableID = null;
		String nutrientTableCode = null;
		
		for (Pair<String, ? extends NutrientMappingService> table : nutrientTables) {
			if (foodData.nutrientTableCodes().contains(table.left)) {
				nutrientTableID = table.left;
				nutrientTableCode = foodData.nutrientTableCodes().apply(nutrientTableID);
				nutrients = table.right.javaNutrientsFor(nutrientTableCode, weight);
				break;
			}
		}
		
		if (nutrients == null || nutrientTableID == null || nutrientTableCode == null)
			throw new RuntimeException("No supported nutrient table ID found");
		
		final FoodGroup foodGroup = foodDataService.foodGroup(foodData.groupCode(), locale).get();

		final boolean reasonableAmount = (weight <= foodData.reasonableAmount());

		Option<String> localDescription;

		if (foodGroup.localDescription().isDefined())
			localDescription = Option.some(foodGroup.localDescription().get());
		else
			localDescription = Option.none();

		return new NutritionMappedFood(food.code, foodData.englishDescription(), ScalaConversions.toJavaOption(foodData.localDescription()), nutrientTableID, nutrientTableCode, food.isReadyMeal,
				food.searchTerm, food.portionSize, foodGroup.id(), foodGroup.englishDescription(), localDescription, reasonableAmount,
				food.brand, nutrients, food.customData);
	}

	private NutritionMappedMeal mapMeal(CompletedMeal meal, String locale) {
		ArrayList<NutritionMappedFood> mappedFoods = new ArrayList<NutritionMappedFood>();

		for (CompletedFood f : meal.foods) {
			NutritionMappedFood mappedf = mapFood(f, locale);
			mappedFoods.add(mappedf);
		}

		return new NutritionMappedMeal(meal.name, mappedFoods, meal.time, meal.customData);
	}

	public NutritionMappedSurvey map(CompletedSurvey survey, String locale) {
		ArrayList<NutritionMappedMeal> mappedMeals = new ArrayList<NutritionMappedMeal>();

		for (CompletedMeal m : survey.meals) {
			NutritionMappedMeal mappedm = mapMeal(m, locale);
			mappedMeals.add(mappedm);
		}

		return new NutritionMappedSurvey(survey.startTime, survey.endTime, mappedMeals, survey.log, survey.username, survey.customData);
	}
}