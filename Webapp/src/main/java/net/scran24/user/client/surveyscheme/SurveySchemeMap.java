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

package net.scran24.user.client.surveyscheme;

import net.scran24.datastore.shared.SurveySchemeReference;
import net.scran24.user.client.SurveyInterfaceManager;
import net.scran24.user.client.surveyscheme.cftest15.CrowdflowerTestNov15;
import net.scran24.user.client.surveyscheme.shesjun15.SHeSJun15;
import net.scran24.user.client.surveyscheme.ucljan15.UCLJan15;

public class SurveySchemeMap {
	/**
	 * Create a survey scheme handler from a given survey scheme reference.
	 * @param welcomePageHtml
	 * HTML to display as welcome text. 
	 * Usually extracted from the host HTML page which allows for simple customisation.
	 * @param finalPageHtml
	 * HTML to display as a "debriefing" following a successful survey submission. 
	 * Usually extracted from the host HTML page which allows for simple customisation.
	 */
	public static SurveyScheme initScheme (SurveySchemeReference ref, final String locale, final SurveyInterfaceManager interfaceManager) {
		return ref.accept(new SurveySchemeReference.Visitor<SurveyScheme>() {
			@Override
			public SurveyScheme visitDefault() {
				return new DefaultScheme(locale, interfaceManager);
			}

			@Override
			public SurveyScheme visitYoungScot() {
				return new YoungScot2014Scheme(locale, interfaceManager);
			}
			
			public SurveyScheme visitUclJan15() {
				return new UCLJan15(locale, interfaceManager);
			}

			@Override
			public SurveyScheme visitSHeSJun15() {
				return new SHeSJun15(locale, interfaceManager);
			}

			@Override
			public SurveyScheme visitCrowdflowerNov15() {
				return new CrowdflowerTestNov15(locale, interfaceManager);
			}
		});
	}
}
