/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
 */

package net.scran24.user.client.test;

import java.util.Iterator;
import java.util.List;

import net.scran24.datastore.shared.CompletedPortionSize;
import net.scran24.datastore.shared.Time;
import net.scran24.user.client.json.SerialisableSurveyCodec;
import net.scran24.user.client.survey.CompoundFoodTemplateManager;
import net.scran24.user.client.survey.FoodTemplates;
import net.scran24.user.client.survey.flat.Selection;
import net.scran24.user.client.survey.flat.SelectionMode;
import net.scran24.user.client.survey.flat.Survey;
import net.scran24.user.client.survey.flat.SurveyXmlSerialiser;
import net.scran24.user.client.survey.flat.VersionMismatchException;
import net.scran24.user.client.survey.flat.serialisable.SerialisableSurvey;
import net.scran24.user.client.survey.portionsize.experimental.DefaultPortionSizeScripts;
import net.scran24.user.client.survey.portionsize.experimental.PortionSize;
import net.scran24.user.client.survey.portionsize.experimental.PortionSizeScript;
import net.scran24.user.client.survey.portionsize.experimental.PortionSizeScriptManager;
import net.scran24.user.shared.CompoundFood;
import net.scran24.user.shared.EncodedFood;
import net.scran24.user.shared.FoodData;
import net.scran24.user.shared.FoodEntry;
import net.scran24.user.shared.FoodLink;
import net.scran24.user.shared.FoodPrompt;
import net.scran24.user.shared.Meal;
import net.scran24.user.shared.MissingFood;
import net.scran24.user.shared.MissingFoodDescription;
import net.scran24.user.shared.RawFood;
import net.scran24.user.shared.TemplateFood;
import net.scran24.user.shared.TemplateFoodData;
import net.scran24.user.shared.UUID;
import net.scran24.user.shared.lookup.PortionSizeMethod;

import org.junit.Test;
import org.pcollections.client.HashTreePMap;
import org.pcollections.client.HashTreePSet;
import org.pcollections.client.PMap;
import org.pcollections.client.PSet;
import org.pcollections.client.PVector;
import org.pcollections.client.TreePVector;
import org.workcraft.gwt.shared.client.Either;
import org.workcraft.gwt.shared.client.Function2;
import org.workcraft.gwt.shared.client.Option;

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.Random;

public class SurveySerialisationTest extends GWTTestCase {

	private SerialisableSurveyCodec codec;

	private PortionSizeScriptManager scriptManager;
	private CompoundFoodTemplateManager templateManager;

	@Override
	public void gwtSetUp() {
		scriptManager = new PortionSizeScriptManager(DefaultPortionSizeScripts.getCtors());
		templateManager = new CompoundFoodTemplateManager(HashTreePMap.<String, TemplateFoodData> empty().plus("sandwich", FoodTemplates.sandwich)
				.plus("salad", FoodTemplates.salad));
		codec = GWT.create(SerialisableSurveyCodec.class);
	}

	private Survey emptySurvey() {
		return new Survey(TreePVector.<Meal> empty(), new Selection.EmptySelection(SelectionMode.AUTO_SELECTION), 0l, HashTreePSet.<String> empty(),
				HashTreePMap.<String, String> empty());
	}

	private String encode(Survey survey) {
		GWT.create(SerialisableSurveyCodec.class);

		return codec.encode(new SerialisableSurvey(survey)).toString();
	}

	private Survey decode(String json) {
		return codec.decode(json).toSurvey(scriptManager, templateManager);
	}

	private Boolean compareFoods(FoodEntry a, final FoodEntry b) {
		assertEquals(a.link, b.link);
		assertEquals(a.flags, b.flags);
		assertEquals(a.customData, b.customData);

		return a.accept(new FoodEntry.Visitor<Boolean>() {
			@Override
			public Boolean visitRaw(final RawFood food1) {
				if (b.isRaw()) {
					RawFood food2 = (RawFood) b;
					assertEquals(food1.description, food2.description);
					return true;
				} else {
					fail();
					return false;
				}
			}

			@Override
			public Boolean visitEncoded(EncodedFood food1) {
				if (b.isEncoded()) {
					EncodedFood food2 = (EncodedFood) b;

					assertEquals(food1.data.askIfReadyMeal, food2.data.askIfReadyMeal);
					assertEquals(food1.data.brands, food2.data.brands);
					assertEquals(food1.data.caloriesPer100g, food2.data.caloriesPer100g);
					assertEquals(food1.data.categories, food2.data.categories);
					assertEquals(food1.data.code, food2.data.code);
					assertEquals(food1.data.localDescription, food2.data.localDescription);
					assertEquals(food1.data.portionSizeMethods, food2.data.portionSizeMethods);
					assertEquals(food1.data.prompts, food2.data.prompts);
					assertEquals(food1.data.sameAsBeforeOption, food2.data.sameAsBeforeOption);

					assertEquals(food1.portionSizeMethodIndex, food2.portionSizeMethodIndex);
					assertEquals(food1.portionSize, food2.portionSize);
					assertEquals(food1.brand, food2.brand);
					assertEquals(food1.searchTerm, food2.searchTerm);
					assertEquals(food1.enabledPrompts, food2.enabledPrompts);

					return true;
				} else {
					fail();
					return false;
				}
			}

			@Override
			public Boolean visitCompound(CompoundFood food1) {
				if (b.isCompound()) {
					CompoundFood food2 = (CompoundFood) b;

					assertEquals(food1.isDrink, food2.isDrink);
					assertEquals(food1.description, food2.description);

					return true;
				} else {
					fail();
					return false;
				}
			}

			@Override
			public Boolean visitTemplate(TemplateFood food1) {
				if (b.isTemplate()) {
					TemplateFood food2 = (TemplateFood) b;

					assertEquals(food1.data.template_id, food2.data.template_id);
					assertEquals(food1.markedAsComplete, food2.markedAsComplete);
					assertEquals(food1.components, food2.components);
					assertEquals(food1.description, food2.description);

					return true;
				} else {
					fail();
					return false;
				}
			}

			@Override
			public Boolean visitMissing(MissingFood food1) {
				if (b.isMissing()) {
					MissingFood food2 = (MissingFood) b;

					assertEquals(food1.description, food2.description);

					return true;
				} else {
					fail();
					return false;
				}
			}
		});
	}

	private Boolean compareMeals(Meal a, Meal b) {
		assertEquals(a.name, b.name);
		assertEquals(a.flags, b.flags);
		assertEquals(a.time, b.time);
		assertEquals(a.customData, b.customData);

		return compareList(a.foods, b.foods, new Function2<FoodEntry, FoodEntry, Boolean>() {
			@Override
			public Boolean apply(FoodEntry arg1, FoodEntry arg2) {
				return compareFoods(arg1, arg2);
			};
		});
	}

	private <T> boolean compareList(PVector<T> a, PVector<T> b, Function2<T, T, Boolean> cmp) {
		assertEquals(a.size(), b.size());

		Iterator<T> ait = a.iterator();
		Iterator<T> bit = b.iterator();

		while (ait.hasNext()) {
			T anext = ait.next();
			T bnext = bit.next();
			if (!cmp.apply(anext, bnext))
				return false;
		}

		return true;
	}

	private boolean compare(Survey a, Survey b) {
		assertEquals(a.startTime, b.startTime);
		assertEquals(a.selectedElement, b.selectedElement);
		assertEquals(a.flags, b.flags);
		assertEquals(a.customData, b.customData);

		return compareList(a.meals, b.meals, new Function2<Meal, Meal, Boolean>() {
			@Override
			public Boolean apply(Meal arg1, Meal arg2) {
				return compareMeals(arg1, arg2);
			}
		});
	}

	private Selection generateSelection() {
		int type = Random.nextInt(3);

		SelectionMode mode = (Random.nextInt(2) == 0) ? SelectionMode.AUTO_SELECTION : SelectionMode.MANUAL_SELECTION;

		switch (type) {
		case 0:
			return new Selection.SelectedFood(Random.nextInt(100), Random.nextInt(100), mode);
		case 1:
			return new Selection.SelectedMeal(Random.nextInt(100), mode);
		case 2:
			return new Selection.EmptySelection(mode);
		default:
			fail();
			return null;
		}
	}

	private <T> Option<T> generateOption(T value) {
		if (Random.nextBoolean()) {
			return Option.some(value);
		} else {
			return Option.none();
		}
	}

	private FoodLink generateFoodLink() {
		return new FoodLink(UUID.randomUUID(), generateOption(UUID.randomUUID()));
	}

	private PVector<FoodEntry> generateFoods() {
		int count = Random.nextInt(10);

		PVector<FoodEntry> result = TreePVector.empty();

		for (int i = 0; i < count; i++) {

			PSet<String> flags = generateFlags();
			PMap<String, String> customData = generateCustomData();
			FoodLink link = generateFoodLink();

			int type = Random.nextInt(5);

			switch (type) {
			case 0:
				result = result.plus(new RawFood(link, generateString(16), flags, customData));
				break;
			case 1:
				result = result.plus(new EncodedFood(generateFoodData(), link, generateOption(Random.nextInt(50)), generatePortionSize(),
						generateOption(generateString(8)), generateString(16), generateFoodPrompts(), flags, customData));
				break;
			case 2:
				result = result.plus(new CompoundFood(link, generateString(16), Random.nextBoolean(), flags, customData));
				break;
			case 3:
				TemplateFoodData template = templateManager.getTemplate(generateTemplateId());
				PMap<Integer, PSet<UUID>> components = HashTreePMap.empty();
				for (int k = 0; k < template.template.size(); k++)
					components = components.plus(k, generateUuidSet());
				result = result.plus(new TemplateFood(link, generateString(16), Random.nextBoolean(), template, generateIntSet(), components, flags,
						customData));
				break;
			case 4:
				result = result.plus(new MissingFood(link, generateString(16), Random.nextBoolean(), generateOption(new MissingFoodDescription(
						generateOption(generateString(8)), generateOption(generateString(8)), generateOption(generateString(8)),
						generateOption(generateString(8))))));
				break;
			default:
				fail("mjo");
			}

		}

		return result;
	}

	private PSet<UUID> generateUuidSet() {
		int size2 = Random.nextInt(2);

		PSet<UUID> uuids = HashTreePSet.empty();

		for (int j = 0; j < size2; j++)
			uuids = uuids.plus(UUID.randomUUID());
		return uuids;
	}

	private String generateTemplateId() {
		String[] methods = new String[] { "salad", "sandwich" };
		return methods[Random.nextInt(methods.length)];
	}

	private <L, R> Either<L, R> generateEither(L leftCandidate, R rightCandidate) {
		if (Random.nextBoolean()) {
			return new Either.Left<L, R>(leftCandidate);
		} else
			return new Either.Right<L, R>(rightCandidate);
	}

	private String generatePortionSizeMethod() {
		String[] methods = new String[] { "as-served", "guide-image", "drink-scale", "standard-portion", "cereal", "milk-on-cereal",
				"milk-in-a-hot-drink", "pizza" };
		return methods[Random.nextInt(methods.length)];
	}

	private Option<Either<PortionSize, CompletedPortionSize>> generatePortionSize() {
		String method = generatePortionSizeMethod();

		return generateOption(generateEither(
				new PortionSize(method, generateCustomData(), scriptManager.getInstance(method), Option.<PortionSizeScript> none()),
				new CompletedPortionSize(method, generateCustomData())));
	}

	private PVector<FoodPrompt> generateFoodPrompts() {
		int count = Random.nextInt(10);

		PVector<FoodPrompt> result = TreePVector.empty();

		for (int i = 0; i < count; i++) {
			result = result
					.plus(new FoodPrompt(generateString(4), Random.nextBoolean(), generateString(16), Random.nextBoolean(), generateString(16)));
		}

		return result;
	}

	private FoodData generateFoodData() {
		return new FoodData(generateString(4), Random.nextBoolean(), Random.nextBoolean(), Random.nextDouble(), generateString(16),
				generatePortionSizeMethods(), generateFoodPrompts(), generateList(), generateList());
	}

	private List<PortionSizeMethod> generatePortionSizeMethods() {
		int count = Random.nextInt(10);

		PVector<PortionSizeMethod> result = TreePVector.empty();

		for (int i = 0; i < count; i++) {
			result = result.plus(new PortionSizeMethod(generatePortionSizeMethod(), generateString(8), generateString(8), Random.nextBoolean(),
					generateCustomData()));
		}

		return result;
	}

	private PVector<Meal> generateMeals() {
		int count = Random.nextInt(10);

		PVector<Meal> result = TreePVector.empty();

		for (int i = 0; i < count; i++) {
			result = result.plus(new Meal(generateString(16), generateFoods(), generateOption(new Time(Random.nextInt(24), Random.nextInt(60))),
					generateFlags(), generateCustomData()));
		}

		return result;
	}

	private Survey generateSurvey() {
		return new Survey(generateMeals(), generateSelection(), Random.nextInt(), generateFlags(), generateCustomData());
	}

	private static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

	private String generateString(int length) {
		StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; i++)
			sb.append(AB.charAt(Random.nextInt(AB.length())));
		return sb.toString();
	}

	private PSet<String> generateFlags() {
		int length = Random.nextInt(5);

		PSet<String> result = HashTreePSet.empty();

		for (int i = 0; i < length; i++) {
			result = result.plus(generateString(8));
		}

		return result;
	}

	private PSet<Integer> generateIntSet() {
		int length = Random.nextInt(5);

		PSet<Integer> result = HashTreePSet.empty();

		for (int i = 0; i < length; i++) {
			result = result.plus(Random.nextInt());
		}

		return result;
	}

	private PMap<String, String> generateCustomData() {
		int length = Random.nextInt(5);

		PMap<String, String> result = HashTreePMap.empty();

		for (int i = 0; i < length; i++) {
			result = result.plus(generateString(8), generateString(8));
		}

		return result;
	}

	private PVector<String> generateList() {
		int length = Random.nextInt(5);

		PVector<String> result = TreePVector.empty();

		for (int i = 0; i < length; i++) {
			result = result.plus(generateString(8));
		}

		return result;
	}

	@Test
	public void testRandom() throws VersionMismatchException {

		for (int i = 0; i < 50; i++) {

			Survey survey = generateSurvey();
			System.out.println("Iteration " + i);
			String json = encode(survey);
			System.out.println(json);
			Survey decoded = decode(json);

			compare(survey, decoded);
		}
	}

	@Override
	public String getModuleName() {
		return "net.scran24.user.Scran24User";
	}
}
