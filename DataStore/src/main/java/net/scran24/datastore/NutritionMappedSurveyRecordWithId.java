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
*/

package net.scran24.datastore;

import java.util.Map;

public class NutritionMappedSurveyRecordWithId {
	final public NutritionMappedSurvey survey;
	final public Map<String, String> userCustomFields;
	final public String id;

	public NutritionMappedSurveyRecordWithId(NutritionMappedSurvey survey, Map<String, String> customFields, String id) {
		this.survey = survey;
		this.userCustomFields = customFields;
		this.id = id;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
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
		NutritionMappedSurveyRecordWithId other = (NutritionMappedSurveyRecordWithId) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		
		// System.out.println ("id ok");
		
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
