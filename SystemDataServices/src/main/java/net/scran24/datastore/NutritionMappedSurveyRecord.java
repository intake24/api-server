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

public class NutritionMappedSurveyRecord {
	final public NutritionMappedSurvey survey;
	final public Map<String, String> userCustomFields;

	public NutritionMappedSurveyRecord(NutritionMappedSurvey survey, Map<String, String> customFields) {
		this.survey = survey;
		this.userCustomFields = customFields;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((survey == null) ? 0 : survey.hashCode());
		result = prime * result + ((userCustomFields == null) ? 0 : userCustomFields.hashCode());
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
		NutritionMappedSurveyRecord other = (NutritionMappedSurveyRecord) obj;
		
		if (survey == null) {
			if (other.survey != null)
				return false;
		} else if (!survey.equals(other.survey))
			return false;
		
		// System.out.println ("survey ok");
		
		if (userCustomFields == null) {
			if (other.userCustomFields != null)
				return false;
		} else if (!userCustomFields.equals(other.userCustomFields))
			return false;
		
		
		// System.out.println ("custom fields ok");
		
		return true;
	}
}
