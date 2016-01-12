/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.shared;

import org.pcollections.client.PMap;
import org.pcollections.client.PSet;

public class RawFood extends FoodEntry {
	public static final String FLAG_DRINK = "drink";
	public static final String FLAG_DISABLE_SPLIT = "disable-split";
	public static final String KEY_LIMIT_LOOKUP_TO_CATEGORY = "limit-lookup-to-category";

	public final String description;

	public RawFood(FoodLink link, String description, PSet<String> flags, PMap<String, String> customData) {
		super(link, flags, customData);
		this.description = description;
	}

	@Override
	public <T> T accept(Visitor<T> visitor) {
		return visitor.visitRaw(this);
	}

	@Override
	public String description() {
		return description;
	}

	@Override
	public boolean isDrink() {
		return flags.contains(FLAG_DRINK);
	}

	public boolean applySplit() {
		return !flags.contains(FLAG_DISABLE_SPLIT);
	}

	@Override
	public FoodEntry relink(FoodLink link) {
		return new RawFood(link, description, flags, customData);
	}

	@Override
	public FoodEntry withFlags(PSet<String> new_flags) {
		return new RawFood(link, description, new_flags, customData);
	}

	@Override
	public FoodEntry withCustomDataField(String key, String value) {
		return new RawFood(link, description, flags, customData.plus(key, value));
	}
}
