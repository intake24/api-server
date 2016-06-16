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

package net.scran24.user.shared;

import static org.workcraft.gwt.shared.client.CollectionUtils.filter;
import static org.workcraft.gwt.shared.client.CollectionUtils.forall;
import static org.workcraft.gwt.shared.client.CollectionUtils.indexOf;
import static org.workcraft.gwt.shared.client.CollectionUtils.lastIndexOf;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.scran24.common.client.LocaleUtil;
import net.scran24.datastore.shared.Time;
import net.scran24.user.client.survey.prompts.messages.PromptMessages;

import org.pcollections.client.HashTreePMap;
import org.pcollections.client.HashTreePSet;
import org.pcollections.client.PMap;
import org.pcollections.client.PSet;
import org.pcollections.client.PVector;
import org.pcollections.client.TreePVector;
import org.workcraft.gwt.shared.client.CollectionUtils;
import org.workcraft.gwt.shared.client.Function1;
import org.workcraft.gwt.shared.client.Option;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.Window;

public class Meal {
	
	private static final PromptMessages messages = PromptMessages.Util.getInstance();
	
	private static final String FLAG_NO_MEALS_AFTER = "no-meals-after";
	private static final String FLAG_NO_MEALS_BEFORE = "no-meals-before";
	private static final String FLAG_CONFIRMED_NO_DRINKS = "confirmed-no-drinks";
	private static final String FLAG_FREE_ENTRY_COMPLETE = "free-entry-complete";
	private static final String FLAG_READY_MEALS_COMPLETE = "ready-meals-complete";
	
	@JsonProperty
	public final String name;
	@JsonProperty
	public final PVector<FoodEntry> foods;
	@JsonProperty
	public final Option<Time> time;
	@JsonProperty
	public final PSet<String> flags;
	@JsonProperty
	public final PMap<String, String> customData;
	
	public boolean isEmpty() {
		return foods.isEmpty();
	}
	
	public boolean encodingComplete() {
		return forall(foods, new Function1<FoodEntry, Boolean>() {
			@Override
			public Boolean apply(FoodEntry argument) {
				return argument.accept(new FoodEntry.Visitor<Boolean>() {
					@Override
					public Boolean visitRaw(RawFood food) {
						return false;
					}

					@Override
					public Boolean visitEncoded(EncodedFood food) {
						return true;
					}

					@Override
					public Boolean visitTemplate(TemplateFood food) {
						return true;
					}

					@Override
					public Boolean visitMissing(MissingFood food) {
						return true;
					}

					@Override
					public Boolean visitCompound(CompoundFood food) {
						return true;
					}
				});
			}
		});
	}

	public boolean portionSizeComplete() {
		return forall(foods, new Function1<FoodEntry, Boolean>() {
			@Override
			public Boolean apply(FoodEntry argument) {
				return argument.isPortionSizeComplete();
			}
		});
	}
	
	public boolean readyMealsComplete() {
		return flags.contains(FLAG_READY_MEALS_COMPLETE);
	}
	
	public boolean freeEntryComplete() {
		return flags.contains(FLAG_FREE_ENTRY_COMPLETE);
	}
	
	public boolean confirmedNoDrinks() {
		return flags.contains(FLAG_CONFIRMED_NO_DRINKS);
	}
	
	public boolean confirmedNoMealsBefore() {
		return flags.contains(FLAG_NO_MEALS_BEFORE);
	}
	
	public boolean confirmedNoMealsAfter() {
		return flags.contains(FLAG_NO_MEALS_AFTER);
	}

	public boolean hasDrinks() {
		return CollectionUtils.exists(foods, new Function1<FoodEntry, Boolean>() {
			@Override
			public Boolean apply(FoodEntry argument) {
				return argument.isDrink();
			}
		});
	}

	public Time guessTime() {
		String lcn = name.toLowerCase();
		if (lcn.contains(messages.predefMeal_Breakfast().toLowerCase()))
			return new Time(8, 0);
		else if (lcn.contains(messages.predefMeal_EarlySnack().toLowerCase()))
			return new Time(10, 30);
		else if (lcn.contains(messages.predefMeal_Lunch().toLowerCase()))
			return new Time(13, 0);
		else if (lcn.contains(messages.predefMeal_MidDaySnack().toLowerCase()))
			return new Time(16, 0);
		else if (lcn.contains(messages.predefMeal_Dinner().toLowerCase()) || lcn.contains(messages.predefMeal_EveningMeal().toLowerCase()))
			return new Time(19, 0);
		else if (lcn.contains(messages.predefMeal_LateSnack()))
			return new Time(22, 0);

		return new Time(12, 0);
	}

	public static PVector<FoodEntry> linkedFoods(final PVector<FoodEntry> foods, final FoodEntry mainFood) {
		return filter(foods, new Function1<FoodEntry, Boolean>() {
			@Override
			public Boolean apply(FoodEntry argument) {
				return argument.link.linkedTo.accept(new Option.Visitor<UUID, Boolean>() {
					@Override
					public Boolean visitSome(UUID item) {
						return item.equals(mainFood.link.id);
					}

					@Override
					public Boolean visitNone() {
						return false;
					}
				});
			}
		});
	}
	
	@JsonCreator
	public Meal(@JsonProperty("name") String name, @JsonProperty("foods") List<FoodEntry> foods, @JsonProperty("time") Option<Time> time, 
			@JsonProperty("flags") Set<String> flags, @JsonProperty("customData") Map<String, String> customData) {
		this(name, TreePVector.<FoodEntry>from(foods), time, HashTreePSet.<String>from(flags), HashTreePMap.<String, String>from(customData));
	}

	public Meal(String name, PVector<FoodEntry> foods, Option<Time> time, PSet<String> flags, PMap<String, String> customData) {
		this.name = name;
		this.foods = foods;
		this.time = time;
		this.flags = flags;
		this.customData = customData;
	}

	public int foodIndex(final UUID id) {
		return foodIndex(this.foods, id);
	}

	public int foodIndex(final FoodEntry food) {
		return foodIndex(food.link.id);
	}

	public static Meal empty(String name) {
		return new Meal(name, TreePVector.<FoodEntry> empty(), Option.<Time> none(), HashTreePSet.<String>empty(), HashTreePMap.<String, String>empty());
	}

	public Meal withTime(Time time) {
		return new Meal(this.name, this.foods, Option.<Time> some(time), this.flags, this.customData);
	}

	public Meal withFoods(PVector<FoodEntry> foods) {
		return new Meal(this.name, foods, this.time, this.flags, this.customData);
	}

	public Meal updateFood(int index, FoodEntry value) {
		return withFoods(foods.with(index, value));
	}

	public Meal minusFood(int index) {
		return withFoods(foods.minus(index));
	}

	private int insertionIndex(final FoodEntry value) {
		return insertionIndex(this.foods, value);	
	}
	
	public Meal plusAllFoods(Collection<FoodEntry> foodsToAdd) {
		PVector<FoodEntry> newFoods = foods;
		
		for (FoodEntry food : foodsToAdd) {
			newFoods = newFoods.plus(Meal.insertionIndex(newFoods, food), food);
		}
				
		return withFoods(newFoods);
	}

	public Meal plusFood(FoodEntry value) {
		// if the food being inserted is linked,
		// insert it after last food linked to the same parent food
		// if it is the only linked food, insert it after the parent food
		// this is done to maintain intuitive automatic selection order
		// for portion size estimation
		return withFoods(foods.plus(insertionIndex(value), value));
	}
	
	public Meal withFlag(String flag) {
		return new Meal(this.name, this.foods, this.time, this.flags.plus(flag), this.customData);
	}
	
	public Meal withCustomDataField(String key, String value) {
		return new Meal(this.name, this.foods, this.time, this.flags, this.customData.plus(key, value));
	}

	public Meal markFreeEntryComplete() {
		return withFlag(FLAG_FREE_ENTRY_COMPLETE);
	}
	
	public Meal markConfirmedNoDrinks() {
		return withFlag(FLAG_CONFIRMED_NO_DRINKS);
	}
	
	public Meal markNoMealsBefore() {
		return withFlag(FLAG_NO_MEALS_BEFORE);
	}

	public Meal markNoMealsAfter() {
		return withFlag(FLAG_NO_MEALS_AFTER);
	}
	
	public Meal markReadyMealsComplete() {
		return withFlag(FLAG_READY_MEALS_COMPLETE);
	}
	
	public String safeName() {
		return SafeHtmlUtils.htmlEscape(name.toLowerCase());
	}
	
	public String safeNameWithTime() {
		return safeName() + " (" + time.map(new Function1<Time, String>(){
			@Override
			public String apply(Time argument) {
				return argument.toString();
			}
		}).getOrElse("Time unknown") + ")";
	}
	
	public String safeNameWithTimeCapitalised() {
		String name = safeNameWithTime();
		return LocaleUtil.capitaliseFirstCharacter(name);
	}
	
	public static int foodIndex(PVector<FoodEntry> foods, final UUID id) {
		return indexOf(foods, new Function1<FoodEntry, Boolean>() {
			@Override
			public Boolean apply(FoodEntry argument) {
				return argument.link.id.equals(id);
			}
		});
	}
	
	public static int insertionIndex(PVector<FoodEntry> foods, final FoodEntry food) {
		if (food.link.isLinked()) {
			int lastLinkedIndex = lastIndexOf(foods, new Function1<FoodEntry, Boolean>() {
				@Override
				public Boolean apply(FoodEntry argument) {
					return argument.link.linkedTo.equalTo(food.link.linkedTo);
				}
			});

			if (lastLinkedIndex == -1)
				return foodIndex(foods, food.link.linkedTo.getOrDie()) + 1;
			else
				return lastLinkedIndex + 1;
		} else {
			return foods.size();
		}		
	}
	
	public static Function1<Meal, Meal> updateTimeFunc(final Time time) {
		return new Function1<Meal, Meal>() {

			@Override
			public Meal apply(Meal argument) {
				return argument.withTime(time);
			}
		};
	};

	public static Function1<Meal, Meal> updateFoodsFunc(final PVector<FoodEntry> foods) {
		return new Function1<Meal, Meal>() {

			@Override
			public Meal apply(Meal argument) {
				return argument.withFoods(foods);
			}
		};
	};

	public static Function1<Meal, Boolean> isEncodingCompleteFunc = new Function1<Meal, Boolean>() {
		@Override
		public Boolean apply(Meal argument) {
			return argument.encodingComplete();
		}
	};

	public static Function1<Meal, Boolean> isFreeEntryCompleteFunc = new Function1<Meal, Boolean>() {
		@Override
		public Boolean apply(Meal argument) {
			return argument.freeEntryComplete();
		}
	};

	public static Function1<Meal, Boolean> isPortionSizeComplete = new Function1<Meal, Boolean>() {
		@Override
		public Boolean apply(Meal argument) {
			return argument.portionSizeComplete();
		}
	};
}