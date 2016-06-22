/*
This file is part of Intake24

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

package net.scran24.user.client.json.serialisable;

import java.util.List;

import net.scran24.user.shared.FoodData;
import net.scran24.user.shared.FoodPrompt;
import net.scran24.user.shared.lookup.PortionSizeMethod;

import org.pcollections.client.PVector;
import org.pcollections.client.TreePVector;
import org.workcraft.gwt.shared.client.Function1;

import static org.workcraft.gwt.shared.client.CollectionUtils.map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SerialisableFoodData {
	@JsonProperty
	public final String code;
	@JsonProperty
	public final String localDescription;
	@JsonProperty
	public final boolean askIfReadyMeal;
	@JsonProperty
	public final boolean sameAsBeforeOption;
	@JsonProperty
	public final double caloriesPer100g;
	@JsonProperty
	public final PVector<SerialisablePortionSizeMethod> portionSizeMethods;
	@JsonProperty
	public final PVector<SerialisableFoodPrompt> prompts;
	@JsonProperty
	public final PVector<String> brands;
	@JsonProperty
	public final PVector<String> categories;

	@JsonCreator
	public SerialisableFoodData(@JsonProperty("code") String code, 
			@JsonProperty("askIfReadyMeal") boolean askIfReadyMeal,
			@JsonProperty("sameAsBeforeOption") boolean sameAsBeforeOption, 
			@JsonProperty("caloriesPer100g") double caloriesPer100g,
			@JsonProperty("localDescription") String localDescription,
			@JsonProperty("portionSizeMethods") List<SerialisablePortionSizeMethod> portionSizeMethods,
			@JsonProperty("prompts") List<SerialisableFoodPrompt> prompts, 
			@JsonProperty("brands") List<String> brands,
			@JsonProperty("categories") List<String> categories) {
		this.askIfReadyMeal = askIfReadyMeal;
		this.sameAsBeforeOption = sameAsBeforeOption;
		this.caloriesPer100g = caloriesPer100g;
		this.localDescription = localDescription;
		this.code = code;
		this.portionSizeMethods = TreePVector.from(portionSizeMethods);
		this.prompts = TreePVector.from(prompts);
		this.brands = TreePVector.from(brands);
		this.categories = TreePVector.from(categories);
	}

	public SerialisableFoodData(FoodData data) {
		this(data.code, data.askIfReadyMeal, data.sameAsBeforeOption, data.caloriesPer100g, data.localDescription, 
				map(TreePVector.from(data.portionSizeMethods), new Function1<PortionSizeMethod, SerialisablePortionSizeMethod>() {
					@Override
					public SerialisablePortionSizeMethod apply(PortionSizeMethod argument) {
						return new SerialisablePortionSizeMethod(argument);
					}

				}), 
				map(TreePVector.from(data.prompts), new Function1<FoodPrompt, SerialisableFoodPrompt>() {
					@Override
					public SerialisableFoodPrompt apply(FoodPrompt argument) {
						return new SerialisableFoodPrompt(argument);
					}
				}), 
				TreePVector.from(data.brands), TreePVector.from(data.categories));
	}
	
	public FoodData toFoodData() {
		return new FoodData(code, askIfReadyMeal, sameAsBeforeOption, caloriesPer100g, localDescription,
				map(portionSizeMethods, new Function1<SerialisablePortionSizeMethod, PortionSizeMethod>() {
					@Override
					public PortionSizeMethod apply(SerialisablePortionSizeMethod argument) {
						return argument.toPortionSizeMethod();
					}

				}), 
				map(prompts, new Function1<SerialisableFoodPrompt, FoodPrompt>() {
					@Override
					public FoodPrompt apply(SerialisableFoodPrompt argument) {
						return argument.toFoodPrompt();
					}
				}), 
				brands, categories);
	}

}