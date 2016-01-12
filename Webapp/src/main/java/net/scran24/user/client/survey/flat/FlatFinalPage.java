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

package net.scran24.user.client.survey.flat;

import java.util.List;

import net.scran24.common.client.AsyncRequest;
import net.scran24.common.client.AsyncRequestAuthHandler;
import net.scran24.common.client.CurrentUser;
import net.scran24.common.client.EmbeddedData;
import net.scran24.common.client.LoadingPanel;
import net.scran24.common.client.WidgetFactory;
import net.scran24.user.client.services.SurveyProcessingServiceAsync;
import net.scran24.user.client.survey.SimpleSurveyStageInterface;
import net.scran24.user.client.survey.SurveyMessages;
import net.scran24.user.client.survey.SurveyStage;
import net.scran24.user.client.survey.prompts.messages.PromptMessages;
import net.scran24.user.shared.CompletedSurvey;

import org.workcraft.gwt.shared.client.Callback1;
import org.workcraft.gwt.shared.client.Callback2;
import org.workcraft.gwt.shared.client.Option;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.RequestTimeoutException;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;

public class FlatFinalPage implements SurveyStage<Survey> {
	public final SurveyProcessingServiceAsync processingService = SurveyProcessingServiceAsync.Util.getInstance(EmbeddedData.surveyId());
	private final static PromptMessages messages = PromptMessages.Util.getInstance();
	private final static SurveyMessages surveyMessages = SurveyMessages.Util.getInstance();

	private final String finalPageHtml;
	private final Survey data;
	private final List<String> log;

	public FlatFinalPage(String finalPageHtml, Survey data, List<String> log) {
		this.finalPageHtml = finalPageHtml;
		this.data = data;
		this.log = log;		
	}

	@Override
	public SimpleSurveyStageInterface getInterface(Callback1<Survey> onComplete, Callback2<Survey, Boolean> onIntermediateStateChange) {
		final CompletedSurvey finalData = data.finalise(log);
		final FlowPanel contents = new FlowPanel();
		contents.addStyleName("intake24-survey-content-container");
		
		contents.add(new LoadingPanel(messages.submitPage_loadingMessage()));

		AsyncRequestAuthHandler.execute(new AsyncRequest<Void>() {
			@Override
			public void execute(AsyncCallback<Void> callback) {
				processingService.submit(finalData, callback);
			}
		}, new AsyncCallback<Void>() {
			@Override
			public void onFailure(final Throwable caught) {
				contents.clear();
				
				caught.printStackTrace();

				if (caught instanceof RequestTimeoutException) {
					final AsyncCallback<Void> outer = this;

					contents.add(new HTMLPanel(SafeHtmlUtils.fromSafeConstant(messages.submitPage_timeout())));

					contents.add(WidgetFactory.createGreenButton(messages.submitPage_tryAgainButton(), new ClickHandler() {
						@Override
						public void onClick(ClickEvent event) {
							processingService.submit(finalData, outer);
						}
					}));
				} else {
					contents.add(new HTMLPanel(SafeHtmlUtils.fromSafeConstant(messages.submitPage_error())));
				}
				
				contents.add(new HTMLPanel(finalPageHtml));
			}

			@Override
			public void onSuccess(Void result) {
				contents.clear();
				contents.add(new HTMLPanel(SafeHtmlUtils.fromSafeConstant(messages.submitPage_success())));

                CurrentUser.userInfo.surveyParameters.surveyMonkeyUrl.accept(new Option.SideEffectVisitor<String>() {
                    @Override
                    public void visitSome(final String url) {

                        FlowPanel surveyMonkeyDiv = new FlowPanel();

                        surveyMonkeyDiv.add(WidgetFactory.createGreenButton(surveyMessages.finalPage_continueToSurveyMonkey(), new ClickHandler() {
                            @Override
                            public void onClick(ClickEvent clickEvent) {
                                Window.Location.replace(url.replace("[intake24_username_value]", CurrentUser.userInfo.userName));
                            }
                        }));

                        contents.add(surveyMonkeyDiv);
                    }

                    @Override
                    public void visitNone() {

                    }
                });

				contents.add(new HTMLPanel(finalPageHtml));
				StateManagerUtil.clearLatestState(CurrentUser.userInfo.userName);
			}
		});

		return new SimpleSurveyStageInterface(contents);
	}
}