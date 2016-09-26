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

import com.google.gwt.user.client.rpc.IsSerializable;

public class NutritionMappedSurvey implements IsSerializable {
	public long startTime;
	public long endTime;
	public List<NutritionMappedMeal> meals;
	public List<String> log;
	public String userName;
	public Map<String, String> customData;

	@Deprecated
	public NutritionMappedSurvey() { }

	public NutritionMappedSurvey(long startTime, long endTime, List<NutritionMappedMeal> meals, List<String> log, String userName, Map<String, String> customData) {
		this.startTime = startTime;
		this.endTime = endTime;
		this.meals = meals;
		this.log = log;
		this.userName = userName;
		this.customData = customData;
	}
	
	public int timeToComplete() {
		return (int)(endTime - startTime) / (1000 * 60);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((customData == null) ? 0 : customData.hashCode());
		result = prime * result + (int) (endTime ^ (endTime >>> 32));
		result = prime * result + ((log == null) ? 0 : log.hashCode());
		result = prime * result + ((meals == null) ? 0 : meals.hashCode());
		result = prime * result + (int) (startTime ^ (startTime >>> 32));
		result = prime * result + ((userName == null) ? 0 : userName.hashCode());
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
		NutritionMappedSurvey other = (NutritionMappedSurvey) obj;
		if (customData == null) {
			if (other.customData != null)
				return false;
		} else if (!customData.equals(other.customData))
			return false;
		
		// System.out.println("NMS custom data ok");
		
		if (endTime != other.endTime)
			return false;
		
		// System.out.println("NMS end time ok");
		
		if (log == null) {
			if (other.log != null)
				return false;
		} else if (!log.equals(other.log))
			return false;
		
		// System.out.println("NMS log OK");
		
		if (meals == null) {
			if (other.meals != null)
				return false;
		} else if (!meals.equals(other.meals))
			return false;
		
		// System.out.println("NMS meals ok");
		
		if (startTime != other.startTime)
			return false;
		if (userName == null) {
			if (other.userName != null)
				return false;
		} else if (!userName.equals(other.userName))
			return false;
		
		
		// System.out.println("Start time ok");
		
		return true;
	}	
}
