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

import static net.scran24.user.client.survey.flat.PromptUtil.setAdditionalField;
import static net.scran24.user.client.survey.flat.PromptUtil.withBackLink;
import static net.scran24.user.client.survey.portionsize.experimental.PortionSizeScriptUtil.asServedPrompt;
import static net.scran24.user.client.survey.portionsize.experimental.PortionSizeScriptUtil.defaultLeftoversPrompt;
import static net.scran24.user.client.survey.portionsize.experimental.PortionSizeScriptUtil.defaultServingSizePrompt;
import static net.scran24.user.client.survey.portionsize.experimental.PortionSizeScriptUtil.done;
import static net.scran24.user.client.survey.portionsize.experimental.PortionSizeScriptUtil.yesNoPromptZeroField;
import net.scran24.user.client.survey.flat.SimplePrompt;
import net.scran24.user.client.survey.prompts.messages.PromptMessages;
import net.scran24.user.shared.FoodData;
import net.scran24.user.shared.lookup.AsServedDef;

import org.pcollections.client.PMap;
import org.workcraft.gwt.shared.client.Option;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;

public class AsServedScript implements PortionSizeScript {
	public static final String name = "as-served";

	public final AsServedDef servingImages;
	public final Option<AsServedDef> leftoversImages;
	
	private final PromptMessages messages = GWT.create(PromptMessages.class);

	public AsServedScript(AsServedDef servingImages, Option<AsServedDef> leftoversImages) {
		this.servingImages = servingImages;
		this.leftoversImages = leftoversImages;
	}

	@Override
	public Option<SimplePrompt<UpdateFunc>> nextPrompt(PMap<String, String> data, FoodData foodData) {
		boolean hasLeftoverImages = !leftoversImages.isEmpty();
		
		/*Logger log = Logger.getLogger("AsServedScript");
		
		log.info("Has leftovers: " + hasLeftoverImages);
		
		for (String k : data.keySet()) {
			log.info (k + " = " + data.get(k));
		}*/
		
		if (!data.containsKey("servingWeight")) {
			SimplePrompt<UpdateFunc> portionSizePrompt = 
					withBackLink(
							asServedPrompt(servingImages,	messages.asServed_servedLessButtonLabel(), messages.asServed_servedMoreButtonLabel(), messages.asServed_servedContinueButtonLabel(), "servingChoiceIndex", "servingImage", "servingWeight", defaultServingSizePrompt(foodData.localDescription.getOrElse(foodData.englishDescription)))
							 );
			
			if (!hasLeftoverImages)
				return Option.some(setAdditionalField(portionSizePrompt, "leftoversWeight", "0"));
			else
				return Option.some(portionSizePrompt);
		}
		else if (!data.containsKey("leftoversWeight") && hasLeftoverImages) {
			if (!data.containsKey("leftovers")) 
				return Option.some(withBackLink(
						yesNoPromptZeroField(SafeHtmlUtils.fromSafeConstant(messages.asServed_leftoversQuestionPromptText(SafeHtmlUtils.htmlEscape(foodData.localDescription.getOrElse(foodData.englishDescription).toLowerCase()))), messages.yesNoQuestion_defaultYesLabel(), messages.yesNoQuestion_defaultNoLabel(), "leftovers", "leftoversWeight")));
			else
				return Option.some(withBackLink(asServedPrompt(leftoversImages.getOrDie(), 
						messages.asServed_leftLessButtonLabel(), messages.asServed_leftMoreButtonLabel(), messages.asServed_leftContinueButtonLabel(), "leftoversChoiceIndex", "leftoversImage", "leftoversWeight", defaultLeftoversPrompt(foodData.localDescription.getOrElse(foodData.englishDescription)))));
		}
		else
			return done();
	}
}