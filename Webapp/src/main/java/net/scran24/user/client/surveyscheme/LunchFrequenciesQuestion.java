/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.surveyscheme;

import net.scran24.common.client.WidgetFactory;
import net.scran24.user.client.survey.SimpleSurveyStageInterface;
import net.scran24.user.client.survey.SurveyStage;
import net.scran24.user.client.survey.flat.Survey;
import net.scran24.user.client.survey.prompts.RadioButtonQuestion;

import org.pcollections.client.PMap;
import org.pcollections.client.PVector;
import org.workcraft.gwt.shared.client.Callback1;
import org.workcraft.gwt.shared.client.Callback2;
import org.workcraft.gwt.shared.client.Option;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;

public class LunchFrequenciesQuestion implements SurveyStage<Survey> {
	final private Survey state;
	final private PVector<String> frequencyOptions;

	public LunchFrequenciesQuestion(final Survey state, PVector<String> frequencyOptions) {
		this.state = state;
		this.frequencyOptions = frequencyOptions;
	}

	@Override
	public SimpleSurveyStageInterface getInterface(final Callback1<Survey> onComplete, Callback2<Survey, Boolean> onIntermediateStateChange) {
		final FlowPanel content = new FlowPanel();
		content.addStyleName("intake24-survey-content-container");

		final RadioButtonQuestion shopFreq = new RadioButtonQuestion(
				SafeHtmlUtils.fromSafeConstant("<p>In a normal school week how often do you go out to the shops for <strong>lunch</strong>?</p>"),
				frequencyOptions, "shopFreq", Option.<String>none());
		final RadioButtonQuestion packFreq = new RadioButtonQuestion(
				SafeHtmlUtils.fromSafeConstant("<p>In a normal school week how often do you bring in a packed <strong>lunch</strong> from home?</p>"),
				frequencyOptions, "packFreq", Option.<String>none());
		final RadioButtonQuestion schoolLunchFreq = new RadioButtonQuestion(
				SafeHtmlUtils.fromSafeConstant("<p>In a normal school week how often do you have a school <strong>lunch</strong>?</p>"), frequencyOptions,
				"schoolLunchFreq", Option.<String>none());
		final RadioButtonQuestion homeFreq = new RadioButtonQuestion(
				SafeHtmlUtils.fromSafeConstant("<p>In a normal school week how often do you go home/to a friend's house for <strong>lunch</strong>?</p>"),
				frequencyOptions, "homeFreq", Option.<String>none());
		final RadioButtonQuestion skipFreq = new RadioButtonQuestion(
				SafeHtmlUtils.fromSafeConstant("<p>In a normal school week how often do you skip <strong>lunch</strong>?</p>"), frequencyOptions, "skipFreq",
				Option.<String>none());
		final RadioButtonQuestion workFreq = new RadioButtonQuestion(
				SafeHtmlUtils.fromSafeConstant("<p>In a normal school week how often do you work through <strong>lunch</strong>?</p>"),
				frequencyOptions, "workFreq", Option.<String>none());

		content.add(shopFreq);
		content.add(packFreq);
		content.add(schoolLunchFreq);
		content.add(homeFreq);
		content.add(skipFreq);
		content.add(workFreq);

		final Button accept = WidgetFactory.createButton("Continue");

		accept.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				for (RadioButtonQuestion q : new RadioButtonQuestion[] { shopFreq, packFreq, schoolLunchFreq, homeFreq, skipFreq, workFreq }) 
					if (q.getChoice().isEmpty()) {
						q.showWarning();
						return;
					}
					else
						q.clearWarning();
				

				PMap<String, String> data = state.customData;
				data = data.plus("shopFreq", shopFreq.getChoice().getOrDie())
				.plus("packFreq", packFreq.getChoice().getOrDie())
				.plus("schoolLunchFreq", schoolLunchFreq.getChoice().getOrDie())
				.plus("homeFreq", homeFreq.getChoice().getOrDie())
				.plus("skipFreq", skipFreq.getChoice().getOrDie())
				.plus("workFreq", workFreq.getChoice().getOrDie());
				
				accept.setEnabled(false);
				onComplete.call(state.withData(data));
			}
		});

		content.add(WidgetFactory.createButtonsPanel(accept));

		return new SimpleSurveyStageInterface(content);
	}
}