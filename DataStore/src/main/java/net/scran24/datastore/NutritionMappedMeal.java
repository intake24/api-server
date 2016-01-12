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

import java.util.List;
import java.util.Map;

import net.scran24.datastore.shared.Time;

import com.google.gwt.user.client.rpc.IsSerializable;

public class NutritionMappedMeal implements IsSerializable {
	public String name;
	public List<NutritionMappedFood> foods;
	public Time time;
	public Map<String, String> customData;

	@Deprecated
	public NutritionMappedMeal() {
	}

	public NutritionMappedMeal(String name, List<NutritionMappedFood> foods, Time time, Map<String, String> customData) {
		this.name = name;
		this.foods = foods;
		this.time = time;
		this.customData = customData;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((customData == null) ? 0 : customData.hashCode());
		result = prime * result + ((foods == null) ? 0 : foods.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((time == null) ? 0 : time.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NutritionMappedMeal other = (NutritionMappedMeal) obj;
		
		// System.out.println ("NMM begin");
		
		if (customData == null) {
			if (other.customData != null)
				return false;
		} else if (!customData.equals(other.customData))
			return false;
		
		// System.out.println("NMM custom data ok");
		
		if (foods == null) {
			if (other.foods != null)
				return false;
		} else if (!foods.equals(other.foods))
			return false;
		
		// System.out.println("NMM foods ok");
		
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		
		
		// System.out.println("NMM name ok");
		
		if (time == null) {
			if (other.time != null)
				return false;
		} else if (!time.equals(other.time))
			return false;
		
		// System.out.println("NMM time ok");
		
		return true;
	}

}
