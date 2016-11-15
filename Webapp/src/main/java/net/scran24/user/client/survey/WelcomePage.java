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

import net.scran24.common.client.WidgetFactory;
import net.scran24.common.client.survey.TutorialVideo;
import net.scran24.user.client.survey.flat.Survey;

import org.workcraft.gwt.shared.client.Callback1;
import org.workcraft.gwt.shared.client.Callback2;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;

public class WelcomePage implements SurveyStage<Survey> {
  public static final String FLAG_WELCOME_PAGE_SHOWN = "welcome-page-shown";

  private static final SurveyMessages surveyMessages = SurveyMessages.Util.getInstance();

  private final Survey initialData;
  private final String welcomeHtml;

  public WelcomePage(String welcomeHtml, Survey initialData) {
    this.welcomeHtml = welcomeHtml;
    this.initialData = initialData;
  }

  @Override
  public SimpleSurveyStageInterface getInterface(final Callback1<Survey> onComplete,
      Callback2<Survey, Boolean> onIntermediateStateChange) {
    final Button startButton = WidgetFactory.createGreenButton(surveyMessages.welcomePage_ready(), new ClickHandler() {
      public void onClick(ClickEvent event) {
        onComplete.call(initialData.withFlag(FLAG_WELCOME_PAGE_SHOWN));
      }
    });

    HTMLPanel tutorialVideo = new HTMLPanel(SafeHtmlUtils.fromSafeConstant(TutorialVideo.embedHTML));

    FlowPanel contents = new FlowPanel();
    contents.getElement().addClassName("intake24-survey-content-container");
    contents.add(tutorialVideo);
    HTMLPanel htmlPanel = new HTMLPanel(SafeHtmlUtils.fromSafeConstant(welcomeHtml));

    contents.add(htmlPanel);
    contents.add(startButton);

    return new SimpleSurveyStageInterface(contents);
  }
}
