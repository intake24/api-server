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
import net.scran24.user.client.survey.SurveyMessages;
import net.scran24.user.client.survey.WelcomePage;
import net.scran24.user.client.survey.flat.FlatFinalPage;
import net.scran24.user.client.survey.flat.IntakeSurvey;
import net.scran24.user.client.survey.flat.Survey;

/**
 * Default survey scheme with a welcome page that is customisable through the
 * host HTML page, the food intake survey and the final page, also customisable
 * using HTML.
 */
public class DefaultScheme extends BasicScheme {
	final private static SurveyMessages surveyMessages = SurveyMessages.Util.getInstance();

	public DefaultScheme(String locale, final SurveyInterfaceManager interfaceManager) {
		super(locale, interfaceManager);
	}

	private IntakeSurvey cachedSurveyPage = null;
	
	@Override
	public void showNextPage() {
		final Survey state = stateManager.getCurrentState();
		// Logger log = Logger.getLogger("showNextPage");
		// log.info(SurveyXmlSerialiser.toXml(state));

		if (!state.flags.contains(WelcomePage.FLAG_WELCOME_PAGE_SHOWN)) {
			interfaceManager.show(new WelcomePage(surveyMessages.welcomePage_welcomeText(), state));
		} else if (!state.completionConfirmed()) {
			if (cachedSurveyPage == null)
				cachedSurveyPage = new IntakeSurvey(stateManager, defaultPromptManager, defaultSelectionManager, defaultScriptManager);
			interfaceManager.show(cachedSurveyPage);
		} else {
			interfaceManager.show(new FlatFinalPage(surveyMessages.finalPage_text(), postProcess(state, basicPostProcess), log.log));
		}
	}

	@Override
	public String getDataVersion() {
		return "1";
	}

	@Override
	public String getSchemeId() {
		return SurveySchemeReference.DefaultScheme.ID;
	}

}