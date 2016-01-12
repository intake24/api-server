/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey.prompts.simple;

import java.util.List;

import net.scran24.common.client.WidgetFactory;
import net.scran24.user.client.survey.flat.SimplePrompt;
import net.scran24.user.client.survey.portionsize.experimental.StandardUnitDef;
import net.scran24.user.client.survey.prompts.RadioButtonQuestion;

import org.pcollections.client.PVector;
import org.pcollections.client.TreePVector;
import org.workcraft.gwt.shared.client.Callback1;
import org.workcraft.gwt.shared.client.Function1;
import org.workcraft.gwt.shared.client.Option;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;

public class StandardUnitPrompt implements SimplePrompt<Integer> {
	final private SafeHtml promptText;
	final private List<StandardUnitDef> units;
	final private String acceptText;
	private final Function1<StandardUnitDef, String> label;
	
	public StandardUnitPrompt(SafeHtml promptText, String acceptText, List<StandardUnitDef> units, Function1<StandardUnitDef, String> label) {
		this.promptText = promptText;
		this.acceptText = acceptText;
		this.units = units;
		this.label = label;
	}

	@Override
	public FlowPanel getInterface(final Callback1<Integer> onComplete) {
		final FlowPanel content = new FlowPanel();
		content.addStyleName("intake24-multiple-choice-question");
		
		PVector<String> choices = TreePVector.<String>empty();
		
		for (StandardUnitDef def : units)
			choices = choices.plus (label.apply(def));
		
		final RadioButtonQuestion choice = new RadioButtonQuestion(promptText, choices, "standard-unit-choice", Option.<String>none());
		
		choice.selectFirst();

		Button accept = WidgetFactory.createGreenButton(acceptText, new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
						onComplete.call(choice.getChoiceIndex().getOrDie());
			}
		});

		content.add(choice);
		content.add(WidgetFactory.createButtonsPanel(accept));

		return content;
	}
}