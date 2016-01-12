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

package net.scran24.user.client.survey.flat;

import static org.workcraft.gwt.shared.client.CollectionUtils.filter;
import static org.workcraft.gwt.shared.client.CollectionUtils.forall;
import static org.workcraft.gwt.shared.client.CollectionUtils.flatten;
import static org.workcraft.gwt.shared.client.CollectionUtils.flattenOption;
import static org.workcraft.gwt.shared.client.CollectionUtils.map;
import static org.workcraft.gwt.shared.client.CollectionUtils.sort;
import static org.workcraft.gwt.shared.client.CollectionUtils.zipWithIndex;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import net.scran24.common.client.CurrentUser;

import net.scran24.datastore.shared.Time;
import net.scran24.user.shared.CompletedFood;
import net.scran24.user.shared.CompletedMeal;
import net.scran24.user.shared.CompletedMissingFood;
import net.scran24.user.shared.CompletedSurvey;
import net.scran24.user.shared.CompoundFood;
import net.scran24.user.shared.TemplateFood;
import net.scran24.user.shared.EncodedFood;
import net.scran24.user.shared.FoodEntry;
import net.scran24.user.shared.Meal;
import net.scran24.user.shared.MissingFood;
import net.scran24.user.shared.MissingFoodDescription;
import net.scran24.user.shared.RawFood;

import org.pcollections.client.PMap;
import org.pcollections.client.PSet;
import org.pcollections.client.PVector;
import org.workcraft.gwt.shared.client.CollectionUtils.WithIndex;
import org.workcraft.gwt.shared.client.Function1;
import org.workcraft.gwt.shared.client.Option;

public class Survey {
	public static final String FLAG_ENERGY_VALUE_CONFIRMED = "energy-value-confirmed";
	public static final String FLAG_COMPLETION_CONFIRMED = "completion-confirmed";
	public static final String FLAG_ENCODING_COMPLETE = "encoding-complete";
	public static final String FLAG_FREE_ENTRY_COMPLETE = "free-entry-complete";
	public static final String FLAG_NO_MORE_PROMPTS = "no-more-prompts";
	public static final String FLAG_SKIP_HISTORY = "skip-history";
	
	public final long startTime;

	public final PVector<Meal> meals;
	public final PVector<WithIndex<Meal>> mealsSortedByTime;
	public final Selection selectedElement;
	public final PSet<String> flags;
	public final PMap<String, String> customData;

	public Survey(PVector<Meal> meals, Selection selectedElement, long startTime, PSet<String> flags, PMap<String, String> customData) {
		this.meals = meals;
		this.startTime = startTime;
		this.customData = customData;
		
		PVector<WithIndex<Meal>> mealsWithIndex = zipWithIndex(meals);

		mealsSortedByTime = sort(mealsWithIndex, new Comparator<WithIndex<Meal>>() {
			@Override
			public int compare(WithIndex<Meal> arg0, WithIndex<Meal> arg1) {
				if (arg0.value.time.isEmpty()) {
					if (arg1.value.time.isEmpty())
						return 0;
					else
						return 1;
				} else {
					if (arg1.value.time.isEmpty())
						return -1;
					else {
						Time t0 = arg0.value.time.getOrDie();
						Time t1 = arg1.value.time.getOrDie();

						if (t0.hours != t1.hours)
							return t0.hours - t1.hours;
						else
							return t0.minutes - t1.minutes;
					}
				}
			}
		});

		this.selectedElement = selectedElement;

		PSet<String> f = flags;
		
		if (forall(meals, Meal.isFreeEntryCompleteFunc))
			f = f.plus(FLAG_FREE_ENTRY_COMPLETE);
		if (forall(meals, Meal.isEncodingCompleteFunc))
			f = f.plus(FLAG_ENCODING_COMPLETE);
		
		this.flags = f;
	}

	public boolean isPortionSizeComplete() {
		return forall(meals, Meal.isPortionSizeComplete);
	}

	public CompletedSurvey finalise(List<String> log) {
		PVector<CompletedMeal> completedMeals = map(meals, new Function1<Meal, CompletedMeal>() {
			@Override
			public CompletedMeal apply(Meal argument) {
				PVector<CompletedFood> completedFoods = map(filter(argument.foods, new Function1<FoodEntry, Boolean>() {
					@Override
					public Boolean apply(FoodEntry argument) {
						return !argument.isTemplate() && !argument.isCompound() &&!argument.isMissing();
					}
				}), new Function1<FoodEntry, CompletedFood>() {
					@Override
					public CompletedFood apply(FoodEntry foodEntry) {
						return foodEntry.finalise();
					}
				});

				return new CompletedMeal(argument.name, new ArrayList<CompletedFood>(completedFoods), argument.time
						.getOrDie("Cannot finalise survey because it contains an undefined time entry"), new HashMap<String, String>(argument.customData));
			}
		});
		
		
		PVector<CompletedMissingFood> missingFoods = flatten(map(meals, new Function1<Meal, PVector<CompletedMissingFood>>() {
			@Override
			public PVector<CompletedMissingFood> apply(Meal meal) {
				return flattenOption(map(meal.foods, new Function1<FoodEntry, Option<CompletedMissingFood>>() {
					@Override
					public Option<CompletedMissingFood> apply(FoodEntry foodEntry) {
						return foodEntry.accept(new FoodEntry.Visitor<Option<CompletedMissingFood>>() {
							@Override
							public Option<CompletedMissingFood> visitRaw(RawFood food) {
								return Option.none();
							}

							@Override
							public Option<CompletedMissingFood> visitEncoded(EncodedFood food) {
								return Option.none();
							}

							@Override
							public Option<CompletedMissingFood> visitTemplate(TemplateFood food) {
								return Option.none();
							}

							@Override
							public Option<CompletedMissingFood> visitMissing(MissingFood food) {
								
								MissingFoodDescription desc = food.description.getOrDie("Cannot finalise survey because it contains a missing food entry with no description");
								
								return Option.some(new CompletedMissingFood(
										food.name,
										desc.brand.getOrElse(null),
										desc.description.getOrElse(null),
										desc.portionSize.getOrElse(null),
										desc.leftovers.getOrElse(null)));
							}

							@Override
							public Option<CompletedMissingFood> visitCompound(
									CompoundFood food) {
								return Option.none();
							}
						});
					}
				}));			
			}
		}));
		
		// FIXME: username should be determined on the server
		return new CompletedSurvey(startTime, System.currentTimeMillis(), new ArrayList<CompletedMeal>(completedMeals),
				new ArrayList<CompletedMissingFood>(missingFoods), log, CurrentUser.userInfo.userName, new HashMap<String, String>(customData));
	}

	public Survey withSelection(Selection selectedElement) {
		return new Survey(meals, selectedElement, startTime, flags, customData);
	}

	public Survey plusMeal(Meal meal) {
		return new Survey(meals.plus(meal), selectedElement, startTime, flags, customData);
	}

	public Survey minusMeal(int mealIndex) {
		return new Survey(meals.minus(mealIndex), selectedElement, startTime, flags, customData);
	}

	public Survey updateMeal(int mealIndex, Meal value) {
		return new Survey(meals.with(mealIndex, value), selectedElement, startTime, flags, customData);
	}

	public Survey updateFood(int mealIndex, int foodIndex, FoodEntry value) {
		return updateMeal(mealIndex, meals.get(mealIndex).updateFood(foodIndex, value));
	}

	public Survey withMeals(PVector<Meal> newMeals) {
		return new Survey(newMeals, selectedElement, startTime, flags, customData);
	}

	public static Function1<Survey, Survey> addMealFunc(final Meal meal) {
		return new Function1<Survey, Survey>() {
			@Override
			public Survey apply(Survey argument) {
				return argument.plusMeal(meal);
			}
		};
	}

	public Survey invalidateSelection() {
		return this.withSelection(new Selection.EmptySelection(SelectionType.AUTO_SELECTION));
	}
	
	public Survey withFlag(String flag) {
		return new Survey(meals, selectedElement, startTime, flags.plus(flag), customData);		
	}
	
	public Survey clearFlag(String flag) {
		return new Survey(meals, selectedElement, startTime, flags.minus(flag), customData);
	}
	
	public Survey markCompletionConfirmed() {
		return withFlag(FLAG_COMPLETION_CONFIRMED);
	}
	
	public Survey clearCompletionConfirmed() {
		return clearFlag(FLAG_COMPLETION_CONFIRMED);		
	}
	
	public Survey markEnergyValueConfirmed() {
		return withFlag(FLAG_ENERGY_VALUE_CONFIRMED);
	}
	
	public Survey markFreeEntryComplete() {
		return withFlag(FLAG_FREE_ENTRY_COMPLETE);
	}
	
	public boolean freeEntryComplete() {
		return flags.contains(FLAG_FREE_ENTRY_COMPLETE);
	}
		
	public Survey clearEnergyValueConfirmed() {
		return clearFlag(FLAG_ENERGY_VALUE_CONFIRMED);
	}
	
	public boolean completionConfirmed() {
		return flags.contains(FLAG_COMPLETION_CONFIRMED);
	}
	
	public boolean energyValueConfirmed() {
		return flags.contains(FLAG_ENERGY_VALUE_CONFIRMED);
	}
	
	public Survey withData(PMap<String, String> newData) {
		return new Survey (meals, selectedElement, startTime, flags, newData);
	}
	
	public Survey withData(String key, String value) {
		return withData(customData.plus(key, value));
	}
}