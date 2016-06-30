/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
 */

package net.scran24.user.client.json.serialisable;

import java.util.Map;
import java.util.Set;

import net.scran24.user.shared.MissingFood;
import net.scran24.user.shared.MissingFoodDescription;

import org.pcollections.client.HashTreePMap;
import org.pcollections.client.HashTreePSet;
import org.workcraft.gwt.shared.client.Function1;
import org.workcraft.gwt.shared.client.Option;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("missing")
public class SerialisableMissingFood extends SerialisableFoodEntry {
	@JsonProperty
	public final String name;
	@JsonProperty
	public final Option<SerialisableMissingFoodDescription> description;
	@JsonProperty
	public final boolean isDrink;

	@JsonCreator
	public SerialisableMissingFood(@JsonProperty("link") SerialisableFoodLink link, @JsonProperty("name") String name, @JsonProperty("isDrink") boolean isDrink,
			@JsonProperty("description") Option<SerialisableMissingFoodDescription> description, @JsonProperty("flags") Set<String> flags, @JsonProperty("customData") Map<String, String> customData) {
		super(link, HashTreePSet.from(flags), HashTreePMap.from(customData));
		
		this.name = name;
		this.description = description;
		this.isDrink = isDrink;
	}

	public SerialisableMissingFood(MissingFood food) {
		this(new SerialisableFoodLink(food.link), food.name, food.isDrink, food.description.map(new Function1<MissingFoodDescription, SerialisableMissingFoodDescription>() {
			@Override
			public SerialisableMissingFoodDescription apply(MissingFoodDescription argument) {
				return new SerialisableMissingFoodDescription(argument);
			}}), food.flags, food.customData); 
	}
	
	public MissingFood toMissingFood() {
		return new MissingFood(link.toFoodLink(), name, isDrink, description.map(new Function1<SerialisableMissingFoodDescription, MissingFoodDescription>() {
			@Override
			public MissingFoodDescription apply(SerialisableMissingFoodDescription argument) {
				return argument.toMissingFoodDescription();
			}			
		}), flags, customData);
	}

	@Override
	public <T> T accept(Visitor<T> visitor) {
		return visitor.visitMissing(this);
	}	
}
