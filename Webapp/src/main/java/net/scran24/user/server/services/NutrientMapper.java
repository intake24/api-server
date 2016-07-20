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
import java.util.HashMap;
import java.util.Map;

import net.scran24.datastore.NutritionMappedFood;
import net.scran24.datastore.NutritionMappedMeal;
import net.scran24.datastore.NutritionMappedSurvey;
import net.scran24.user.shared.CompletedFood;
import net.scran24.user.shared.CompletedMeal;
import net.scran24.user.shared.CompletedSurvey;

import org.workcraft.gwt.shared.client.Option;

import scala.Tuple2;
import scala.util.Either;
import uk.ac.ncl.openlab.intake24.FoodGroup;
import uk.ac.ncl.openlab.intake24.FoodRecord;
import uk.ac.ncl.openlab.intake24.UserFoodData;
import uk.ac.ncl.openlab.intake24.nutrients.Nutrient;
import uk.ac.ncl.openlab.intake24.services.AdminFoodDataService;
import uk.ac.ncl.openlab.intake24.services.CodeError;
import uk.ac.ncl.openlab.intake24.services.FoodDataError;
import uk.ac.ncl.openlab.intake24.services.FoodDataSources;
import uk.ac.ncl.openlab.intake24.services.NutrientMappingError;
import uk.ac.ncl.openlab.intake24.services.UserFoodDataService;
import uk.ac.ncl.openlab.intake24.services.nutrition.NutrientMappingService;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class NutrientMapper {

	private final AdminFoodDataService adminDataService;
	private final UserFoodDataService userDataService;
	private final NutrientMappingService nutrientMappingService;

	@Inject
	public NutrientMapper(NutrientMappingService nutrientMappingService, UserFoodDataService userDataService, AdminFoodDataService adminDataService) {
		this.nutrientMappingService = nutrientMappingService;
		this.userDataService = userDataService;
		this.adminDataService = adminDataService;

	}

	private NutritionMappedFood mapFood(CompletedFood food, String locale) {
		final double weight = food.portionSize.servingWeight() - food.portionSize.leftoversWeight();

		Either<FoodDataError, Tuple2<UserFoodData, FoodDataSources>> foodDataResult = userDataService.foodData(food.code, locale);

		Either<CodeError, FoodRecord> foodDefResult = adminDataService.foodRecord(food.code, locale);

		if (foodDefResult.isLeft())
			throw new RuntimeException("Cannot retreive food record for " + food.code);

		FoodRecord foodDef = foodDefResult.right().get();

		if (foodDataResult.isLeft())
			throw new RuntimeException("Cannot retreive data for food code " + food.code);
		else {

			UserFoodData foodData = foodDataResult.right().get()._1();

			// FIXME: Undefined behaviour: only the first nutrient table code
			// (in random order) will be used

			scala.Option<Tuple2<String, String>> tableCode = foodData.nutrientTableCodes().headOption();

			if (tableCode.isEmpty())
				throw new RuntimeException(String.format("Food %s (%s) has no nutrient table codes", foodData.localDescription(), foodData.code()));
			else {
				String nutrientTableId = tableCode.get()._1;
				String nutrientTableRecordId = tableCode.get()._2;

				Either<NutrientMappingError, Map<Nutrient, Double>> nutrientsResult = nutrientMappingService.javaNutrientsFor(nutrientTableId,
						nutrientTableRecordId, weight);

				if (nutrientsResult.isLeft())
					throw new RuntimeException(String.format("Failed to look up nutrients for %s in nutrient table %s", nutrientTableRecordId,
							nutrientTableId));
				else {
					
					Map<Nutrient, Double> nutrients = nutrientsResult.right().get();
					
					final FoodGroup foodGroup = adminDataService.foodGroup(foodData.groupCode(), locale).get();

					final boolean reasonableAmount = (weight <= foodData.reasonableAmount());

					Option<String> localDescription;

					if (foodGroup.localDescription().isDefined())
						localDescription = Option.some(foodGroup.localDescription().get());
					else
						localDescription = Option.none();
					
					final Map<String, Double> legacyNutrientMap = new HashMap<String, Double>();
					
					for (Nutrient nutrientType : nutrients.keySet()) 
						legacyNutrientMap.put(nutrientType.key(), nutrients.get(nutrientType));

					return new NutritionMappedFood(food.code, foodDef.main().englishDescription(), ScalaConversions.toJavaOption(foodDef.local()
							.localDescription()), nutrientTableId, nutrientTableRecordId, food.isReadyMeal, food.searchTerm, food.portionSize,
							foodGroup.id(), foodGroup.englishDescription(), localDescription, reasonableAmount, food.brand, legacyNutrientMap,
							food.customData);
				}

			}
		}
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