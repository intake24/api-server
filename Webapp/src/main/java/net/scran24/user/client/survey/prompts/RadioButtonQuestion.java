/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey.prompts;

import net.scran24.common.client.UnorderedList;
import net.scran24.common.client.WidgetFactory;
import net.scran24.user.client.ShepherdTour;
import net.scran24.user.client.survey.prompts.messages.HelpMessages;

import org.pcollections.client.PVector;
import org.workcraft.gwt.shared.client.Option;

import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public class RadioButtonQuestion extends Composite {
	final public PVector<String> choices;
	
	final private RadioButton[] optionButtons; 
	final private Option<String> otherOptionName;
	final private FlowPanel warningDiv;
	final private FlowPanel contents;
	
	private RadioButton otherOption;
	private TextBox otherBox;
	
	public final FlowPanel promptPanel;
	public final Widget radioButtons;
	
	public Option<String> getChoice() {
		for (int i = 0; i < choices.size(); i++)
			if (optionButtons[i].getValue())
				return Option.some(choices.get(i));
		
		if (!otherOptionName.isEmpty() && otherOption.getValue())
			return Option.some(otherBox.getText());
		
		return Option.none(); 
	}
	
	public void selectFirst() {
		optionButtons[0].setValue(true);
	}
	
	public Option<Integer> getChoiceIndex() {
		for (int i = 0; i < choices.size(); i++)
			if (optionButtons[i].getValue())
				return Option.some(i);
		return Option.none();
	}
	
	public void clearWarning() {
		warningDiv.clear();
	}
	
	public void showWarning() {
		warningDiv.clear();
		warningDiv.add(new Label("Please answer this question before continuing"));
		contents.getElement().scrollIntoView();
	}
	
	public RadioButtonQuestion(SafeHtml promptText, PVector<String> choices, String groupId, Option<String> otherOptionName) {
		this.choices = choices;
		this.otherOptionName = otherOptionName;
		
		UnorderedList<Widget> choiceList = new UnorderedList<Widget>();
		choiceList.getElement().setId("intake24-radio-button-choices");
		
		optionButtons = new RadioButton[choices.size()];
		
		for (int i = 0; i < choices.size(); i++) {
			optionButtons[i] = new RadioButton(groupId, SafeHtmlUtils.fromString(choices.get(i)));
			choiceList.addItem(optionButtons[i]);
		}
		
		radioButtons = choiceList;
		
		if (!otherOptionName.isEmpty()) {
			FlowPanel otherPanel = new FlowPanel();
			otherOption = new RadioButton(groupId, otherOptionName.getOrDie() + ": ");
			otherPanel.add(otherOption);
			otherBox = new TextBox();
			otherPanel.add(otherBox);
			choiceList.addItem(otherPanel);
			
			otherBox.addFocusHandler(new FocusHandler() {
				@Override
				public void onFocus(FocusEvent event) {
					otherOption.setValue(true);
				}
			});
		}
		
		contents = new FlowPanel();
		contents.addStyleName("intake24-radio-button-question");
		
		promptPanel = WidgetFactory.createPromptPanel(promptText);
		promptPanel.getElement().setId("intake24-radio-button-question");
		contents.add(promptPanel);
		
		warningDiv = new FlowPanel();
		warningDiv.addStyleName("intake24-radio-button-question-warning");
		
		contents.add(warningDiv);
		contents.add(choiceList);
		
		initWidget(contents);
	}	
}