/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.shared;

import org.workcraft.gwt.shared.client.Option;

public class MissingFoodDescription {
	
	public final Option<String> brand;
	public final Option<String> description;
	public final Option<String> portionSize;
	public final Option<String> leftovers;

	public MissingFoodDescription(Option<String> brand, Option<String> description, Option<String> portionSize, Option<String> leftovers) {
		this.brand = brand;
		this.description = description;
		this.portionSize = portionSize;
		this.leftovers = leftovers;
	}		
}
