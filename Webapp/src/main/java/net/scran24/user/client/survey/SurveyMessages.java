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

package net.scran24.user.client.survey;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Messages;

public interface SurveyMessages extends Messages {

	public static class Util {
		private static SurveyMessages instance = null;

		public static SurveyMessages getInstance() {
			if (instance == null)
				instance = GWT.create(SurveyMessages.class);
			return instance;
		}
	}

	public String goBackLabel();

	public String addMealLabel();

	public String navPanelHeader();
	
	public String navPanelSuggestedTooltip();
	public String navPanelMatchedTooltip(); 
	public String navPanelNotMatchedTooltip();
	public String navPanelPortionSizeComplete(); 
	public String navPanelPortionSizeIncomplete(); 
	
	public String navBar_tutorialVideo();

	public String navBar_logOut();

	public String welcomePage_ready();

	public String welcomePage_welcomeText();

	public String finalPage_continueToSurveyMonkey();

	public String finalPage_text();

	public String survey_notInitialised();

	public String survey_suspended(String reason);
	
	public String survey_finished();

	public String help_nextButtonLabel();
	
	public String help_backButtonLabel();
	
	public String help_exitButtonLabel();
	
	public String help_stillStuckButtonLabel();

	public String help_doneButtonLabel();
	
}
