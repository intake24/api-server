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

package net.scran24.user.client.survey.portionsize.experimental;

import static net.scran24.user.client.survey.flat.PromptUtil.withBackLink;
import static net.scran24.user.client.survey.portionsize.experimental.PortionSizeScriptUtil.done;
import static net.scran24.user.client.survey.portionsize.experimental.PortionSizeScriptUtil.foodWeightPrompt;
import net.scran24.user.client.survey.flat.SimplePrompt;
import net.scran24.user.client.survey.prompts.messages.PromptMessages;
import net.scran24.user.shared.FoodData;

import org.pcollections.client.PMap;
import org.workcraft.gwt.shared.client.Option;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;

public class WeightPortionScript implements PortionSizeScript {
	public static final String name = "weight";

	private final PromptMessages messages = GWT.create(PromptMessages.class);
	
	@Override
	public Option<SimplePrompt<UpdateFunc>> nextPrompt(PMap<String, String> data, FoodData foodData) {
		if (data.containsKey("servingWeight"))
			return done();
		else
			return Option.some(withBackLink(foodWeightPrompt(
					SafeHtmlUtils.fromSafeConstant(messages.weightTypeIn_promptText(SafeHtmlUtils.htmlEscape(foodData.localDescription.getOrElse(foodData.englishDescription)))),
					SafeHtmlUtils.fromSafeConstant(messages.weightTypeIn_unitLabel()), messages.weightTypeIn_continueLabel(), "servingWeight", "leftoversWeight")));
	}
}
