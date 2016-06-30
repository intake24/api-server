/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.json.serialisable;

import net.scran24.user.shared.MissingFoodDescription;

import org.workcraft.gwt.shared.client.Option;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SerialisableMissingFoodDescription {
	
	@JsonProperty
	public final Option<String> brand;
	@JsonProperty
	public final Option<String> description;
	@JsonProperty
	public final Option<String> portionSize;
	@JsonProperty
	public final Option<String> leftovers;

	@JsonCreator
	public SerialisableMissingFoodDescription(@JsonProperty("brand") Option<String> brand, @JsonProperty("description") Option<String> description, 
			@JsonProperty("portionSize") Option<String> portionSize, @JsonProperty("leftovers") Option<String> leftovers) {
		this.brand = brand;
		this.description = description;
		this.portionSize = portionSize;
		this.leftovers = leftovers;
	}
	
	public SerialisableMissingFoodDescription(MissingFoodDescription missingFoodDescription) {
		this(missingFoodDescription.brand, missingFoodDescription.description, missingFoodDescription.portionSize, missingFoodDescription.leftovers);		
	}
	
	public MissingFoodDescription toMissingFoodDescription() {
		return new MissingFoodDescription(this.brand, this.description, this.portionSize, this.leftovers);
	}
}
