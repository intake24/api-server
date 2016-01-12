/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey.prompts.simple;

import net.scran24.common.client.WidgetFactory;
import net.scran24.user.client.ShepherdTour;
import net.scran24.user.client.survey.flat.SimplePrompt;
import net.scran24.user.client.survey.prompts.RadioButtonQuestion;
import net.scran24.user.client.survey.prompts.messages.HelpMessages;

import org.pcollections.client.PVector;
import org.pcollections.client.TreePVector;
import org.workcraft.gwt.shared.client.Callback1;
import org.workcraft.gwt.shared.client.Option;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;

public class RadioButtonPrompt implements SimplePrompt<String> {
	private final static HelpMessages helpMessages = HelpMessages.Util.getInstance();

	private final static PVector<ShepherdTour.Step> tour = TreePVector
			.<ShepherdTour.Step> empty()
			.plus(new ShepherdTour.Step("prompt", "#intake24-radio-button-question", helpMessages.multipleChoice_questionTitle(), helpMessages.multipleChoice_questionDescription()))
			.plus(new ShepherdTour.Step("radioButtons", "#intake24-radio-button-choices", helpMessages.multipleChoice_choicesTitle(), helpMessages.multipleChoice_choicesDescription(), false))
			.plus(new ShepherdTour.Step("continueButton", "#intake24-radio-button-continue-button", helpMessages.multipleChoice_continueButtonTitle(), helpMessages.multipleChoice_continueButtonDescription(), false));

	private final SafeHtml promptText;
	private final PVector<String> options;
	private final String continueLabel;
	private final String buttonGroupId;
	private final Option<String> otherOption;
	private final String promptType;

	public RadioButtonPrompt(SafeHtml promptText, String promptType, PVector<String> options, String continueLabel, String buttonGroupId, Option<String> otherOption) {
		this.promptText = promptText;
		this.options = options;
		this.continueLabel = continueLabel;
		this.buttonGroupId = buttonGroupId;
		this.otherOption = otherOption;
		this.promptType = promptType;
	}

	@Override
	public FlowPanel getInterface(final Callback1<String> onComplete) {
		final FlowPanel content = new FlowPanel();

		final RadioButtonQuestion radioButtonBlock = new RadioButtonQuestion(promptText, options, buttonGroupId, otherOption);
		
		Button helpButton = ShepherdTour.createTourButton(tour, promptType);
		helpButton.getElement().getStyle().setFloat(com.google.gwt.dom.client.Style.Float.RIGHT);
		radioButtonBlock.promptPanel.insert(helpButton, 0);

		content.add(radioButtonBlock);

		Button continueButton = WidgetFactory.createButton(continueLabel, new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				Option<String> choice = radioButtonBlock.getChoice();

				if (choice.isEmpty()) {
					radioButtonBlock.showWarning();
					return;
				} else
					onComplete.call(choice.getOrDie());
			}
		});

		continueButton.getElement().setId("intake24-radio-button-continue-button");

		content.add(continueButton);

		ShepherdTour.makeShepherdTarget(radioButtonBlock.promptPanel, radioButtonBlock.radioButtons, continueButton);

		return content;
	}
}