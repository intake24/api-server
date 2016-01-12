/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey.prompts.simple;

import net.scran24.user.client.survey.flat.SimplePrompt;
import net.scran24.user.client.survey.prompts.YesNoQuestion;

import org.workcraft.gwt.shared.client.Callback1;

import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Panel;

public class YesNoPrompt implements SimplePrompt<Boolean> {
	private final SafeHtml promptText;
	private final String yesText;
	private final String noText;

	public YesNoPrompt(SafeHtml promptText, String yesText, String noText) {
		this.promptText = promptText;
		this.yesText = yesText;
		this.noText = noText;
	}

	@Override
	public FlowPanel getInterface(final Callback1<Boolean> onComplete) {
		final FlowPanel content = new FlowPanel();

		content.add(new YesNoQuestion(promptText, yesText, noText, new YesNoQuestion.ResultHandler() {
			@Override
			public void handleYes() {
				onComplete.call(true);
			}

			@Override
			public void handleNo() {
				onComplete.call(false);
			}
		}));
		
		return content;
	}
}