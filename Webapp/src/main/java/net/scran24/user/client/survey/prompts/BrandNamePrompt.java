/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey.prompts;


import java.util.List;

import net.scran24.common.client.WidgetFactory;
import net.scran24.user.client.ShepherdTour;
import net.scran24.user.client.survey.SurveyStageInterface;
import net.scran24.user.client.survey.flat.FoodOperation;
import net.scran24.user.client.survey.flat.Prompt;
import net.scran24.user.client.survey.prompts.messages.HelpMessages;
import net.scran24.user.client.survey.prompts.messages.PromptMessages;
import net.scran24.user.shared.EncodedFood;
import net.scran24.user.shared.FoodEntry;

import org.pcollections.client.PVector;
import org.pcollections.client.TreePVector;
import org.workcraft.gwt.shared.client.Callback1;
import org.workcraft.gwt.shared.client.Function1;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.VerticalPanel;

public class BrandNamePrompt implements Prompt<FoodEntry, FoodOperation> {
	private final static PromptMessages messages = PromptMessages.Util.getInstance();
	private final static HelpMessages helpMessages = HelpMessages.Util.getInstance(); 
	
	private final String description;
	private final List<String> brandNames;
	
	private String choice;
	
	private final static PVector<ShepherdTour.Step> tour = TreePVector
			.<ShepherdTour.Step> empty()
			.plus(new ShepherdTour.Step("brandNameList", "#intake24-brand-choice-panel", helpMessages.brandName_brandListTitle(), helpMessages.brandName_brandListDescription()))
			.plus(new ShepherdTour.Step("cont", "#intake24-brand-continue-button", helpMessages.brandName_continueButtonTitle(), helpMessages.brandName_continueButtonDescription()));
	
	public BrandNamePrompt(String description, List<String> brandNames) {
		this.description = description;
		this.brandNames = brandNames;
	}

	public SurveyStageInterface getInterface(final Callback1<FoodOperation> onComplete,
			final Callback1<Function1<FoodEntry, FoodEntry>> onIntermediateStateChange) {

		final FlowPanel content = new FlowPanel();
		
		FlowPanel promptPanel = WidgetFactory.createPromptPanel(SafeHtmlUtils.fromSafeConstant(messages.brandName_promptText(SafeHtmlUtils.htmlEscape(description.toLowerCase()))), 
				ShepherdTour.createTourButton(tour, BrandNamePrompt.class.getSimpleName()));
		ShepherdTour.makeShepherdTarget(promptPanel);
		
		content.add(promptPanel);
		
		final Button contButton = WidgetFactory.createGreenButton (messages.brandName_continueButtonLabel(), new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				onComplete.call(FoodOperation.updateEncoded(new Function1<EncodedFood, EncodedFood>(){
					@Override
					public EncodedFood apply(EncodedFood argument) {
						return argument.withBrand(choice);
					}
				}));
			}
		});
		
		contButton.setEnabled(false);
		contButton.getElement().setId("intake24-brand-continue-button");
		ShepherdTour.makeShepherdTarget(contButton);
		
		VerticalPanel panel = new VerticalPanel();
		
		for (final String name: brandNames) {
			RadioButton btn = new RadioButton("brand", name);
			btn.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
				@Override
				public void onValueChange(ValueChangeEvent<Boolean> event) {
					if (event.getValue())
						choice = name;
					contButton.setEnabled(true);
				}
			});
			panel.add(btn);
		}

		panel.setSpacing(4);
		panel.addStyleName("scran24-brand-name-choice-panel");
		panel.getElement().setId("intake24-brand-choice-panel");
		ShepherdTour.makeShepherdTarget(panel);
		
		content.add(panel);
		content.add(WidgetFactory.createButtonsPanel(contButton));

		return new SurveyStageInterface.Aligned(content, HasHorizontalAlignment.ALIGN_LEFT, HasVerticalAlignment.ALIGN_TOP,
				SurveyStageInterface.DEFAULT_OPTIONS);
	}

	@Override
	public String toString() {
		return "Brand name prompt";
	}
}