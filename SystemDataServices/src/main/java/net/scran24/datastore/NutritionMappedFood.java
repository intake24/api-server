/*
This file is part of Intake24.

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

package net.scran24.datastore;

import java.util.Map;

import org.workcraft.gwt.shared.client.Option;

import net.scran24.datastore.shared.CompletedPortionSize;

import com.google.gwt.user.client.rpc.IsSerializable;

public class NutritionMappedFood implements IsSerializable {
	public String code;
	public String englishDescription;
	public Option<String> localDescription;
	public String nutrientTableID;
	public String nutrientTableCode;
	public boolean isReadyMeal;
	public String searchTerm;
	public CompletedPortionSize portionSize;
	public int foodGroupCode;
	public String foodGroupEnglishDescription;
	public Option<String> foodGroupLocalDescription;
	public boolean reasonableAmount;
	public String brand;
	public Map<Long, Double> nutrients;
	public Map<String, String> customData;

	@Deprecated
	public NutritionMappedFood() {
	}

	public NutritionMappedFood(String code, String englishDescription, Option<String> localDescription, String nutrientTableID, String nutrientTableCode, boolean isReadyMeal, String searchTerm, CompletedPortionSize portionSize,
			int foodGroupCode, String foodGroupEnglishDescription, Option<String> foodGroupLocalDescription, boolean reasonableAmount, String brand, Map<Long, Double> nutrients, Map<String, String> customData) {
		this.code = code;
		this.englishDescription = englishDescription;
		this.localDescription = localDescription;
		this.nutrientTableID = nutrientTableID;
		this.nutrientTableCode = nutrientTableCode;
		this.isReadyMeal = isReadyMeal;
		this.searchTerm = searchTerm;
		this.portionSize = portionSize;
		this.foodGroupCode = foodGroupCode;
		this.foodGroupEnglishDescription = foodGroupEnglishDescription;
		this.foodGroupLocalDescription = foodGroupLocalDescription;
		this.reasonableAmount = reasonableAmount;
		this.brand = brand;
		this.nutrients = nutrients;
		this.customData = customData;
	}
}
