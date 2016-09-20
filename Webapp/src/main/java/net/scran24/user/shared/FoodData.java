/*
This file is part of Intake24

Copyright 2015, 2016 Newcastle University.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

This file is based on Intake24 v1.0.

© Crown copyright, 2012, 2013, 2014

Licensed under the Open Government Licence 3.0: 

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.shared;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.scran24.user.shared.lookup.PortionSizeMethod;

import com.google.gwt.user.client.rpc.IsSerializable;

public class FoodData implements IsSerializable {
	public String code;
	public String localDescription;
	public boolean askIfReadyMeal;
	public boolean sameAsBeforeOption;
	public double caloriesPer100g;
	public List<PortionSizeMethod> portionSizeMethods;
	public List<FoodPrompt> prompts;
	public List<String> brands;
	public List<String> categories;
		
	@Deprecated
	public FoodData() { }
	
	public FoodData(String code, boolean askIfReadyMeal, boolean sameAsBeforeOption, double caloriesPer100g, String localDescription, List<PortionSizeMethod> portionSizeMethods, List<FoodPrompt> prompts, List<String> brands, List<String> categories) {
		this.askIfReadyMeal = askIfReadyMeal;
		this.sameAsBeforeOption = sameAsBeforeOption;
		this.caloriesPer100g = caloriesPer100g;
		this.localDescription = localDescription;
		this.code = code;
		this.portionSizeMethods = portionSizeMethods;
		this.prompts = prompts;
		this.brands = brands;
		this.categories = categories;
	}
	
	public FoodData withPortionSizeMethods (List<PortionSizeMethod> portionSizeMethods) {
		return new FoodData (code, askIfReadyMeal, sameAsBeforeOption, caloriesPer100g, localDescription, portionSizeMethods, prompts, brands, categories);
	}
	
	public FoodData withRecipePortionSizeMethods(PortionSizeMethod weight) {
		List<PortionSizeMethod> methods = new ArrayList<PortionSizeMethod>();
		
		for (PortionSizeMethod m: portionSizeMethods) {
			if (m.useForRecipes)
				methods.add(m);
		}
		
		methods.add(weight);
		
		return withPortionSizeMethods(methods);
	}
	
	public String description() {
		return localDescription;
	}
	

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FoodData other = (FoodData) obj;
		if (askIfReadyMeal != other.askIfReadyMeal)
			return false;
		if (brands == null) {
			if (other.brands != null)
				return false;
		} else if (!brands.equals(other.brands))
			return false;
		if (Double.doubleToLongBits(caloriesPer100g) != Double.doubleToLongBits(other.caloriesPer100g))
			return false;
		if (categories == null) {
			if (other.categories != null)
				return false;
		} else if (!categories.equals(other.categories))
			return false;
		if (code == null) {
			if (other.code != null)
				return false;
		} else if (!code.equals(other.code))
			return false;
		if (localDescription == null) {
			if (other.localDescription != null)
				return false;
		} else if (!localDescription.equals(other.localDescription))
			return false;
		if (portionSizeMethods == null) {
			if (other.portionSizeMethods != null)
				return false;
		} else if (!portionSizeMethods.equals(other.portionSizeMethods))
			return false;
		if (prompts == null) {
			if (other.prompts != null)
				return false;
		} else if (!prompts.equals(other.prompts))
			return false;
		if (sameAsBeforeOption != other.sameAsBeforeOption)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "FoodData [code=" + code + ", localDescription=" + localDescription + ", askIfReadyMeal=" + askIfReadyMeal + ", sameAsBeforeOption="
				+ sameAsBeforeOption + ", caloriesPer100g=" + caloriesPer100g + ", portionSizeMethods=" + portionSizeMethods + ", prompts=" + prompts
				+ ", brands=" + brands + ", categories=" + categories + "]";
	}
	
	
}
