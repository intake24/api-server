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

package net.scran24.user.shared;

import java.util.Map;

import org.workcraft.gwt.shared.client.Option;

import net.scran24.datastore.shared.CompletedPortionSize;

import com.google.gwt.user.client.rpc.IsSerializable;

public class CompletedFood implements IsSerializable {
	public String code;
/*	public String englishDescription;
	public Option<String> localDescription;
	public Map<String, String> nutrientTableCodes;
	public int foodGroupCode;
	public int reasonableAmount; */
	public boolean isReadyMeal;
	public String searchTerm;
	public String brand;
	public CompletedPortionSize portionSize;
	public Map<String, String> customData;
	
	@Deprecated
	public CompletedFood() { }

	public CompletedFood(String code, /* String englishDescription, Option<String> localDescription, Map<String, String> nutrientTableCodes, int foodGroupCode, int reasonableAmount,*/ boolean isReadyMeal, String searchTerm, CompletedPortionSize portionSize, String brand, Map<String, String> customData) {
		this.isReadyMeal = isReadyMeal;
		this.code = code;
		/* this.englishDescription = englishDescription;
		this.localDescription = localDescription;
		this.nutrientTableCodes = nutrientTableCodes;
		this.foodGroupCode = foodGroupCode;
		this.reasonableAmount = reasonableAmount; */
		this.searchTerm = searchTerm;
		this.portionSize = portionSize;
		this.brand = brand;
		this.customData = customData;
	}
}
