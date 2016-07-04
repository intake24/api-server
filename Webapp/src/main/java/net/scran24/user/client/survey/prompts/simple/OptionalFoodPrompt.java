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

package net.scran24.user.client.survey.prompts.simple;

import net.scran24.common.client.WidgetFactory;
import net.scran24.user.client.survey.flat.PromptUtil;
import net.scran24.user.client.survey.flat.SimplePrompt;
import net.scran24.user.client.survey.prompts.FoodBrowser;
import net.scran24.user.client.survey.prompts.SkipFoodHandler;
import net.scran24.user.shared.FoodData;

import org.workcraft.gwt.shared.client.Callback;
import org.workcraft.gwt.shared.client.Callback1;
import org.workcraft.gwt.shared.client.Option;
import org.workcraft.gwt.shared.client.Pair;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Panel;

public class OptionalFoodPrompt implements SimplePrompt<Option<String>> {
	private final OptionalFoodPromptDef def;
	private FlowPanel interf;
	private Panel buttonsPanel;
	private final String locale;

	public OptionalFoodPrompt(String locale, OptionalFoodPromptDef def) {
		this.locale = locale;
		this.def = def;
	}

	@Override
	public FlowPanel getInterface(final Callback1<Option<String>> onComplete) {
		final FlowPanel content = new FlowPanel();

		final Panel promptPanel = WidgetFactory.createPromptPanel(def.promptHtml);
		content.add(promptPanel);

		final FoodBrowser foodBrowser = new FoodBrowser(locale, new Callback1<FoodData>() {
			@Override
			public void call(FoodData result) {
				onComplete.call(Option.some(result.code));
			}
		}, new Callback1<String>() {
			@Override
			public void call(String code) {
				throw new RuntimeException("Special foods are not allowed as associated foods");
			}
		}, new Callback() {
			@Override
			public void call() {
				// FIXME: A quick hack to get rid of compiler errors -- this
				// prompt seems to be unused
				// fix this if this is actually used!
				onComplete.call(Option.<String> none());
			}
		}, Option.<SkipFoodHandler> none(), true, Option.<Pair<String, String>> none());

		Button no = WidgetFactory.createButton(def.noButtonText/* "No, I did not" */, new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				onComplete.call(Option.<String> none());
			}
		});

		Button yes = WidgetFactory.createButton(def.yesButtonText/* "Yes, I had some" */, new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				content.clear();
				PromptUtil.addBackLink(content);
				content.add(promptPanel);
				content.add(new HTMLPanel(def.foodChoicePromptHtml /*
																	 * SafeHtmlUtils
																	 * .
																	 * fromSafeConstant
																	 * (
																	 * "<p>Please select the specific type of this food that you've had:</p>"
																	 * )
																	 */));
				content.add(interf);

				content.add(foodBrowser);

				foodBrowser.browse(def.categoryCode, "?");
			}
		});

		buttonsPanel = WidgetFactory.createButtonsPanel(no, yes);
		content.add(buttonsPanel);

		interf = new FlowPanel();

		return content;
	}
}