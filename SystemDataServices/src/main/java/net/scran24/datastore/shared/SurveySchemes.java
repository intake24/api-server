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

package net.scran24.datastore.shared;

import java.util.Arrays;
import java.util.List;

/**
 * Maps string survey ids to type-safe SurveySchemeReference instances.
 */
public class SurveySchemes {
	public static final List<SurveySchemeReference> allSchemes = Arrays.asList( new SurveySchemeReference[] {
		new SurveySchemeReference.DefaultScheme(),
		new SurveySchemeReference.YoungScot2014Scheme(),
		new SurveySchemeReference.UclJan15Scheme(),
		new SurveySchemeReference.SHeSJun15Scheme(),
		new SurveySchemeReference.CrowdflowerNov15Scheme()
	});
	
	/**
	 * Get a type-safe SurveySchemeReference object for a given string id.
	 * @throws IllegalArgumentException if the survey id is unknown.
	 */
	public static SurveySchemeReference schemeForId(String schemeId) {
		if (schemeId.equals(SurveySchemeReference.DefaultScheme.ID)) 
			return new SurveySchemeReference.DefaultScheme();
		else if (schemeId.equals(SurveySchemeReference.YoungScot2014Scheme.ID))
			return new SurveySchemeReference.YoungScot2014Scheme();
		else if (schemeId.equals(SurveySchemeReference.UclJan15Scheme.ID))
			return new SurveySchemeReference.UclJan15Scheme();
		else if (schemeId.equals(SurveySchemeReference.SHeSJun15Scheme.ID))
			return new SurveySchemeReference.SHeSJun15Scheme();
		else if (schemeId.equals(SurveySchemeReference.CrowdflowerNov15Scheme.ID))
			return new SurveySchemeReference.CrowdflowerNov15Scheme();
		else
			throw new IllegalArgumentException("Unknown survey scheme: " + schemeId);
	}
}
