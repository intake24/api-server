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

package net.scran24.user.client.json.serialisable;

import static org.workcraft.gwt.shared.client.CollectionUtils.map;
import net.scran24.user.client.survey.CompoundFoodTemplateManager;
import net.scran24.user.client.survey.portionsize.experimental.PortionSizeScriptManager;
import net.scran24.user.shared.CompoundFood;
import net.scran24.user.shared.EncodedFood;
import net.scran24.user.shared.FoodEntry;
import net.scran24.user.shared.MissingFood;
import net.scran24.user.shared.RawFood;
import net.scran24.user.shared.TemplateFood;

import org.pcollections.client.PMap;
import org.pcollections.client.PSet;
import org.pcollections.client.PVector;
import org.workcraft.gwt.shared.client.Function1;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

@JsonSubTypes({ @Type(SerialisableRawFood.class), @Type(SerialisableEncodedFood.class), @Type(SerialisableCompoundFood.class),
		@Type(SerialisableTemplateFood.class), @Type(SerialisableMissingFood.class) })
@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "entryType")
public abstract class SerialisableFoodEntry {

	@JsonProperty
	public final SerialisableFoodLink link;
	@JsonProperty
	public final PSet<String> flags;
	@JsonProperty
	public final PMap<String, String> customData;

	public interface Visitor<T> {
		public T visitRaw(SerialisableRawFood food);

		public T visitEncoded(SerialisableEncodedFood food);

		public T visitCompound(SerialisableCompoundFood food);

		public T visitTemplate(SerialisableTemplateFood food);

		public T visitMissing(SerialisableMissingFood food);
	}

	public SerialisableFoodEntry(SerialisableFoodLink link, PSet<String> flags, PMap<String, String> customData) {
		this.link = link;
		this.flags = flags;
		this.customData = customData;
	}

	abstract public <T> T accept(Visitor<T> visitor);

	public FoodEntry toFoodEntry(final PortionSizeScriptManager scriptManager, final CompoundFoodTemplateManager templateManager) {
		return accept(new Visitor<FoodEntry>() {
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

	public static PVector<SerialisableFoodEntry> toSerialisable(PVector<FoodEntry> foods) {
		return map(foods, new Function1<FoodEntry, SerialisableFoodEntry>() {
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
	}

	public static PVector<FoodEntry> toRuntime(PVector<SerialisableFoodEntry> foods, final PortionSizeScriptManager scriptManager, final CompoundFoodTemplateManager templateManager) {
		return map(foods, new Function1<SerialisableFoodEntry, FoodEntry>() {
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
	}
}
