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

package net.scran24.user.client.surveyscheme.shesjun15;

import net.scran24.common.client.CurrentUser;
import net.scran24.user.client.SurveyInterfaceManager;
import net.scran24.user.client.survey.SurveyMessages;
import net.scran24.user.client.survey.WelcomePage;
import net.scran24.user.client.survey.flat.FlatFinalPage;
import net.scran24.user.client.survey.flat.IntakeSurvey;
import net.scran24.user.client.survey.flat.Survey;
import net.scran24.user.client.surveyscheme.BasicScheme;
import net.scran24.user.client.surveyscheme.MultipleChoiceCheckboxQuestion;
import net.scran24.user.client.surveyscheme.MultipleChoiceRadioButtonQuestion;

import org.pcollections.client.PVector;
import org.pcollections.client.TreePVector;
import org.workcraft.gwt.shared.client.Option;

import com.google.gwt.safehtml.shared.SafeHtmlUtils;

public class SHeSJun15 extends BasicScheme {
	final private static SurveyMessages surveyMessages = SurveyMessages.Util.getInstance();

	final private PVector<String> dayOfWeekOptions = TreePVector.<String> empty()
			.plus("Monday")
			.plus("Tuesday")
			.plus("Wednesday")
			.plus("Thursday")
			.plus("Friday")
			.plus("Saturday")
			.plus("Sunday");
	
	final private PVector<String> foodTypeOptions = TreePVector.<String> empty()
			.plus("Yes")
			.plus("Don`t know");

	final private PVector<String> foodAmountOptions = TreePVector.<String> empty()
			.plus("Usual amount")
			.plus("Less than usual")
			.plus("More than usual")
			.plus("Don`t know");

	final private PVector<String> supplementOptions = TreePVector.<String> empty()
			.plus("Multivitamin")
			.plus("Multivitamin and mineral")
			.plus("Vitamin A")
			.plus("Vitamin B complex")
			.plus("Vitamin C")
			.plus("Vitamin D")
			.plus("Vitamin E")
			.plus("Calcium")
			.plus("Iron")
			.plus("Magnesium")
			.plus("Selenium")
			.plus("Zinc")
			.plus("Cod liver/ Fish oil")
			.plus("Evening Primrose oil")
			.plus("Chondroitin")
			.plus("Glucosamine");
	
	final private PVector<String> dietOptions = TreePVector.<String>empty()
			.plus("No diet")
			.plus("5:2 Diet (Intermittent Fasting)")
			.plus("Atkins")
			.plus("Blood Group")
			.plus("Blowout")
			.plus("Cabbage Soup")
			.plus("Cambridge Diet")
			.plus("Caveman")
			.plus("Detox")
			.plus("Dukan")
			.plus("FODMAP")
			.plus("Grapefruit Diet")
			.plus("High Fibre")
			.plus("Lighter Life")
			.plus("Low Carb")
			.plus("Low Fat")
			.plus("Low GI Diet")
			.plus("Maple Syrup Diet")
			.plus("Paleo")
			.plus("Rosemary Conley Diet")
			.plus("Slimming World")
			.plus("South Beach")
			.plus("Tony Ferguson")
			.plus("Weight Watchers");
	

	public SHeSJun15(final SurveyInterfaceManager interfaceManager) {
		super(interfaceManager);
	}

	private IntakeSurvey cachedSurveyPage = null;

	@Override
	public void showNextPage() {
		final Survey state = stateManager.getCurrentState();
		
		/* 	new CustomFieldDef("dayOfWeek", "Day of week"),
					new CustomFieldDef("usualFoods", "Usual foods"),
					new CustomFieldDef("foodAmount", "Food amount"),
					new CustomFieldDef("supplements", "Supplements"),
					new CustomFieldDef("diet", "Diet")});*/

		if (!state.flags.contains(WelcomePage.FLAG_WELCOME_PAGE_SHOWN)) {
			interfaceManager.show(new WelcomePage(surveyMessages.welcomePage_welcomeText(), state));
		} else if (!state.completionConfirmed()) {
			if (cachedSurveyPage == null)
				cachedSurveyPage = new IntakeSurvey(stateManager, defaultPromptManager, defaultSelectionManager, defaultScriptManager);
			interfaceManager.show(cachedSurveyPage);
		} else if (!state.customData.containsKey("dayOfWeek")) {
			interfaceManager.show(new MultipleChoiceRadioButtonQuestion(state, SafeHtmlUtils
					.fromSafeConstant("<p>Which day did you enter your food and drink intake for?</p>"), "Continue", dayOfWeekOptions, "dayOfWeek",
					Option.<String>none()));
		} else if (!state.customData.containsKey("usualFoods")) {
			interfaceManager.show(new MultipleChoiceRadioButtonQuestion(state, SafeHtmlUtils
					.fromSafeConstant("<p>Were the types of foods and drinks you had yesterday similar to what you normally have?</p>"), "Continue",
					foodTypeOptions, "usualFoods", Option.some("No (please explain why)")));
		} else if (!state.customData.containsKey("foodAmount")) {
			interfaceManager.show(new MultipleChoiceRadioButtonQuestion(state, SafeHtmlUtils
					.fromSafeConstant("<p>Was the amount of food that you had yesterday similar to what you normally have? </p>"), "Continue",
					foodAmountOptions, "foodAmount", Option.<String>none()));
		} else if (!state.customData.containsKey("supplements")) {
			interfaceManager.show(new MultipleChoiceCheckboxQuestion(state, SafeHtmlUtils
					.fromSafeConstant("<p>Do you take any dietary supplements e.g. Multivitamins?</p>"), "Continue", supplementOptions,
					"supplements", Option.some("Other")));
		} else if (!state.customData.containsKey("diet")) {
			interfaceManager.show(new MultipleChoiceRadioButtonQuestion(state, SafeHtmlUtils
					.fromSafeConstant("<p>Are you currently following a particular diet?</p>"), "Continue", dietOptions, "diet", Option.some("Other")));
		} else {
			interfaceManager.show(new FlatFinalPage(HtmlResources.INSTANCE.getFinalHtml().getText().replace("[intake24_username_value]", CurrentUser.getUserInfo().userName), postProcess(state, basicPostProcess), log.log));
		}
	}
}
