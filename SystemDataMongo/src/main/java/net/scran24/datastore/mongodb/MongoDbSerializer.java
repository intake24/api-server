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
*/

package net.scran24.datastore.mongodb;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.scran24.datastore.MissingFoodRecord;
import net.scran24.datastore.NutritionMappedFood;
import net.scran24.datastore.NutritionMappedMeal;
import net.scran24.datastore.NutritionMappedSurveyRecord;
import net.scran24.datastore.shared.CompletedPortionSize;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

public class MongoDbSerializer {

	private DBObject mapAsDBObject(Map<String, String> data) {
		BasicDBObject res = new BasicDBObject();

		for (String k : data.keySet())
			res.append(k, data.get(k));

		return res;
	}

	private DBObject portionSizeAsDBObject(CompletedPortionSize size) {

		BasicDBObject res = new BasicDBObject("scriptName", size.scriptName);

		return res.append("data", mapAsDBObject(size.data));
	}

	public DBObject nutrientsAsDBObject(Map<Long, Double> nutrients) {
		BasicDBObject result = new BasicDBObject();

		for (Long k : nutrients.keySet())
			result = result.append(LegacyNutrientTypes.idToLegacyKey.get(k), nutrients.get(k));

		return result;
	}

	private DBObject foodAsDBObject(NutritionMappedFood food) {
		return new BasicDBObject("code", food.code)
		.append("englishDescription", food.englishDescription)
		.append("localDescription", food.localDescription.getOrElse(null))
		.append("readyMeal", food.isReadyMeal ? "true" : "false")
		.append("searchTerm", food.searchTerm)
		.append("portionSize", portionSizeAsDBObject(food.portionSize))
		.append("reasonableAmount", food.reasonableAmount ? "true" : "false")
		.append("foodGroupCode", food.foodGroupCode)
		.append("foodGroupEnglishDescription", food.foodGroupEnglishDescription)
		.append("foodGroupLocalDescription", food.foodGroupLocalDescription.getOrElse(null))
		.append("brand", food.brand)
		.append("nutrientTableID", food.nutrientTableID)
		.append("nutrientTableCode", food.nutrientTableCode)
		.append("nutrients", nutrientsAsDBObject(food.nutrients))
		.append("customData", mapAsDBObject(food.customData));
	}

	private List<DBObject> foodsAsDBObject(List<NutritionMappedFood> foods) {
		ArrayList<DBObject> result = new ArrayList<DBObject>();

		for (NutritionMappedFood f : foods)
			result.add(foodAsDBObject(f));

		return result;
	}

	private DBObject mealAsDBObject(NutritionMappedMeal meal) {
		return new BasicDBObject("hours", meal.time.hours)
		.append("minutes", meal.time.minutes)
		.append("name", meal.name)
		.append("foods", foodsAsDBObject(meal.foods))
		.append("customData", mapAsDBObject(meal.customData));
	}

	private List<DBObject> mealsAsDBObject(List<NutritionMappedMeal> meals) {
		ArrayList<DBObject> result = new ArrayList<DBObject>();

		for (NutritionMappedMeal m : meals)
			result.add(mealAsDBObject(m));

		return result;
	}
	
	private DBObject missingFoodAsDBObject(MissingFoodRecord missingFood) {
		return new BasicDBObject("name", missingFood.name)
		.append("surveyId", missingFood.surveyId)
		.append("userName", missingFood.userName)		
		.append("submitted_at", missingFood.submittedAt)
		.append("brand", missingFood.brand)
		.append("description", missingFood.description)
		.append("portionSize", missingFood.portionSize)
		.append("leftovers", missingFood.leftovers);
	}
	
	public List<DBObject> missingFoodsAsDBObject(List<MissingFoodRecord> missingFoods) {
		ArrayList<DBObject> result = new ArrayList<DBObject>();
		
		for (MissingFoodRecord food: missingFoods) 
			result.add(missingFoodAsDBObject(food));
		
		return result;		
	}

	public DBObject serialize(NutritionMappedSurveyRecord survey, String username) {

		BasicDBObject customFields = new BasicDBObject();

		for (String k : survey.userCustomFields.keySet()) {
			customFields.put(k, survey.userCustomFields.get(k));
		}

		return new BasicDBObject("startTime", survey.survey.startTime)
		.append("endTime", survey.survey.endTime)
		.append("log", survey.survey.log)
		.append("userName", username)
		.append("userData", customFields)
		.append("meals", mealsAsDBObject(survey.survey.meals))
		.append("customData", mapAsDBObject(survey.survey.customData));
	}
}