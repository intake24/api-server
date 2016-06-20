/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey.flat.serialisable;


import net.scran24.user.shared.FoodLink;
import net.scran24.user.shared.UUID;

import org.workcraft.gwt.shared.client.Option;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SerialisableFoodLink {
	@JsonProperty
	public final UUID id;
	@JsonProperty
	public final Option<UUID> linkedTo;

	@JsonCreator
	public SerialisableFoodLink(@JsonProperty("id") UUID id, @JsonProperty("linkedTo") Option<UUID> linkedTo) {
		this.id = id;
		this.linkedTo = linkedTo;
	}
	
	public SerialisableFoodLink(FoodLink link) {
		this(link.id, link.linkedTo);
	}
	
	public FoodLink toFoodLink() {
		return new FoodLink(id, linkedTo);		
	}
}
