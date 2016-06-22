/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.json.serialisable;

import java.util.Map;

import net.scran24.user.shared.lookup.PortionSizeMethod;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public  class SerialisablePortionSizeMethod {
	@JsonProperty
	public final String name;
	@JsonProperty
	public final Map<String, String> params;
	@JsonProperty
	public final String description;
	@JsonProperty
	public final String imageUrl;
	@JsonProperty
	public final boolean useForRecipes;
	
	@JsonCreator
	public SerialisablePortionSizeMethod(@JsonProperty("name") String name, @JsonProperty("description") String description, @JsonProperty("imageUrl") String imageUrl, 
			@JsonProperty("useForRecipes") boolean useForRecipes, @JsonProperty("params") Map<String, String> params) {
		this.name = name;
		this.description = description;
		this.imageUrl = imageUrl;
		this.useForRecipes = useForRecipes;
		this.params = params;
	}
	
	public SerialisablePortionSizeMethod(PortionSizeMethod method) {
		this(method.name, method.description, method.imageUrl, method.useForRecipes, method.params);
	}
	
	public PortionSizeMethod toPortionSizeMethod() {
		return new PortionSizeMethod(name, description, imageUrl, useForRecipes, params);
	}
}