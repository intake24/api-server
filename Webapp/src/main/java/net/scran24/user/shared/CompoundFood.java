/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
 */

package net.scran24.user.shared;

import java.util.Map;
import java.util.Set;

import org.pcollections.client.HashTreePMap;
import org.pcollections.client.HashTreePSet;
import org.pcollections.client.PMap;
import org.pcollections.client.PSet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CompoundFood extends FoodEntry {
	public static final String FLAG_INGREDIENTS_COMPLETE = "ingredients-complete";
	@JsonProperty
	public final String description;
	@JsonProperty
	public final boolean isDrink;

	public CompoundFood(FoodLink link, String description, boolean isDrink) {
		super(link, HashTreePSet.<String> empty(), HashTreePMap.<String, String> empty());
		this.description = description;
		this.isDrink = isDrink;
	}

	@JsonCreator
	@Deprecated
	public CompoundFood(@JsonProperty("link") FoodLink link, @JsonProperty("description") String description,
			@JsonProperty("isDrink") boolean isDrink, @JsonProperty("flags") Set<String> flags,
			@JsonProperty("customData") Map<String, String> customData) {
		this(link, description, isDrink, HashTreePSet.from(flags), HashTreePMap.from(customData));
	}

	public CompoundFood(FoodLink link, String description, boolean isDrink, PSet<String> flags, PMap<String, String> customData) {
		super(link, flags, customData);
		this.description = description;
		this.isDrink = isDrink;
	}

	@Override
	public CompoundFood relink(FoodLink link) {
		return new CompoundFood(link, description, isDrink, flags, customData);
	}

	@Override
	public String description() {
		return description;
	}

	@Override
	public boolean isDrink() {
		return isDrink;
	}

	@Override
	public CompoundFood withFlags(PSet<String> flags) {
		return new CompoundFood(link, description, isDrink, flags, customData);
	}

	@Override
	public CompoundFood withCustomDataField(String key, String value) {
		return new CompoundFood(link, description, isDrink, flags, customData.plus(key, value));
	}

	@Override
	public <T> T accept(Visitor<T> visitor) {
		return visitor.visitCompound(this);
	}
}
