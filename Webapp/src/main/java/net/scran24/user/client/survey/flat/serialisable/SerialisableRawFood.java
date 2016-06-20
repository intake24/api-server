/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey.flat.serialisable;

import java.util.Map;
import java.util.Set;

import net.scran24.user.shared.RawFood;

import org.pcollections.client.HashTreePMap;
import org.pcollections.client.HashTreePSet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SerialisableRawFood extends SerialisableFoodEntry {

	@JsonProperty
	public final String description;

	@JsonCreator
	public SerialisableRawFood(@JsonProperty("link") SerialisableFoodLink link, 
			@JsonProperty("description") String description, 
			@JsonProperty("flags") Set<String> flags, 
			@JsonProperty("customData") Map<String, String> customData) {
		super(link, HashTreePSet.from(flags), HashTreePMap.from(customData));
		this.description = description;
	}
	
	public RawFood toRawFood() {
		return new RawFood(link.toFoodLink(), description, flags, customData);		
	}
	
	@Override
	public <T> T accept(Visitor<T> visitor) {
		return visitor.visitRaw(this);
	}

}
