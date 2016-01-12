/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.datastore;


public abstract class MapSurveyRecords {
	/*
	 * database evolution script, no longer relevant
	 * retained for reference
	 * 
	 * private static final String dbHost = "workcraft.org";
	private static final int dbPort = 27017;

	private static final String dbName = "scran24";
	private static final String dataPath = "D:\\SCRAN24\\Data";

	private static final FoodLookupServiceConfiguration config = new FoodLookupServiceConfiguration(dataPath + File.separator + "foods.xml", dataPath
			+ File.separator + "food-groups.xml", dataPath + File.separator + "brands.xml", dataPath + File.separator + "categories.xml", dataPath
			+ File.separator + "as-served.xml", dataPath + File.separator + "drinkware.xml", dataPath + File.separator + "index_exclude", dataPath
			+ File.separator + "index_filter", dataPath + File.separator + "synsets", dataPath + File.separator + "split_list", dataPath + File.separator
			+ "guide.xml", dataPath + File.separator + "prompts.xml", dataPath + File.separator + "nutrients.csv");

	private static final FoodLookupService lookupService = new FoodLookupService(config, new PopularityCounter() {
		@Override
		public Map<String, Integer> getCount(Set<String> codes) {
			HashMap<String, Integer> result = new HashMap<String, Integer>();
			for (String s : codes)
				result.put(s, 0);
			return result;
		}
	});

	private static List<NutritionMappedMeal> mapMeals(List<NutritionMappedMeal> meals) {
		ArrayList<NutritionMappedMeal> result = new ArrayList<>();
		for (NutritionMappedMeal m : meals)
			result.add(new NutritionMappedMeal(m.name, mapFoods(m.foods), m.time, m.nutrients));
		return result;
	}

	private static List<NutritionMappedFood> mapFoods(List<NutritionMappedFood> foods) {
		ArrayList<NutritionMappedFood> result = new ArrayList<>();
		for (NutritionMappedFood f : foods) {

			final FoodGroup group = lookupService.foodGroups().forFood(f.code);
			final double reasonableAmount = lookupService.reasonableAmount(f.code);
			final boolean reasonableAmountFlag = (f.portionSize.servingWeight() - f.portionSize.leftoversWeight()) <= reasonableAmount;

			result.add(new NutritionMappedFood(f.code, f.description, f.ndnsCode, f.isReadyMeal, f.searchTerm, f.portionSize, group.id(), group
					.description(), reasonableAmountFlag, f.brand, f.nutrients));
		}
		return result;
	}

	private static NutritionMappedSurveyRecord map(NutritionMappedSurveyRecord in) {
		return new NutritionMappedSurveyRecord(new NutritionMappedSurvey(in.survey.startTime, in.survey.endTime, mapMeals(in.survey.meals),
				in.survey.nutrients, in.survey.log, in.survey.userName), in.userCustomFields, in.id);
	}

	public static void process (String survey_id) {
		System.out.println ("Processing " + survey_id);
		try {
			final MongoDbDataStore ds = new MongoDbDataStore(dbHost, dbPort, dbName, "", "");
			final String source_id = survey_id;
			final String target_id = survey_id + "_mapped";
			
			System.out.println ("Mapping sureveys");
			
			ds.processSurveys(source_id, Long.MIN_VALUE, Long.MAX_VALUE, new Callback1<NutritionMappedSurveyRecord>() {
				@Override
				public void call(NutritionMappedSurveyRecord in) {
					try {
						System.out.println ("Processing " + in.id);
						ds.saveSurvey(target_id, map(in));
					} catch (DataStoreException e) {
						e.printStackTrace();
					}
				}
			});
			
			
			ds.db.getCollection("surveys_" + source_id).rename("_old_surveys_" + source_id);
			ds.db.getCollection("surveys_" + source_id + "_mapped").rename("surveys_" + source_id);
		} catch (DataStoreException e) {
			e.printStackTrace();
		}
		System.out.println("Done");
	}
	
	public static void main(String[] args) {
		for (String s : new String[] { "livewell", "culturelab1", "sensewear", "usertest4", "usertest5" } ) {
			process(s);			
		}
	}*/
}