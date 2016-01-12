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
import java.util.HashMap;
import java.util.Map;

import net.scran24.datastore.MissingFoodRecord;
import net.scran24.datastore.NutritionMappedFood;
import net.scran24.datastore.NutritionMappedMeal;
import net.scran24.datastore.NutritionMappedSurvey;
import net.scran24.datastore.NutritionMappedSurveyRecord;
import net.scran24.datastore.NutritionMappedSurveyRecordWithId;
import net.scran24.datastore.shared.CompletedPortionSize;
import net.scran24.datastore.shared.Time;

import org.bson.types.ObjectId;
import org.workcraft.gwt.shared.client.Option;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

public class MongoDbDeserializer {

	private Map<String, Double> parseNutrients(DBObject obj) {
		HashMap<String, Double> result = new HashMap<String, Double>();
		for (String k : obj.keySet())
			result.put(k, (Double) obj.get(k));
		return result;
	}

	private Map<String, String> parseData(DBObject dbObject) {
		HashMap<String, String> result = new HashMap<String, String>();

		// FIXME: this is a hack to support legacy database entries, should not
		// be here
		if (dbObject == null)
			return result;

		for (String k : dbObject.keySet()) {
			result.put(k, dbObject.get(k).toString());
		}

		return result;
	}

	private CompletedPortionSize parsePortionSize(DBObject dbObject) {
		String name = (String) dbObject.get("scriptName");
		Map<String, String> data = parseData((DBObject) dbObject.get("data"));
		return new CompletedPortionSize(name, data);
	}

	/*
	 * private DBObject foodAsDBObject (CompletedFood food) { return new
	 * BasicDBObject ("code", food.code) .append ("searchTerm", food.searchTerm)
	 * .append ("portionSize", portionSizeAsDBObject(food.portionSize)) .append
	 * ("ndnsCode", food.ndnsCode) .append ("nutrients",
	 * nutrientsAsDBObject(nutrientMappingService.getNutrients(food.code,
	 * food.portionSize.servingWeight() - food.portionSize.leftoversWeight())));
	 * }
	 */
	public NutritionMappedFood parseFood(DBObject obj) {
		String code = (String) obj.get("code");

		boolean readyMeal = false;

		if (obj.containsField("readyMeal") && obj.get("readyMeal").equals("true"))
			readyMeal = true;

		String englishDescription = (String) obj.get("englishDescription");
		
		if (englishDescription == null)
			englishDescription = (String) obj.get("description"); // fall back to unlocalised version
		
		String localDescription = (String) obj.get("localDescription");

		String brand = (String) obj.get("brand");
		if (brand == null)
			brand = "";
		String searchTerm = (String) obj.get("searchTerm");

		// Support multiple nutrient tables, but fall back to old NDNS only
		// implementation for old records
		String nutrientTableID = (String) obj.get("nutrientTableID");

		if (nutrientTableID == null)
			nutrientTableID = "NDNS";

		String nutrientTableCode = (String) obj.get("nutrientTableCode");

		if (nutrientTableCode == null)
			nutrientTableCode = Integer.toString((Integer) obj.get("ndnsCode"));

		CompletedPortionSize portionSize = parsePortionSize((DBObject) obj.get("portionSize"));

		DBObject nutrients = (DBObject) obj.get("nutrients");

		Integer foodGroupCode = (Integer) obj.get("foodGroupCode");
		if (foodGroupCode == null)
			foodGroupCode = 0;

		String englishFoodGroupDescription = (String) obj.get("foodGroupEnglishDescription");
		if (englishFoodGroupDescription == null)
			englishFoodGroupDescription = (String) obj.get("foodGroupDescription");
		if (englishFoodGroupDescription == null)
			englishFoodGroupDescription = "N/A";

		String localFoodGroupDescription = (String) obj.get("foodGroupLocalDescription");

		boolean reasonableAmountFlag;

		String reasonableAmount = (String) obj.get("reasonableAmount");
		if (reasonableAmount == null)
			reasonableAmountFlag = true;
		else
			reasonableAmountFlag = reasonableAmount.equals("true");

		return new NutritionMappedFood(code, englishDescription, Option.<String> fromNullable(localDescription), nutrientTableID, nutrientTableCode,
				readyMeal, searchTerm, portionSize, foodGroupCode, englishFoodGroupDescription, Option.fromNullable(localFoodGroupDescription),
				reasonableAmountFlag, brand, parseNutrients(nutrients), parseData((DBObject) obj.get("customData")));
	}

	/*
	 * return new BasicDBObject ("hours", meal.time.hours) .append ("minutes",
	 * meal.time.minutes) .append ("name", meal.name) .append ("foods",
	 * foodsAsDBObject(meal.foods));
	 */
	public NutritionMappedMeal parseMeal(DBObject obj) {
		Integer hours = (Integer) obj.get("hours");
		Integer minutes = (Integer) obj.get("minutes");
		String name = (String) obj.get("name");

		ArrayList<NutritionMappedFood> foods = new ArrayList<NutritionMappedFood>();

		for (Object f : (BasicDBList) obj.get("foods")) {
			foods.add(parseFood((BasicDBObject) f));
		}

		return new NutritionMappedMeal(name, foods, new Time(hours, minutes), parseData((DBObject) obj.get("customData")));
	}

	/*
	 * return new BasicDBObject ("startTime", survey.startTime) .append
	 * ("endTime", survey.endTime) .append ("log", log) .append ("meals",
	 * mealsAsDBObject (survey.meals));
	 */
	public NutritionMappedSurveyRecordWithId deserialize(DBObject obj) {
		ArrayList<NutritionMappedMeal> meals = new ArrayList<NutritionMappedMeal>();

		Long startTime = (Long) obj.get("startTime");
		Long endTime = (Long) obj.get("endTime");
		String userName = (String) obj.get("userName");

		BasicDBList mealsList = (BasicDBList) obj.get("meals");

		for (Object m : mealsList) {
			meals.add(parseMeal((DBObject) m));
		}

		ArrayList<String> log = new ArrayList<String>();

		BasicDBList loglist = (BasicDBList) obj.get("log");

		for (Object o : loglist)
			log.add((String) o);

		Map<String, String> customFields = new HashMap<String, String>();

		if (obj.containsField("userData")) {
			BasicDBObject userCustomFields = (BasicDBObject) obj.get("userData");
			for (String k : userCustomFields.keySet())
				customFields.put(k, userCustomFields.getString(k));
		}

		return new NutritionMappedSurveyRecordWithId(new NutritionMappedSurvey(startTime, endTime, meals, log, userName,
				parseData((DBObject) obj.get("customData"))), customFields, ((ObjectId) obj.get("_id")).toString());
	}

	public MissingFoodRecord deserializeMissingFood(DBObject o) {
		return new MissingFoodRecord((Long) o.get("submitted_at"), (String) o.get("surveyId"), (String) o.get("userName"), (String) o.get("name"),
				(String) o.get("brand"), (String) o.get("description"), (String) o.get("portionSize"), (String) o.get("leftovers"));
	}
}
