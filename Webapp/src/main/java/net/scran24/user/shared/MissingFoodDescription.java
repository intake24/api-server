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
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MissingFoodDescription other = (MissingFoodDescription) obj;
		if (brand == null) {
			if (other.brand != null)
				return false;
		} else if (!brand.equals(other.brand))
			return false;
		if (description == null) {
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		if (leftovers == null) {
			if (other.leftovers != null)
				return false;
		} else if (!leftovers.equals(other.leftovers))
			return false;
		if (portionSize == null) {
			if (other.portionSize != null)
				return false;
		} else if (!portionSize.equals(other.portionSize))
			return false;
		return true;
	}



	@Override
	public String toString() {
		return "MissingFoodDescription [brand=" + brand + ", description=" + description + ", portionSize=" + portionSize + ", leftovers="
				+ leftovers + "]";
	}
	
	
}
