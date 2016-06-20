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

import java.util.List;
import java.util.Map;
import java.util.Set;

import net.scran24.datastore.shared.CompletedPortionSize;
import net.scran24.user.client.survey.portionsize.experimental.PortionSize;
import net.scran24.user.client.survey.portionsize.experimental.PortionSizeScriptManager;
import net.scran24.user.shared.EncodedFood;
import net.scran24.user.shared.FoodPrompt;

import org.pcollections.client.HashTreePMap;
import org.pcollections.client.HashTreePSet;
import org.pcollections.client.PVector;
import org.pcollections.client.TreePVector;
import org.workcraft.gwt.shared.client.Either;
import org.workcraft.gwt.shared.client.Function1;
import org.workcraft.gwt.shared.client.Option;

import static org.workcraft.gwt.shared.client.CollectionUtils.map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SerialisableEncodedFood extends SerialisableFoodEntry {

	@JsonProperty
	public final SerialisableFoodData data;
	@JsonProperty
	public final Option<Integer> portionSizeMethodIndex;
	@JsonProperty
	public final Option<Either<SerialisablePortionSize, SerialisableCompletedPortionSize>> portionSize;
	@JsonProperty
	public final Option<String> brand;
	@JsonProperty
	public final String searchTerm;
	@JsonProperty
	public final PVector<SerialisableFoodPrompt> enabledPrompts;

	@JsonCreator
	public SerialisableEncodedFood(
			@JsonProperty("data") SerialisableFoodData data, 
			@JsonProperty("link") SerialisableFoodLink link,
			@JsonProperty("portionSizeMethodIndex") Option<Integer> portionSizeMethodIndex,
			@JsonProperty("portionSize") Option<Either<SerialisablePortionSize, SerialisableCompletedPortionSize>> portionSize, 
			@JsonProperty("brand") Option<String> brand,
			@JsonProperty("searchTerm") String searchTerm,
			@JsonProperty("enabledPrompts") List<SerialisableFoodPrompt> enabledPrompts,
			@JsonProperty("flags") Set<String> flags,
			@JsonProperty("customData") Map<String, String> customData) {
		super(link, HashTreePSet.from(flags), HashTreePMap.from(customData));
		this.data = data;
		this.portionSizeMethodIndex = portionSizeMethodIndex;
		this.portionSize = portionSize;
		this.brand = brand;
		this.searchTerm = searchTerm;
		this.enabledPrompts = TreePVector.from(enabledPrompts);
	}

	private static Option<Either<SerialisablePortionSize, SerialisableCompletedPortionSize>> toSerialisable(
			Option<Either<PortionSize, CompletedPortionSize>> portionSize) {
		return portionSize.map(new Function1<Either<PortionSize, CompletedPortionSize>, Either<SerialisablePortionSize, SerialisableCompletedPortionSize>>() {
					@Override
					public Either<SerialisablePortionSize, SerialisableCompletedPortionSize> apply(Either<PortionSize, CompletedPortionSize> argument) {
						return argument.accept(new Either.Visitor<PortionSize, CompletedPortionSize, Either<SerialisablePortionSize, SerialisableCompletedPortionSize>>() {
							@Override
							public Either<SerialisablePortionSize, SerialisableCompletedPortionSize> visitRight(CompletedPortionSize value) {
								return new Either.Right<SerialisablePortionSize, SerialisableCompletedPortionSize>(new SerialisableCompletedPortionSize(value));
							}

							@Override
							public Either<SerialisablePortionSize, SerialisableCompletedPortionSize> visitLeft(PortionSize value) {
								return new Either.Left<SerialisablePortionSize, SerialisableCompletedPortionSize>(new SerialisablePortionSize(value));
							}
						});
					}});
	}
	
	private static Option<Either<PortionSize, CompletedPortionSize>> toRuntime(Option<Either<SerialisablePortionSize, SerialisableCompletedPortionSize>> portionSize, final PortionSizeScriptManager scriptManager) {
		return portionSize.map(new Function1<Either<SerialisablePortionSize, SerialisableCompletedPortionSize>, Either<PortionSize, CompletedPortionSize>> () {
			@Override
			public Either<PortionSize, CompletedPortionSize> apply(Either<SerialisablePortionSize, SerialisableCompletedPortionSize> argument) {
				return argument.accept(new Either.Visitor<SerialisablePortionSize, SerialisableCompletedPortionSize, Either<PortionSize, CompletedPortionSize>> () {
					@Override
					public Either<PortionSize, CompletedPortionSize> visitRight(SerialisableCompletedPortionSize value) {
						return new Either.Right<PortionSize, CompletedPortionSize>(value.toCompletedPortionSize());
					}

					@Override
					public Either<PortionSize, CompletedPortionSize> visitLeft(SerialisablePortionSize value) {
						return new Either.Left<PortionSize, CompletedPortionSize>(value.toPortionSize(scriptManager));
					}					
				});
			}			
		});
	}

	public SerialisableEncodedFood(EncodedFood food) {
		this(
				new SerialisableFoodData(food.data), 
				new SerialisableFoodLink(food.link), 				
				food.portionSizeMethodIndex, 
				toSerialisable(food.portionSize),
				food.brand,
				food.searchTerm,
				map(food.enabledPrompts, new Function1<FoodPrompt, SerialisableFoodPrompt>() {
					@Override
					public SerialisableFoodPrompt apply(FoodPrompt argument) {
						return new SerialisableFoodPrompt(argument);
					}			
				}),
				food.flags,
				food.customData
			);			
	}
	
	public EncodedFood toEncodedFood(PortionSizeScriptManager scriptManager) {
		return new EncodedFood(data.toFoodData(), link.toFoodLink(), portionSizeMethodIndex, toRuntime(portionSize, scriptManager), brand, searchTerm,
				map(enabledPrompts, new Function1<SerialisableFoodPrompt, FoodPrompt>() {
					@Override
					public FoodPrompt apply(SerialisableFoodPrompt argument) {
						return argument.toFoodPrompt();
					}
					
				}), 
				flags, 
				customData);
	}

	@Override
	public <T> T accept(Visitor<T> visitor) {
		return visitor.visitEncoded(this);
	}
}
