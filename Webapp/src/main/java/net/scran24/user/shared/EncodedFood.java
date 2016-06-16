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

import java.util.List;
import java.util.Map;
import java.util.Set;

import net.scran24.datastore.shared.CompletedPortionSize;
import net.scran24.user.client.survey.portionsize.experimental.PortionSize;

import org.pcollections.client.HashTreePMap;
import org.pcollections.client.HashTreePSet;
import org.pcollections.client.PMap;
import org.pcollections.client.PSet;
import org.pcollections.client.PVector;
import org.pcollections.client.TreePVector;
import org.workcraft.gwt.shared.client.Either;
import org.workcraft.gwt.shared.client.Option;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class EncodedFood extends FoodEntry {
	public final static String FLAG_NOT_SAME_AS_BEFORE = "not-same-as-before";

	@JsonProperty
	public final FoodData data;
	@JsonProperty
	public final Option<Integer> portionSizeMethodIndex;
	@JsonProperty
	public final Option<Either<PortionSize, CompletedPortionSize>> portionSize;
	@JsonProperty
	public final Option<String> brand;
	@JsonProperty
	public final String searchTerm;
	@JsonProperty
	public final PVector<FoodPrompt> enabledPrompts;

	@JsonCreator
	@Deprecated
	public EncodedFood(@JsonProperty("data") FoodData data, 
			@JsonProperty("link") FoodLink link, 
			@JsonProperty("portionSizeMethodIndex") Option<Integer> portionSizeMethodIndex,
			@JsonProperty("portionSize") Option<Either<PortionSize, CompletedPortionSize>> portionSize, 
			@JsonProperty("brand") Option<String> brand,
			@JsonProperty("searchTerm") String searchTerm, 
			@JsonProperty("enabledPrompts") List<FoodPrompt> enabledPrompts, 
			@JsonProperty("flags") Set<String> flags,
			@JsonProperty("customData") Map<String, String> customData) {
		this(data, link, portionSizeMethodIndex, portionSize, brand, searchTerm, TreePVector.from(enabledPrompts), HashTreePSet.from(flags),
				HashTreePMap.from(customData));
	}

	public EncodedFood(FoodData data, FoodLink link, Option<Integer> portionSizeMethodIndex,
			Option<Either<PortionSize, CompletedPortionSize>> portionSize, Option<String> brand, String searchTerm,
			PVector<FoodPrompt> enabledPrompts, PSet<String> flags, PMap<String, String> customData) {
		super(link, flags, customData);
		this.data = data;
		this.portionSizeMethodIndex = portionSizeMethodIndex;
		this.portionSize = portionSize;
		this.brand = brand;
		this.searchTerm = searchTerm;
		this.enabledPrompts = enabledPrompts;
	}

	public EncodedFood(FoodData data, Option<Integer> portionSizeMethodIndex, Option<Either<PortionSize, CompletedPortionSize>> portionSize,
			Option<String> brand, String searchTerm, FoodLink link, PSet<String> flags, PMap<String, String> customData) {
		this(data, link, portionSizeMethodIndex, portionSize, brand, searchTerm, TreePVector.<FoodPrompt> empty().plusAll(data.prompts), flags,
				customData);
	}

	public EncodedFood(FoodData data, FoodLink link, String searchTerm) {
		this(data, link, data.portionSizeMethods.size() == 1 ? Option.some(0) : Option.<Integer> none(), Option
				.<Either<PortionSize, CompletedPortionSize>> none(), Option.<String> none(), searchTerm, TreePVector.<FoodPrompt> empty().plusAll(
				data.prompts), HashTreePSet.<String> empty(), HashTreePMap.<String, String> empty());
	}

	@Override
	public <T> T accept(Visitor<T> visitor) {
		return visitor.visitEncoded(this);
	}

	@Override
	public String description() {
		return data.localDescription;
	}

	@Override
	public boolean isDrink() {
		return isInCategory("DRNK");
	}

	public boolean isPortionSizeComplete() {
		return portionSize.accept(new Option.Visitor<Either<PortionSize, CompletedPortionSize>, Boolean>() {
			@Override
			public Boolean visitSome(Either<PortionSize, CompletedPortionSize> item) {
				return item.accept(new Either.Visitor<PortionSize, CompletedPortionSize, Boolean>() {
					@Override
					public Boolean visitRight(CompletedPortionSize value) {
						return true;
					}

					@Override
					public Boolean visitLeft(PortionSize value) {
						return false;
					}
				});
			}

			@Override
			public Boolean visitNone() {
				return false;
			}
		});
	}

	public CompletedPortionSize completedPortionSize() {
		return portionSize.accept(new Option.Visitor<Either<PortionSize, CompletedPortionSize>, CompletedPortionSize>() {
			@Override
			public CompletedPortionSize visitSome(Either<PortionSize, CompletedPortionSize> item) {
				return item.accept(new Either.Visitor<PortionSize, CompletedPortionSize, CompletedPortionSize>() {
					@Override
					public CompletedPortionSize visitRight(CompletedPortionSize value) {
						return value;
					}

					@Override
					public CompletedPortionSize visitLeft(PortionSize value) {
						throw new IllegalStateException("portion size incomplete");
					}
				});
			}

			@Override
			public CompletedPortionSize visitNone() {
				throw new IllegalStateException("portion size incomplete");
			}
		});
	}

	public EncodedFood withSelectedPortionSizeMethod(int portionSizeMethodIndex) {
		return new EncodedFood(data, link, Option.some(portionSizeMethodIndex), portionSize, brand, searchTerm, enabledPrompts, flags, customData);
	}

	public EncodedFood withPortionSize(Either<PortionSize, CompletedPortionSize> size) {
		return new EncodedFood(data, link, portionSizeMethodIndex, Option.some(size), brand, searchTerm, enabledPrompts, flags, customData);
	}

	public EncodedFood minusPrompt(int index) {
		return new EncodedFood(data, link, portionSizeMethodIndex, portionSize, brand, searchTerm, enabledPrompts.minus(index), flags, customData);

	}

	public EncodedFood disableAllPrompts() {
		return new EncodedFood(data, link, portionSizeMethodIndex, portionSize, brand, searchTerm, TreePVector.<FoodPrompt> empty(), flags,
				customData);
	}

	public boolean isInCategory(String categoryCode) {
		return data.categories.contains(categoryCode);
	}

	@Override
	public String toString() {
		return link.id.toString() + " " + data.code + " " + data.description();
	}

	@Override
	public FoodEntry relink(FoodLink link) {
		return new EncodedFood(data, link, portionSizeMethodIndex, portionSize, brand, searchTerm, enabledPrompts, flags, customData);
	}

	@Override
	public FoodEntry withFlags(PSet<String> new_flags) {
		return new EncodedFood(data, link, portionSizeMethodIndex, portionSize, brand, searchTerm, enabledPrompts, new_flags, customData);
	}

	public EncodedFood markNotSameAsBefore() {
		return new EncodedFood(data, link, portionSizeMethodIndex, portionSize, brand, searchTerm, enabledPrompts,
				flags.plus(FLAG_NOT_SAME_AS_BEFORE), customData);
	}

	public EncodedFood withBrand(String name) {
		return new EncodedFood(data, link, portionSizeMethodIndex, portionSize, Option.some(name), searchTerm, enabledPrompts, flags, customData);
	}

	public boolean notSameAsBefore() {
		return flags.contains(FLAG_NOT_SAME_AS_BEFORE);
	}

	@Override
	public FoodEntry withCustomDataField(String key, String value) {
		return new EncodedFood(data, link, portionSizeMethodIndex, portionSize, brand, searchTerm, enabledPrompts, flags, customData.plus(key, value));

	}
}