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

package net.scran24.user.client.survey.flat.serialisable;

import static org.workcraft.gwt.shared.client.CollectionUtils.map;

import java.util.List;
import java.util.Map;
import java.util.Set;

import net.scran24.datastore.shared.Time;
import net.scran24.user.client.survey.CompoundFoodTemplateManager;
import net.scran24.user.client.survey.portionsize.experimental.PortionSizeScriptManager;
import net.scran24.user.shared.CompoundFood;
import net.scran24.user.shared.EncodedFood;
import net.scran24.user.shared.FoodEntry;
import net.scran24.user.shared.Meal;
import net.scran24.user.shared.MissingFood;
import net.scran24.user.shared.RawFood;
import net.scran24.user.shared.TemplateFood;

import org.pcollections.client.HashTreePMap;
import org.pcollections.client.HashTreePSet;
import org.pcollections.client.PMap;
import org.pcollections.client.PSet;
import org.pcollections.client.PVector;
import org.pcollections.client.TreePVector;
import org.workcraft.gwt.shared.client.Function1;
import org.workcraft.gwt.shared.client.Option;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;


public class SerialisableMeal {
	
	@JsonProperty
	public final String name;
	@JsonProperty
	public final PVector<SerialisableFoodEntry> foods;
	@JsonProperty
	public final Option<SerialisableTime> time;
	@JsonProperty
	public final PSet<String> flags;
	@JsonProperty
	public final PMap<String, String> customData;
	
	@JsonCreator
	public SerialisableMeal(
			@JsonProperty("name") String name,
			@JsonProperty("foods") List<SerialisableFoodEntry> foods,
			@JsonProperty("time") Option<SerialisableTime> time,
			@JsonProperty("flags") Set<String> flags,
			@JsonProperty("customData") Map<String, String> customData) {
		this.name = name;
		this.foods = TreePVector.from(foods);
		this.time = time;
		this.flags = HashTreePSet.from(flags);
		this.customData = HashTreePMap.from(customData);
	}
	
	public SerialisableMeal(Meal meal) {
		this.name = meal.name;
		this.foods = map(meal.foods, new Function1<FoodEntry, SerialisableFoodEntry>() {
			@Override
			public SerialisableFoodEntry apply(FoodEntry argument) {
				return argument.accept(new FoodEntry.Visitor<SerialisableFoodEntry>() {
					@Override
					public SerialisableFoodEntry visitRaw(RawFood food) {
						return new SerialisableRawFood(food);
					}

					@Override
					public SerialisableFoodEntry visitEncoded(EncodedFood food) {
						return new SerialisableEncodedFood(food);
					}

					@Override
					public SerialisableFoodEntry visitCompound(CompoundFood food) {
						return new SerialisableCompoundFood(food);
					}

					@Override
					public SerialisableFoodEntry visitTemplate(TemplateFood food) {
						return new SerialisableTemplateFood(food);
					}

					@Override
					public SerialisableFoodEntry visitMissing(MissingFood food) {
						return new SerialisableMissingFood(food);
					}
				});
			}			
		});
		this.time = meal.time.map(new Function1<Time, SerialisableTime>() {
			@Override
			public SerialisableTime apply(Time argument) {
				return new SerialisableTime(argument);
			}			
		});
		this.flags = meal.flags;
		this.customData = meal.customData;
	}
	
	public Meal toMeal(final PortionSizeScriptManager scriptManager, final CompoundFoodTemplateManager templateManager) {
		
		PVector<FoodEntry> mealFoods = map(foods, new Function1<SerialisableFoodEntry, FoodEntry>() {
			@Override
			public FoodEntry apply(SerialisableFoodEntry argument) {
				return argument.accept(new SerialisableFoodEntry.Visitor<FoodEntry>() {
					@Override
					public FoodEntry visitRaw(SerialisableRawFood food) {
						return food.toRawFood();
					}

					@Override
					public FoodEntry visitEncoded(SerialisableEncodedFood food) {
						return food.toEncodedFood(scriptManager);
					}

					@Override
					public FoodEntry visitCompound(SerialisableCompoundFood food) {
						return food.toCompoundFood();
					}

					@Override
					public FoodEntry visitTemplate(SerialisableTemplateFood food) {
						return food.toTemplateFood(templateManager);
					}

					@Override
					public FoodEntry visitMissing(SerialisableMissingFood food) {
						return food.toMissingFood();
					}
				});
			}			
		});
		
		Option<Time> mealTime = time.map(new Function1<SerialisableTime, Time>() {
			@Override
			public Time apply(SerialisableTime argument) {
				return argument.toTime();
			}			
		});
		
		
		return new Meal(name, mealFoods, mealTime, flags, customData);
	}
	
}