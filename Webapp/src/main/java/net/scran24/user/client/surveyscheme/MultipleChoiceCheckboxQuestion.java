/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.surveyscheme;

import java.util.ArrayList;

import net.scran24.common.client.WidgetFactory;
import net.scran24.user.client.survey.SimpleSurveyStageInterface;
import net.scran24.user.client.survey.SurveyStage;
import net.scran24.user.client.survey.flat.Survey;

import org.pcollections.client.PVector;
import org.workcraft.gwt.shared.client.Callback1;
import org.workcraft.gwt.shared.client.Callback2;
import org.workcraft.gwt.shared.client.Option;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.TextBox;

public class MultipleChoiceCheckboxQuestion implements SurveyStage<Survey> {
	final private SafeHtml questionText;
	final private PVector<String> options;
	final private String acceptText;
	final private String dataField;
	final private Survey state;
	final private Option<String> otherOptionName;

	private CheckBox otherOption;
	private TextBox otherBox;

	public MultipleChoiceCheckboxQuestion(final Survey state, final SafeHtml questionText, final String acceptText, PVector<String> options,
			String dataField, Option<String> otherOptionName) {
		this.state = state;
		this.questionText = questionText;
		this.acceptText = acceptText;
		this.options = options;
		this.dataField = dataField;
		this.otherOptionName = otherOptionName;
	}

	@Override
	public SimpleSurveyStageInterface getInterface(final Callback1<Survey> onComplete, Callback2<Survey, Boolean> onIntermediateStateChange) {
		final FlowPanel content = new FlowPanel();
		content.addStyleName("intake24-multiple-choice-question");
		content.addStyleName("intake24-survey-content-container");

		content.add(WidgetFactory.createPromptPanel(questionText));

		FlowPanel checkboxesDiv = new FlowPanel();
		checkboxesDiv.addStyleName("scran24-ready-meals-checkboxes-block");

		final ArrayList<CheckBox> checkBoxes = new ArrayList<CheckBox>();

		for (String option : options) {
			FlowPanel rowDiv = new FlowPanel();
			CheckBox checkBox = new CheckBox(SafeHtmlUtils.htmlEscape(option));
			checkBox.setFormValue(option);
			checkBox.addStyleName("scran24-ready-meals-checkbox");
			checkBoxes.add(checkBox);
			rowDiv.add(checkBox);
			checkboxesDiv.add(rowDiv);
		}

		if (!otherOptionName.isEmpty()) {
			FlowPanel otherPanel = new FlowPanel();
			otherOption = new CheckBox(otherOptionName.getOrDie() + ": ");
			otherPanel.add(otherOption);
			otherBox = new TextBox();
			otherPanel.add(otherBox);
			checkboxesDiv.add(otherPanel);

			otherBox.addFocusHandler(new FocusHandler() {
				@Override
				public void onFocus(FocusEvent event) {
					otherOption.setValue(true);
				}
			});
		}

		content.add(checkboxesDiv);

		Button accept = WidgetFactory.createGreenButton(acceptText, new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {

				StringBuilder value = new StringBuilder();
				boolean first = true;

				for (CheckBox checkBox : checkBoxes)
					if (checkBox.getValue()) {
						if (first)
							first = false;
						else
							value.append(", ");

						value.append(checkBox.getFormValue());
					}

				if (!otherOptionName.isEmpty()) {
					if (otherOption.getValue()) {
						if (!first)
							value.append(", ");
						value.append(otherBox.getText());
					}
				}

				onComplete.call(state.withData(dataField, value.toString()));
			}
		});

		content.add(checkboxesDiv);
		content.add(accept);

		return new SimpleSurveyStageInterface(content);
	}
}