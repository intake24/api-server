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

import static net.scran24.user.server.services.ScalaConversions.toJavaCategoryHeader;
import static net.scran24.user.server.services.ScalaConversions.toJavaCategoryHeaders;
import static net.scran24.user.server.services.ScalaConversions.toJavaFoodHeader;
import static net.scran24.user.server.services.ScalaConversions.toJavaFoodHeaders;
import static net.scran24.user.server.services.ScalaConversions.toJavaList;
import static net.scran24.user.server.services.ScalaConversions.toJavaPortionSizeMethods;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.ServletException;

import net.scran24.datastore.DataStore;
import net.scran24.datastore.DataStoreException;
import uk.ac.ncl.openlab.intake24.AsServedSet;
import uk.ac.ncl.openlab.intake24.DrinkwareSet;
import uk.ac.ncl.openlab.intake24.GuideImage;
import uk.ac.ncl.openlab.intake24.AssociatedFood;
import uk.ac.ncl.openlab.intake24.UserCategoryContents;
import net.scran24.user.client.services.FoodLookupService;
import net.scran24.user.shared.CategoryHeader;
import net.scran24.user.shared.FoodData;
import net.scran24.user.shared.FoodHeader;
import net.scran24.user.shared.FoodPrompt;
import net.scran24.user.shared.lookup.AsServedDef;
import net.scran24.user.shared.lookup.DrinkScaleDef;
import net.scran24.user.shared.lookup.DrinkwareDef;
import net.scran24.user.shared.lookup.GuideDef;
import net.scran24.user.shared.lookup.LookupResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.workcraft.gwt.imagechooser.shared.ImageDef;
import org.workcraft.gwt.shared.client.Pair;

import scala.Tuple2;
import scala.collection.Iterator;
import scala.collection.JavaConversions;
import scala.collection.Seq;
import scala.util.Either;
import uk.ac.ncl.openlab.intake24.nutrients.Nutrient;
import uk.ac.ncl.openlab.intake24.nutrients.EnergyKcal$;
import uk.ac.ncl.openlab.intake24.services.CodeError;
import uk.ac.ncl.openlab.intake24.services.FoodDataError;
import uk.ac.ncl.openlab.intake24.services.FoodDataSources;
import uk.ac.ncl.openlab.intake24.services.NutrientMappingError;
import uk.ac.ncl.openlab.intake24.services.ResourceError;
import uk.ac.ncl.openlab.intake24.services.UserFoodDataService;
import uk.ac.ncl.openlab.intake24.services.foodindex.FoodIndex;
import uk.ac.ncl.openlab.intake24.services.foodindex.IndexLookupResult;
import uk.ac.ncl.openlab.intake24.services.foodindex.MatchedCategory;
import uk.ac.ncl.openlab.intake24.services.foodindex.MatchedFood;
import uk.ac.ncl.openlab.intake24.services.foodindex.Splitter;
import uk.ac.ncl.openlab.intake24.services.nutrition.NutrientMappingService;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

@SuppressWarnings("serial")
public class FoodLookupServiceImpl extends RemoteServiceServlet implements FoodLookupService {
	private final static Logger log = LoggerFactory.getLogger(FoodLookupServiceImpl.class);

	private DataStore dataStore;
	private UserFoodDataService foodData;

	private Map<String, FoodIndex> foodIndexes;
	private Map<String, Splitter> splitters;
	private NutrientMappingService nutrientMappingService;

	private String imageUrlBase;
	private String thumbnailUrlBase;

	private void crashIfDebugOptionSet(String name) {
		String param = getServletContext().getInitParameter(name);
		if (param != null && param.equals("true"))
			throw new RuntimeException("Crashed on request. If this exception is unexpected, check the \"" + name
					+ "\" context parameter in WEB-INF/web.xml config file.");
	}

	@Override
	public void init() throws ServletException {
		try {
			Injector injector = (Injector) this.getServletContext().getAttribute("intake24.injector");

			dataStore = injector.getInstance(DataStore.class);
			foodData = injector.getInstance(UserFoodDataService.class);
			foodIndexes = injector.getInstance(Key.get(new TypeLiteral<Map<String, FoodIndex>>() {
			}));
			splitters = injector.getInstance(Key.get(new TypeLiteral<Map<String, Splitter>>() {
			}));

			nutrientMappingService = injector.getInstance(NutrientMappingService.class);

			imageUrlBase = getServletContext().getInitParameter("image-url-base");

			thumbnailUrlBase = getServletContext().getInitParameter("thumbnail-url-base");

		} catch (Throwable e) {
			throw new ServletException(e);
		}
	}

	private LookupResult lookupImpl(String description, String locale, int maxResults, boolean includeHidden) {
		try {
			crashIfDebugOptionSet("crash-on-food-lookup");

			if (!foodIndexes.containsKey(locale))
				throw new RuntimeException("Missing food index for locale " + locale);

			IndexLookupResult lookupResult = foodIndexes.get(locale).lookup(description, maxResults);

			HashSet<String> foodCodes = new HashSet<String>();

			ArrayList<FoodHeader> foodHeaders = new ArrayList<FoodHeader>();

			ArrayList<CategoryHeader> categoryHeaders = new ArrayList<CategoryHeader>();

			Iterator<MatchedFood> iter = lookupResult.foods().iterator();

			HashMap<String, Integer> matchCost = new HashMap<String, Integer>();

			while (iter.hasNext()) {
				MatchedFood next = iter.next();

				uk.ac.ncl.openlab.intake24.UserFoodHeader header = next.food();

				// boolean isHidden = true;

				foodCodes.add(header.code());
				matchCost.put(header.code(), next.matchCost());
				foodHeaders.add(toJavaFoodHeader(header));

			}

			Iterator<MatchedCategory> iter2 = lookupResult.categories().iterator();

			while (iter2.hasNext()) {
				MatchedCategory next = iter2.next();
				// if (!next.category().isHidden() || includeHidden)
				categoryHeaders.add(toJavaCategoryHeader(next.category()));
			}

			Map<String, Integer> popularityCount = dataStore.getPopularityCount(foodCodes);

			final HashMap<String, Double> finalCost = new HashMap<String, Double>();

			for (FoodHeader header : foodHeaders) {
				// avoid zero to be able to divide popularity by match cost
				double mcost = (double) matchCost.get(header.code) + 1.0;
				// avoid zero to be able to discern between 0-popularity entries
				// based on match cost
				double pop = (double) popularityCount.get(header.code) + 1.0;
				// this function should be adjusted to get a different ordering
				// based on popularity and match cost
				finalCost.put(header.code, pop / mcost);
			}

			Collections.sort(foodHeaders, new Comparator<FoodHeader>() {
				@Override
				public int compare(FoodHeader o1, FoodHeader o2) {
					return finalCost.get(o2.code).compareTo(finalCost.get(o1.code));
				}
			});

			// category match cost intentionally ignored, sorted alphabetically

			Collections.sort(categoryHeaders, new Comparator<CategoryHeader>() {
				@Override
				public int compare(CategoryHeader o1, CategoryHeader o2) {
					return o1.code.compareTo(o2.code);
				}
			});

			return new LookupResult(foodHeaders, categoryHeaders);
		} catch (DataStoreException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public LookupResult lookup(String description, String locale, int maxResults) {
		return lookupImpl(description, locale, maxResults, false);
	}

	@Override
	public LookupResult lookupInCategory(String description, final String categoryCode, String locale, int maxResults) {
		LookupResult lookupResult = lookupImpl(description, locale, maxResults, true);

		ArrayList<FoodHeader> foods = new ArrayList<FoodHeader>();
		ArrayList<CategoryHeader> categories = new ArrayList<CategoryHeader>();

		for (FoodHeader h : lookupResult.foods)
			for (String c : JavaConversions.asJavaCollection(foodData.foodAllCategories(h.code))) {
				if (c.equals(categoryCode)) {
					foods.add(h);
					break;
				}
			}

		for (CategoryHeader h : lookupResult.categories)
			for (String c : JavaConversions.asJavaCollection(foodData.categoryAllCategories(h.code))) {
				if (c.equals(categoryCode)) {
					categories.add(h);
					break;
				}
			}

		return new LookupResult(foods, categories);
	}

	@Override
	public List<CategoryHeader> getRootCategories(String locale) {
		crashIfDebugOptionSet("crash-on-get-root-categories");

		return toJavaCategoryHeaders(foodData.rootCategories(locale));
	}

	@Override
	public LookupResult browseCategory(String code, String locale) {
		crashIfDebugOptionSet("crash-on-browse-category");

		Either<CodeError, UserCategoryContents> categoryContents = foodData.categoryContents(code, locale);

		if (categoryContents.isLeft())
			throw new RuntimeException("Invalid category code");
		else {
			UserCategoryContents contents = categoryContents.right().get();
			return new LookupResult(toJavaFoodHeaders(contents.foods()), toJavaCategoryHeaders(contents.subcategories()));
		}
	}

	private String labelForAsServed(double weight) {
		return Integer.toString((int) weight) + " g";
	}

	@Override
	public List<String> split(String description, String locale) {
		crashIfDebugOptionSet("crash-on-split");

		ArrayList<String> result = new ArrayList<String>();

		Splitter splitter = splitters.get(locale);

		if (splitter != null) {
			Iterator<String> iter = splitter.split(description).iterator();

			while (iter.hasNext())
				result.add(iter.next());
		} else {
			log.warn("Food description splitter not registered for locale " + locale + ", skipping split check.");
			result.add(description);
		}

		return result;
	}

	@Override
	public AsServedDef getAsServedDef(String asServedSet, String locale) {
		crashIfDebugOptionSet("crash-on-get-as-served-def");

		Either<ResourceError, AsServedSet> asServedDef = foodData.asServedDef(asServedSet);

		if (asServedDef.isLeft())
			throw new RuntimeException("As served set not found: " + asServedSet);
		else {
			uk.ac.ncl.openlab.intake24.AsServedSet set = asServedDef.right().get();

			int size = set.images().size();

			AsServedDef.ImageInfo[] info = new AsServedDef.ImageInfo[size];

			Iterator<uk.ac.ncl.openlab.intake24.AsServedImage> iter = set.images().iterator();

			int i = 0;
			while (iter.hasNext()) {
				uk.ac.ncl.openlab.intake24.AsServedImage img = iter.next();

				info[i++] = new AsServedDef.ImageInfo(new ImageDef(imageUrlBase + "/" + img.url(), thumbnailUrlBase + "/" + img.url(),
						labelForAsServed(img.weight())), img.weight());
			}

			return new AsServedDef(set.description(), info);
		}
	}

	@Override
	public List<AsServedDef> getMultipleAsServedDefs(List<String> ids, String locale) {
		ArrayList<AsServedDef> result = new ArrayList<AsServedDef>();

		for (String id : ids)
			result.add(getAsServedDef(id, locale));

		return result;
	}

	@Override
	public GuideDef getGuideDef(String guideId, String locale) {
		crashIfDebugOptionSet("crash-on-get-guide-def");

		Either<ResourceError, GuideImage> guideDef = foodData.guideDef(guideId);

		if (guideDef.isLeft())
			throw new RuntimeException("Guide image not found: " + guideId);
		else {

			uk.ac.ncl.openlab.intake24.GuideImage image = guideDef.right().get();

			Map<Integer, Double> weights = new TreeMap<Integer, Double>();

			Iterator<uk.ac.ncl.openlab.intake24.GuideImageWeightRecord> iter = image.weights().iterator();

			while (iter.hasNext()) {
				uk.ac.ncl.openlab.intake24.GuideImageWeightRecord wr = iter.next();
				weights.put(wr.objectId(), wr.weight());
			}

			return new GuideDef(image.description(), guideId, weights);
		}
	}

	@Override
	public DrinkwareDef getDrinkwareDef(String drinkwareId, String locale) {
		crashIfDebugOptionSet("crash-on-get-drinkware-def");

		Either<ResourceError, DrinkwareSet> drinkwareDef = foodData.drinkwareDef(drinkwareId);

		if (drinkwareDef.isLeft())
			throw new RuntimeException("Drinkware definition not found: " + drinkwareId);
		else {
			uk.ac.ncl.openlab.intake24.DrinkwareSet set = drinkwareDef.right().get();

			ArrayList<DrinkScaleDef> scaleDefs = new ArrayList<DrinkScaleDef>();

			Iterator<uk.ac.ncl.openlab.intake24.DrinkScale> iter = set.scaleDefs().iterator();

			while (iter.hasNext()) {
				uk.ac.ncl.openlab.intake24.DrinkScale def = iter.next();

				scaleDefs.add(new DrinkScaleDef(def.choice_id(), imageUrlBase + "/" + def.baseImage(), imageUrlBase + "/" + def.overlayImage(), def
						.width(), def.height(), def.emptyLevel(), def.fullLevel(), def.vf().asArray()));
			}

			return new DrinkwareDef(set.guide_id(), scaleDefs.toArray(new DrinkScaleDef[scaleDefs.size()]));
		}
	}

	@Override
	public List<FoodPrompt> getFoodPrompts(String foodCode, String locale) {
		crashIfDebugOptionSet("crash-on-get-food-prompts");

		Either<CodeError, Seq<AssociatedFood>> associatedFoodPrompts = foodData.associatedFoods(foodCode, locale);

		if (associatedFoodPrompts.isLeft()) {
			throw new RuntimeException("Food code " + foodCode + " is not defined");
		} else {

			Iterator<uk.ac.ncl.openlab.intake24.AssociatedFood> iter = associatedFoodPrompts.right().get().iterator();

			ArrayList<FoodPrompt> result = new ArrayList<FoodPrompt>();

			while (iter.hasNext()) {
				uk.ac.ncl.openlab.intake24.AssociatedFood next = iter.next();
				
				if (next.foodOrCategoryCode().isRight())
					result.add(new FoodPrompt(next.foodOrCategoryCode().right().get(), true, next.promptText(), next.linkAsMain(), next.genericName()));
				else
					result.add(new FoodPrompt(next.foodOrCategoryCode().left().get(), false, next.promptText(), next.linkAsMain(), next.genericName()));
			}

			return result;
		}
	}

	public List<String> getBrandNames(String foodCode, String locale) {
		Either<CodeError, Seq<String>> brandNames = foodData.brandNames(foodCode, locale);

		if (brandNames.isLeft())
			throw new RuntimeException("Unknown food code: " + foodCode);
		else
			return toJavaList(brandNames.right().get());
	}

	@Override
	public FoodData getFoodData(String foodCode, String locale) {
		crashIfDebugOptionSet("crash-on-get-food-data");

		Either<FoodDataError, Tuple2<uk.ac.ncl.openlab.intake24.UserFoodData, FoodDataSources>> foodDataResult = foodData.foodData(foodCode, locale);

		if (foodDataResult.isLeft())
			throw new RuntimeException("Food code not found or local description missing: " + foodCode);
		else {

			uk.ac.ncl.openlab.intake24.UserFoodData data = foodDataResult.right().get()._1();

			log.debug(data.toString());

			// FIXME: Undefined behaviour: only the first nutrient table code
			// (in random order) will be used

			scala.Option<Tuple2<String, String>> tableCode = data.nutrientTableCodes().headOption();

			if (tableCode.isEmpty())
				throw new RuntimeException(String.format("Food %s (%s) has no nutrient table codes", data.localDescription(), data.code()));
			else {
				String nutrientTableId = tableCode.get()._1;
				String nutrientTableRecordId = tableCode.get()._2;

				Either<NutrientMappingError, Map<Nutrient, Double>> nutrientsResult = nutrientMappingService.javaNutrientsFor(nutrientTableId,
						nutrientTableRecordId, 100.0);

				if (nutrientsResult.isLeft())
					throw new RuntimeException(String.format("Failed to look up nutrients for %s in nutrient table %s", nutrientTableRecordId,
							nutrientTableId));
				else {
					return new FoodData(data.code(), data.readyMealOption(), data.sameAsBeforeOption(), nutrientsResult.right().get().get(EnergyKcal$.MODULE$), data.localDescription(),
							toJavaPortionSizeMethods(data.portionSize(), imageUrlBase), getFoodPrompts(foodCode, locale), getBrandNames(foodCode,
									locale), toJavaList(foodData.foodAllCategories(foodCode)));
				}
			}

		}
	}
}