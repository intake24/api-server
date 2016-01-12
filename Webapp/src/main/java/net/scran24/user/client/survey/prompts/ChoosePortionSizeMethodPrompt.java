/*
This file is part of Intake24.

Copyright 2015, 2016 Newcastle University.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

This file is based on Intake24 v1.0.

© Crown copyright, 2012, 2013, 2014

Licensed under the Open Government Licence 3.0: 

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey.prompts;

import net.scran24.common.client.WidgetFactory;
import net.scran24.user.client.ShepherdTour;
import net.scran24.user.client.survey.SurveyStageInterface;
import net.scran24.user.client.survey.flat.FoodOperation;
import net.scran24.user.client.survey.flat.Prompt;
import net.scran24.user.client.survey.flat.PromptUtil;
import net.scran24.user.client.survey.prompts.messages.HelpMessages;
import net.scran24.user.client.survey.prompts.messages.PromptMessages;
import net.scran24.user.shared.EncodedFood;
import net.scran24.user.shared.FoodEntry;
import net.scran24.user.shared.lookup.PortionSizeMethod;

import org.pcollections.client.PVector;
import org.pcollections.client.TreePVector;
import org.workcraft.gwt.shared.client.Callback1;
import org.workcraft.gwt.shared.client.Function1;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;

public class ChoosePortionSizeMethodPrompt implements Prompt<FoodEntry, FoodOperation> {
	private final EncodedFood food;
	private final static PromptMessages messages = PromptMessages.Util.getInstance();
	private final static HelpMessages helpMessages = HelpMessages.Util.getInstance(); 
	
	private final static PVector<ShepherdTour.Step> tour = TreePVector.<ShepherdTour.Step>empty()
			.plus(new ShepherdTour.Step("panel", "#intake24-choose-portion-panel", helpMessages.chooseMethod_panelTitle(), helpMessages.chooseMethod_panelDescription(), false));


	public ChoosePortionSizeMethodPrompt(final EncodedFood food) {
		this.food = food;
	}
	
	@Override
	public SurveyStageInterface getInterface(final Callback1<FoodOperation> onComplete, Callback1<Function1<FoodEntry, FoodEntry>> updateIntermediateState) {
		final FlowPanel content = new FlowPanel();
		content.addStyleName("intake24-choose-portion-method-prompt");
		
		PromptUtil.addBackLink(content);
		
		final HTMLPanel header = new HTMLPanel("h2", food.description());
		
		content.add(header);
				
		FlowPanel promptPanel = WidgetFactory.createPromptPanel(SafeHtmlUtils.fromSafeConstant(messages.choosePortionMethod_promptText(food.description())), ShepherdTour.createTourButton(tour, ChoosePortionSizeMethodPrompt.class.getSimpleName()));
		content.add(promptPanel);
		ShepherdTour.makeShepherdTarget(promptPanel);

		final FlowPanel methodPanel = new FlowPanel();
		methodPanel.getElement().setId("intake24-choose-portion-panel");
		ShepherdTour.makeShepherdTarget(methodPanel);
		
		int index = 0;
		
		for (final PortionSizeMethod m: food.data.portionSizeMethods) {
			
			Image img = new Image(m.imageUrl);
			
			final int indexClosure = index; 
			index++;
			
			ClickHandler clickHandler = new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					onComplete.call(FoodOperation.updateEncoded(new Function1<EncodedFood, EncodedFood>() {
						@Override
						public EncodedFood apply(EncodedFood argument) {
							return argument.withSelectedPortionSizeMethod(indexClosure);
						}
					}));
				}
			};
			
			img.addClickHandler(clickHandler);

			img.addStyleName("intake24-choose-portion-image");
			
			FlowPanel container = new FlowPanel();
			container.addStyleName("intake24-choose-portion-container");
						
			container.add(img);
			
			Label label = new Label(m.description.substring(0, 1).toUpperCase() + m.description.substring(1));
			label.addStyleName("intake24-choose-portion-label");
			label.addClickHandler(clickHandler);
			
			container.add(label);
			
			methodPanel.add(container);
		}
		
		content.add(methodPanel);
		
		return new SurveyStageInterface.Aligned(content, HasHorizontalAlignment.ALIGN_LEFT,
				HasVerticalAlignment.ALIGN_TOP, SurveyStageInterface.DEFAULT_OPTIONS);
	}
	
	@Override
	public String toString() {
		return "Choose portion size method prompt";
	}
}