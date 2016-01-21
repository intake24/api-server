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

package net.scran24.user.client.survey.prompts;

import java.util.List;

import net.scran24.common.client.AsyncRequest;
import net.scran24.common.client.AsyncRequestAuthHandler;
import net.scran24.common.client.LoadingPanel;
import net.scran24.common.client.WidgetFactory;
import net.scran24.user.client.services.FoodLookupServiceAsync;
import net.scran24.user.client.survey.SurveyStageInterface;
import net.scran24.user.client.survey.flat.FoodOperation;
import net.scran24.user.client.survey.flat.Prompt;
import net.scran24.user.client.survey.prompts.messages.PromptMessages;
import net.scran24.user.shared.FoodEntry;
import net.scran24.user.shared.FoodLink;
import net.scran24.user.shared.RawFood;

import org.pcollections.client.PVector;
import org.pcollections.client.TreePVector;
import org.workcraft.gwt.shared.client.Callback1;
import org.workcraft.gwt.shared.client.Function1;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;

public class SplitFoodPrompt implements Prompt<FoodEntry, FoodOperation> {
	private final FoodLookupServiceAsync lookupService = FoodLookupServiceAsync.Util.getInstance();
	private final RawFood food;
	private final PromptMessages messages = PromptMessages.Util.getInstance();
	
	private final static String currentLocale = LocaleInfo.getCurrentLocale().getLocaleName();

	public SplitFoodPrompt(final RawFood food) {
		this.food = food;
	}

	@Override
	public SurveyStageInterface getInterface(final Callback1<FoodOperation> onComplete,
			Callback1<Function1<FoodEntry, FoodEntry>> updateIntermediateState) {

		final FlowPanel content = new FlowPanel();
		content.add(new LoadingPanel(messages.foodLookup_loadingMessage(SafeHtmlUtils.htmlEscape(food.description))));

		final FoodOperation disableSplit = FoodOperation.updateRaw(new Function1<RawFood, RawFood>() {
			@Override
			public RawFood apply(RawFood argument) {
				return new RawFood(argument.link, argument.description, argument.flags.plus(RawFood.FLAG_DISABLE_SPLIT), argument.customData);
			}
		});

		AsyncRequestAuthHandler.execute(new AsyncRequest<List<String>>() {
			@Override
			public void execute(AsyncCallback<List<String>> callback) {
				lookupService.split(food.description, currentLocale, callback);
			}
		}, new AsyncCallback<List<String>>() {
			@Override
			public void onFailure(Throwable caught) {
				onComplete.call(disableSplit);
			}

			@Override
			public void onSuccess(final List<String> result) {
				if (result.size() == 1)
					onComplete.call(disableSplit);
				else {
										
					StringBuilder sb = new StringBuilder();
					
					sb.append("<p>");
					sb.append(messages.splitFood_promptText());
					sb.append("</p>");
					
					sb.append("<p>");
					sb.append(SafeHtmlUtils.htmlEscape(food.description()));
					sb.append("</p>");
					
					sb.append("<p>");
					sb.append(messages.splitFood_split());

					sb.append("<ul>");
					
					for (String s: result) {
						sb.append("<li>");
						sb.append(SafeHtmlUtils.htmlEscape(s));
						sb.append("</li>");
					}
					
					sb.append("</ul>");
					sb.append("</p>");
					
					sb.append("<p>");
					sb.append(messages.splitFood_keep());
					sb.append("</p>");
					sb.append("<p>");
					sb.append(messages.splitFood_separateSuggestion());
					sb.append("</p>");

					
					SafeHtml promptText = SafeHtmlUtils.fromSafeConstant(sb.toString());
							
					content.clear();

					content.add(WidgetFactory.createPromptPanel(promptText));

					Button yesButton = WidgetFactory.createButton(messages.splitFood_yesButtonLabel(), new ClickHandler() {
						@Override
						public void onClick(ClickEvent event) {
							PVector<FoodEntry> replacement = TreePVector.<FoodEntry> empty();

							for (String s : result)
								replacement = replacement.plus(new RawFood(FoodLink.newUnlinked(), s, food.flags.plus(RawFood.FLAG_DISABLE_SPLIT),
										food.customData));

							onComplete.call(new FoodOperation.Split(replacement));
						}
					});

					Button noButton = WidgetFactory.createButton(messages.splitFood_noButtonLabel(), new ClickHandler() {
						@Override
						public void onClick(ClickEvent event) {
							onComplete.call(disableSplit);
						}
					});

					content.add(WidgetFactory.createButtonsPanel(yesButton, noButton));
				}
			}
		});

		return new SurveyStageInterface.Aligned(content, HasHorizontalAlignment.ALIGN_LEFT, HasVerticalAlignment.ALIGN_TOP,
				SurveyStageInterface.DEFAULT_OPTIONS);
	}

	@Override
	public String toString() {
		return "Split foods prompt";
	}
}
