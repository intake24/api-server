/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey.prompts;

import net.scran24.common.client.WidgetFactory;

import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;

public class TextBoxQuestion extends Composite {
	final public TextBox textBox;
	final private FlowPanel warningDiv;
	
	public void clearWarning() {
		warningDiv.clear();
	}
	
	public void showWarning() {
		warningDiv.clear();
		warningDiv.add(new Label("Please answer this question before continuing"));
		warningDiv.getElement().scrollIntoView();
	}
	
	public TextBoxQuestion(SafeHtml promptText) {
		FlowPanel contents = new FlowPanel();
		contents.addStyleName("intake24-text-box-question");
		contents.add(WidgetFactory.createPromptPanel(promptText));
		
		warningDiv = new FlowPanel();
		warningDiv.addStyleName("intake24-text-box-question-warning");
		
		textBox = new TextBox();
		
		contents.add(warningDiv);
		contents.add(textBox);
		
		initWidget(contents);
	}	
}