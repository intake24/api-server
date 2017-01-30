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

package net.scran24.user.client.survey.prompts.simple;

import java.util.ArrayList;

import net.scran24.common.client.WidgetFactory;
import net.scran24.common.client.widgets.LabelledCounter;
import net.scran24.user.client.ShepherdTour;
import net.scran24.user.client.survey.flat.SimplePrompt;
import net.scran24.user.client.survey.prompts.messages.HelpMessages;
import net.scran24.user.client.survey.prompts.messages.PromptMessages;
import net.scran24.user.client.survey.prompts.widgets.QuantityCounter;

import org.pcollections.client.PVector;
import org.pcollections.client.TreePVector;
import org.workcraft.gwt.shared.client.Callback1;
import org.workcraft.gwt.shared.client.Pair;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;

public class QuantityPrompt implements SimplePrompt<Double> {
	private static final PromptMessages messages = PromptMessages.Util.getInstance();
	private static final HelpMessages helpMessages = HelpMessages.Util.getInstance();
	
	private final static PVector<ShepherdTour.Step> tour = TreePVector
			.<ShepherdTour.Step> empty()
			.plus(new ShepherdTour.Step("wholeCounter", "#intake24-quantity-prompt-whole-counter", helpMessages.quantity_wholeCounterTitle(), helpMessages.quantity_wholeCounterDescription()))
			.plus(new ShepherdTour.Step("fracCounter", "#intake24-quantity-prompt-frac-counter", helpMessages.quantity_fractionCounterTitle(), helpMessages.quantity_fractionCounterDescription()))
			.plus(new ShepherdTour.Step("continueButton", "#intake24-quantity-prompt-continue-button", helpMessages.quantity_continueButtonTitle(), helpMessages.quantity_continueButtonDescription()));

	final private SafeHtml promptText;

	public QuantityPrompt(SafeHtml promptText) {
		this.promptText = promptText;
	}
	
	@Override
	public FlowPanel getInterface(final Callback1<Double> onComplete) {
		final FlowPanel content = new FlowPanel();
		content.addStyleName("intake24-quantity-prompt");
		
		FlowPanel promptPanel = WidgetFactory.createPromptPanel(promptText, ShepherdTour.createTourButton(tour, QuantityPrompt.class.getSimpleName()));
		content.add(promptPanel);
		
		final FlowPanel amountPanel = new FlowPanel();
		amountPanel.addStyleName("intake24-quantity-prompt-counter-container");
		
		/*
		
		ArrayList<Pair<String, Double>> wholeLabels = new ArrayList<Pair<String, Double>>();
		
		NumberFormat nf = NumberFormat.getDecimalFormat(); 
		
		for (int i=0; i<31; i++) {
			wholeLabels.add(Pair.create(nf.format(i), (double)i));
		}
	
		ArrayList<Pair<String, Double>> fracLabels = new ArrayList<Pair<String, Double>>();
		
		fracLabels.add(Pair.create(messages.quantity_noFraction(), 0.0));
		fracLabels.add(Pair.create(messages.quantity_oneFourth(), 0.25));
		fracLabels.add(Pair.create(messages.quantity_oneHalf(), 0.5));
		fracLabels.add(Pair.create(messages.quantity_threeFourths(), 0.75));
		
		final LabelledCounter wholeCount = new LabelledCounter(wholeLabels, 1);
		wholeCount.addStyleName("intake24-quantity-prompt-whole-counter");
		wholeCount.getElement().setId("intake24-quantity-prompt-whole-counter");
		
		final LabelledCounter fracCount = new LabelledCounter(fracLabels, 0);
		fracCount.addStyleName("intake24-quantity-prompt-frac-counter");
		fracCount.getElement().setId("intake24-quantity-prompt-frac-counter");
		
		amountPanel.add(wholeCount);
		Label label = new Label (messages.quantity_wholeItemsLabel());
		label.addStyleName("intake24-quantity-prompt-whole-label");
		amountPanel.add(label);
		amountPanel.add(fracCount); */
		
		
		final QuantityCounter counter = new QuantityCounter(0.25, 30, 1.0);

		Button cont = WidgetFactory.createGreenButton(messages.quantity_continueButtonLabel(), new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				onComplete.call(counter.getValue());
			}
		});
		
		cont.getElement().setId("intake24-quantity-prompt-continue-button");
		
		content.add(amountPanel);
		content.add(WidgetFactory.createButtonsPanel(cont));
		
		ShepherdTour.makeShepherdTarget(promptPanel, counter.wholeCounter, counter.fractionalCounter, counter.wholeLabel, cont);
	
		return content;
	}
}