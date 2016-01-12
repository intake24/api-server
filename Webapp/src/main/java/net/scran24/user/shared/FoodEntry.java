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

import java.util.HashMap;

import net.scran24.datastore.shared.CompletedPortionSize;
import net.scran24.user.client.survey.portionsize.experimental.PortionSize;

import org.pcollections.client.PMap;
import org.pcollections.client.PSet;
import org.workcraft.gwt.shared.client.Either;
import org.workcraft.gwt.shared.client.Function1;
import org.workcraft.gwt.shared.client.Option;

public abstract class FoodEntry {
	public static final String FLAG_READY_MEAL = "ready-meal";

	public final FoodLink link;
	public final PSet<String> flags;
	public final PMap<String, String> customData;

	public interface Visitor<T> {
		public T visitRaw(RawFood food);

		public T visitEncoded(EncodedFood food);
		
		public T visitCompound(CompoundFood food);
		
		public T visitTemplate(TemplateFood food);

		public T visitMissing(MissingFood food);
	}

	public FoodEntry(FoodLink link, PSet<String> flags, PMap<String, String> customData) {
		this.link = link;
		this.flags = flags;
		this.customData = customData;
	}

	public abstract FoodEntry relink(FoodLink link);

	public abstract String description();

	public abstract boolean isDrink();

	public abstract FoodEntry withFlags(PSet<String> flags);

	public abstract FoodEntry withCustomDataField(String key, String value);

	public boolean isEncoded() {
		return this.accept(new Visitor<Boolean>() {
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
				return false;
			}

			@Override
			public Boolean visitMissing(MissingFood food) {
				return false;
			}

			@Override
			public Boolean visitCompound(CompoundFood food) {
				return false;
			}
		});
	}

	public boolean isRaw() {
		return this.accept(new Visitor<Boolean>() {
			@Override
			public Boolean visitRaw(RawFood food) {
				return true;
			}

			@Override
			public Boolean visitEncoded(EncodedFood food) {
				return false;
			}

			@Override
			public Boolean visitTemplate(TemplateFood food) {
				return false;
			}

			@Override
			public Boolean visitMissing(MissingFood food) {
				return false;
			}

			@Override
			public Boolean visitCompound(CompoundFood food) {
				return false;
			}
		});
	}

	public boolean isTemplate() {
		return this.accept(new Visitor<Boolean>() {
			@Override
			public Boolean visitRaw(RawFood food) {
				return false;
			}

			@Override
			public Boolean visitEncoded(EncodedFood food) {
				return false;
			}

			@Override
			public Boolean visitTemplate(TemplateFood food) {
				return true;
			}

			@Override
			public Boolean visitMissing(MissingFood food) {
				return false;
			}

			@Override
			public Boolean visitCompound(CompoundFood food) {
				return false;
			}
		});
	}
	
	public boolean isCompound() {
		return this.accept(new Visitor<Boolean>() {
			@Override
			public Boolean visitRaw(RawFood food) {
				return false;
			}

			@Override
			public Boolean visitEncoded(EncodedFood food) {
				return false;
			}

			@Override
			public Boolean visitTemplate(TemplateFood food) {
				return false;
			}

			@Override
			public Boolean visitMissing(MissingFood food) {
				return false;
			}

			@Override
			public Boolean visitCompound(CompoundFood food) {
				return true;
			}
		});
	}

	public boolean isMissing() {
		return this.accept(new Visitor<Boolean>() {
			@Override
			public Boolean visitRaw(RawFood food) {
				return false;
			}

			@Override
			public Boolean visitEncoded(EncodedFood food) {
				return false;
			}

			@Override
			public Boolean visitTemplate(TemplateFood food) {
				return false;
			}

			@Override
			public Boolean visitMissing(MissingFood food) {
				return true;
			}

			@Override
			public Boolean visitCompound(CompoundFood food) {
				return false;
			}
		});
	}

	public boolean isPortionSizeComplete() {
		return this.accept(new Visitor<Boolean>() {
			@Override
			public Boolean visitRaw(RawFood food) {
				return false;
			}

			@Override
			public Boolean visitEncoded(EncodedFood food) {
				return food.isPortionSizeComplete();
			}

			@Override
			public Boolean visitTemplate(TemplateFood food) {
				return true;
			}

			@Override
			public Boolean visitMissing(MissingFood food) {
				return food.isDescriptionComplete();
			}

			@Override
			public Boolean visitCompound(CompoundFood food) {
				return true;
			}
		});
	}

	public EncodedFood asEncoded() {
		return this.accept(new Visitor<EncodedFood>() {
			@Override
			public EncodedFood visitRaw(RawFood food) {
				throw new IllegalStateException("not an encoded food");
			}

			@Override
			public EncodedFood visitEncoded(EncodedFood food) {
				return food;
			}

			@Override
			public EncodedFood visitTemplate(TemplateFood food) {
				throw new IllegalStateException("not an encoded food");
			}

			@Override
			public EncodedFood visitMissing(MissingFood food) {
				throw new IllegalStateException("not an encoded food");
			}

			@Override
			public EncodedFood visitCompound(CompoundFood food) {
				throw new IllegalStateException("not an encoded food");
			}
		});
	}

	public FoodEntry withFlag(String flag) {
		return withFlags(flags.plus(flag));
	}

	public FoodEntry markReadyMeal() {
		return withFlag(FLAG_READY_MEAL);
	}

	public static final Function1<FoodEntry, Boolean> isPortionSizeComplete = new Function1<FoodEntry, Boolean>() {
		@Override
		public Boolean apply(FoodEntry argument) {
			return argument.isPortionSizeComplete();
		}
	};
	
	public static final Function1<FoodEntry, Boolean> isEncodedFunc = new Function1<FoodEntry, Boolean>() {
		@Override
		public Boolean apply(FoodEntry argument) {
			return argument.isEncoded();
		}
	};

	public static final Function1<FoodEntry, Boolean> isNotEncodedFunc = new Function1<FoodEntry, Boolean>() {
		@Override
		public Boolean apply(FoodEntry argument) {
			return argument.isRaw();
		}
	};

	public static final Function1<FoodEntry, Boolean> isPortionSizeUnknown = new Function1<FoodEntry, Boolean>() {
		@Override
		public Boolean apply(FoodEntry argument) {
			return !argument.isPortionSizeComplete();
		}
	};

	abstract public <T> T accept(Visitor<T> visitor);

	public CompletedFood finalise() {
		return accept(new FoodEntry.Visitor<CompletedFood>() {
			@Override
			public CompletedFood visitRaw(RawFood food) {
				throw new IllegalStateException("Cannot finalise a raw food entry");
			}

			@Override
			public CompletedFood visitTemplate(TemplateFood food) {
				throw new IllegalStateException("Cannot finalise a template food entry");
			}

			@Override
			public CompletedFood visitEncoded(EncodedFood food) {
				return new CompletedFood(food.data.code, food.flags.contains(FLAG_READY_MEAL), food.searchTerm,
						food.portionSize.accept(new Option.Visitor<Either<PortionSize, CompletedPortionSize>, CompletedPortionSize>() {
							@Override
							public CompletedPortionSize visitSome(Either<PortionSize, CompletedPortionSize> item) {
								return item.accept(new Either.Visitor<PortionSize, CompletedPortionSize, CompletedPortionSize>() {
									@Override
									public CompletedPortionSize visitRight(CompletedPortionSize value) {
										return value;
									}

									@Override
									public CompletedPortionSize visitLeft(PortionSize value) {
										throw new IllegalStateException("Attempt to finalise an incomplete portion size");
									}
								});
							}

							@Override
							public CompletedPortionSize visitNone() {
								throw new IllegalStateException("Attempt to finalise an incomplete portion size");
							}
						}), food.brand.accept(new Option.Visitor<String, String>() {
							@Override
							public String visitSome(String item) {
								return item;
							}

							@Override
							public String visitNone() {
								return "N/A";
							}
						}), new HashMap<String, String>(customData));
			}

			@Override
			public CompletedFood visitMissing(MissingFood food) {
				MissingFoodDescription description = food.description.getOrDie("Cannot finalise missing food without description");
				
				HashMap<String, String> customData = new HashMap<String, String>(food.customData);
				
				customData.put(MissingFood.KEY_DESCRIPTION, description.description.getOrElse(""));
				customData.put(MissingFood.KEY_PORTION_SIZE, description.portionSize.getOrElse(""));
				customData.put(MissingFood.KEY_LEFTOVERS, description.leftovers.getOrElse(""));
				
				return new CompletedFood(SpecialData.FOOD_CODE_MISSING, false, food.name, CompletedPortionSize.ignore("Missing food"), description.brand.getOrElse(""), customData);
			}

			@Override
			public CompletedFood visitCompound(CompoundFood food) {
				throw new IllegalStateException("Cannot finalise a compound food entry");			}
		});
	}
}
