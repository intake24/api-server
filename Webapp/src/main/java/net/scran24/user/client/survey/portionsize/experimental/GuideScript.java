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
import static net.scran24.user.client.survey.portionsize.experimental.PortionSizeScriptUtil.guidePrompt;
import static net.scran24.user.client.survey.portionsize.experimental.PortionSizeScriptUtil.quantityPrompt;
import net.scran24.user.client.survey.flat.PromptUtil;
import net.scran24.user.client.survey.flat.SimplePrompt;
import net.scran24.user.client.survey.prompts.messages.PromptMessages;
import net.scran24.user.shared.FoodData;
import net.scran24.user.shared.lookup.GuideDef;

import org.pcollections.client.PMap;
import org.workcraft.gwt.imagemap.shared.ImageMapDefinition;
import org.workcraft.gwt.shared.client.Function1;
import org.workcraft.gwt.shared.client.Option;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.Window;

public class GuideScript implements PortionSizeScript {
	public static final String name = "guide-image";

	public final GuideDef guideDef;
	public final ImageMapDefinition imageMap;

	private final PromptMessages messages = GWT.create(PromptMessages.class);
	
	public GuideScript(GuideDef guideDef, ImageMapDefinition imageMap) {
		this.guideDef = guideDef;
		this.imageMap = imageMap;
	}

	@Override
	public Option<SimplePrompt<UpdateFunc>> nextPrompt(PMap<String, String> data, FoodData foodData) {
		if (!data.containsKey("objectWeight")) {
			return Option.some(PromptUtil.map(
					withBackLink(
							guidePrompt(
									SafeHtmlUtils.fromSafeConstant(messages.guide_choicePromptText()),
									imageMap, "objectIndex", "imageUrl")),
					new Function1<UpdateFunc, UpdateFunc>() {
						@Override
						public UpdateFunc apply(final UpdateFunc f) {
							return new UpdateFunc() {
								@Override
								public PMap<String, String> apply(PMap<String, String> argument) {
									PMap<String, String> a = f.apply(argument);
									return a.plus("objectWeight", Double.toString(guideDef.weights.get(Integer.parseInt(a.get("objectIndex")))));
								}
							};
						}
					}));

		} else if (!data.containsKey("quantity")) {
			
			return Option.some(withBackLink(PromptUtil.map(
					quantityPrompt(SafeHtmlUtils.fromSafeConstant(messages.guide_quantityPromptText()), messages.guide_quantityContinueButtonLabel(), "quantity"),
					new Function1<UpdateFunc, UpdateFunc>() {
						@Override
						public UpdateFunc apply(final UpdateFunc f) {
							return new UpdateFunc() {
								@Override
								public PMap<String, String> apply(PMap<String, String> argument) {
									PMap<String, String> a = f.apply(argument);
									return a.plus("servingWeight", Double.toString(Double.parseDouble(a.get("objectWeight")) * Double.parseDouble(a.get("quantity"))))
											.plus("leftoversWeight", Double.toString(0));
								}
							};
						}
					})));
		} else
			return done();
	}
}