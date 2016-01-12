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

import org.pcollections.client.PVector;
import org.workcraft.gwt.shared.client.Callback1;
import org.workcraft.gwt.shared.client.Callback2;
import org.workcraft.gwt.shared.client.Option;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;

public class MultipleChoiceRadioButtonQuestion implements SurveyStage<Survey> {
	final private SafeHtml questionText;
	final private PVector<String> options;
	final private String acceptText;
	final private String dataField;
	final private Survey state;
	final private Option<String> otherOption;

	public MultipleChoiceRadioButtonQuestion(final Survey state, final SafeHtml questionText, final String acceptText, PVector<String> options, String dataField,
			Option<String> otherOption) {
		this.state = state;
		this.questionText = questionText;
		this.acceptText = acceptText;
		this.options = options;
		this.dataField = dataField;
		this.otherOption = otherOption;
	}

	@Override
	public SimpleSurveyStageInterface getInterface(final Callback1<Survey> onComplete, Callback2<Survey, Boolean> onIntermediateStateChange) {
		final FlowPanel content = new FlowPanel();
		content.addStyleName("intake24-multiple-choice-question");
		content.addStyleName("intake24-survey-content-container");

		final RadioButtonQuestion choices = new RadioButtonQuestion(questionText, options, dataField, otherOption);

		Button accept = WidgetFactory.createGreenButton(acceptText, new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				Option<String> choice = choices.getChoice();

				if (choice.isEmpty()) {
					choices.showWarning();
					return;
				} else
					onComplete.call(state.withData(dataField, choice.getOrDie()));
			}
		});

		content.add(choices);
		content.add(accept);

		return new SimpleSurveyStageInterface(content);
	}
}