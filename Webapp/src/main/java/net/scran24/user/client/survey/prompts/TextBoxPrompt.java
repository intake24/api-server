/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey.prompts;


import net.scran24.common.client.WidgetFactory;
import net.scran24.user.client.survey.SurveyStageInterface;
import net.scran24.user.client.survey.flat.FoodOperation;
import net.scran24.user.client.survey.flat.Prompt;
import net.scran24.user.shared.FoodEntry;

import org.workcraft.gwt.shared.client.Callback1;
import org.workcraft.gwt.shared.client.Function1;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Widget;

@Deprecated
/** 
 * Use standardised portion size instead 
 * */ 
public class TextBoxPrompt implements Prompt<FoodEntry, FoodOperation> {
	private final TextBoxPromptDef def;
	private Function1<Callback1<FoodOperation>, Widget[]> createTopWidgets;

	public TextBoxPrompt(TextBoxPromptDef def, Function1<Callback1<FoodOperation>, Widget[]> createTopWidgets) {
		this.def = def;
		this.createTopWidgets = createTopWidgets;
	}

	@Override
	public SurveyStageInterface getInterface(final Callback1<FoodOperation> onComplete,	final Callback1<Function1<FoodEntry, FoodEntry>> onIntermediateStateChange) {
		final FlowPanel content = new FlowPanel();
		
		content.add(WidgetFactory.createTopPanel(createTopWidgets.apply(onComplete)));
		content.add(WidgetFactory.createPromptPanel(def.description));
		
		final TextArea text = new TextArea();
		text.getElement().getStyle().setWidth(400, Unit.PX);
		text.getElement().getStyle().setHeight(200, Unit.PX);
		text.getElement().getStyle().setMarginBottom(15, Unit.PX);
		content.add(text);
		
		Button contButton = WidgetFactory.createGreenButton("Continue", new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				onComplete.call(def.f.apply(text.getText()));
			}
		});
		
		content.add(WidgetFactory.createButtonsPanel(contButton));

		return new SurveyStageInterface.Aligned(content, HasHorizontalAlignment.ALIGN_LEFT, HasVerticalAlignment.ALIGN_TOP, SurveyStageInterface.DEFAULT_OPTIONS);
	}

	@Override
	public String toString() {
		return "Text box portion size prompt";
	}
}