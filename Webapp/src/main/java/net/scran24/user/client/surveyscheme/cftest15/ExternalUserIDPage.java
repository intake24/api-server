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

import net.scran24.common.client.WidgetFactory;
import net.scran24.user.client.survey.SimpleSurveyStageInterface;
import net.scran24.user.client.survey.SurveyStage;
import net.scran24.user.client.survey.flat.Survey;

import org.workcraft.gwt.shared.client.Callback1;
import org.workcraft.gwt.shared.client.Callback2;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.TextBox;

public class ExternalUserIDPage implements SurveyStage<Survey> {
	public static final String CUSTOM_DATA_KEY = "external-user-id";
		
	final private Survey data;

	public ExternalUserIDPage(Survey data) {
		this.data = data;
	}

	@Override
	public SimpleSurveyStageInterface getInterface(final Callback1<Survey> onComplete, Callback2<Survey, Boolean> onIntermediateStateChange) {
		
		final FlowPanel contents = new FlowPanel();
		
		final TextBox textBox = new TextBox();
				
		final Button startButton = WidgetFactory.createGreenButton("Continue", new ClickHandler() {
			public void onClick(ClickEvent event) {
				onComplete.call(data.withData(CUSTOM_DATA_KEY, textBox.getText()));
			}
		});
		
		contents.add(textBox);
		contents.add(startButton);
		
		
		return new SimpleSurveyStageInterface(contents);
	}
}