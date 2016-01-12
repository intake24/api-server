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

package net.scran24.user.shared.lookup;

import java.util.List;

import net.scran24.user.shared.CategoryHeader;
import net.scran24.user.shared.FoodHeader;

import com.google.gwt.user.client.rpc.IsSerializable;

public class LookupResult implements IsSerializable {
	public List<FoodHeader> foods;
	public List<CategoryHeader> categories;
	
	@Deprecated
	public LookupResult() { }

	public LookupResult(List<FoodHeader> foods, List<CategoryHeader> categories) {
		this.foods = foods;
		this.categories = categories;
	};

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append ("Foods:\n");
		
		for (FoodHeader f : foods)
			sb.append("  " + f.description() + "\n");
		
		sb.append ("Categories:\n");
		
		for (CategoryHeader c : categories)
			sb.append("  " + c.description() + "\n");
		
		return sb.toString();
	}
}