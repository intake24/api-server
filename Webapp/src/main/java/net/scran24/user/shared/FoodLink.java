/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.shared;


import org.workcraft.gwt.shared.client.Option;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class FoodLink {
	@JsonProperty
	public final UUID id;
	@JsonProperty
	public final Option<UUID> linkedTo;

	@JsonCreator
	public FoodLink(@JsonProperty("id") UUID id, @JsonProperty("linkedTo") Option<UUID> linkedTo) {
		this.id = id;
		this.linkedTo = linkedTo;
	}
	
	public boolean isLinked() {
		return !linkedTo.isEmpty();
	}

	public static FoodLink newUnlinked() {
		return new FoodLink(UUID.randomUUID(), Option.<UUID> none());
	}

	public static FoodLink newLinked(UUID linkedTo) {
		return new FoodLink(UUID.randomUUID(), Option.some(linkedTo));
	}
}
