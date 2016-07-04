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

package net.scran24.user.client.surveyscheme.cftest15;

import java.util.ArrayList;
import java.util.List;

import net.scran24.datastore.shared.SurveySchemeReference;
import net.scran24.user.client.SurveyInterfaceManager;
import net.scran24.user.client.survey.SurveyMessages;
import net.scran24.user.client.survey.WelcomePage;
import net.scran24.user.client.survey.flat.IntakeSurvey;
import net.scran24.user.client.survey.flat.Survey;
import net.scran24.user.client.surveyscheme.BasicScheme;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Anchor;

public class CrowdflowerTestNov15 extends BasicScheme {

	public static final SurveyMessages messages = SurveyMessages.Util.getInstance();

	private IntakeSurvey cachedSurveyPage = null;

	public CrowdflowerTestNov15(String locale, final SurveyInterfaceManager interfaceManager) {
		super(locale, interfaceManager);
	}

	@Override
	public void showNextPage() {
		final Survey state = stateManager.getCurrentState();

		if (!state.flags.contains(WelcomePage.FLAG_WELCOME_PAGE_SHOWN)) {
			interfaceManager.show(new WelcomePage(messages.welcomePage_welcomeText(), state));
		} else if (!state.customData.containsKey(ExternalUserIDPage.CUSTOM_DATA_KEY)) {
			Window.scrollTo(0, 0);
			interfaceManager.show(new ExternalUserIDPage(state));
		} else if (!state.completionConfirmed()) {
			Window.scrollTo(0, 0);
			if (cachedSurveyPage == null)
				cachedSurveyPage = new IntakeSurvey(stateManager, defaultPromptManager, defaultSelectionManager, defaultScriptManager);
			interfaceManager.show(cachedSurveyPage);
		} else {
			interfaceManager.show(new CrowdflowerFinalPage(postProcess(state, basicPostProcess), log.log));
		}
	}

	@Override
	public List<Anchor> navBarLinks() {
		List<Anchor> result = new ArrayList<Anchor>();
		
		Anchor giveUp = new Anchor("I am stuck/give up", "#");
		
		giveUp.addClickHandler(new ClickHandler() {
			
			@Override
			public void onClick(ClickEvent event) {
				//final Survey state = stateManager.getCurrentState();
				
				Window.alert("Give up!");
			}
		});
		
		result.add(giveUp);

		return result;
	}

	@Override
	public String getDataVersion() {
		return "1";
	}

	@Override
	public String getSchemeId() {
		return SurveySchemeReference.CrowdflowerNov15Scheme.ID;
	}
}
