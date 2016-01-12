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

import net.scran24.common.client.WidgetFactory;
import net.scran24.user.client.ShepherdTour;
import net.scran24.user.client.survey.flat.SimplePrompt;
import net.scran24.user.client.survey.prompts.messages.HelpMessages;
import net.scran24.user.client.survey.prompts.messages.PromptMessages;

import org.pcollections.client.PVector;
import org.pcollections.client.TreePVector;
import org.workcraft.gwt.shared.client.Callback1;
import org.workcraft.gwt.shared.client.Function1;
import org.workcraft.gwt.slidingscale.client.SlidingScale;
import org.workcraft.gwt.slidingscale.shared.SlidingScaleDef;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;

public class DrinkScalePrompt implements SimplePrompt<Double> {	
	private static final PromptMessages messages = PromptMessages.Util.getInstance();
	private static final HelpMessages helpMessages = HelpMessages.Util.getInstance();

	private final static PVector<ShepherdTour.Step> tour = TreePVector
			.<ShepherdTour.Step> empty()
			.plus(new ShepherdTour.Step("image", "#intake24-sliding-scale-image", helpMessages.drinkScale_imageTitle(), helpMessages.drinkScale_imageDescription()))
			.plus(new ShepherdTour.Step("overlay", "#intake24-sliding-scale-overlay", helpMessages.drinkScale_overlayTitle(), helpMessages.drinkScale_overlayDescription()))
			.plus(new ShepherdTour.Step("label", "#intake24-sliding-scale-overlay", helpMessages.drinkScale_volumeLabelTitle(), helpMessages.drinkScale_volumeLabelDescription(), "top right", "bottom right"))
			.plus(new ShepherdTour.Step("slider", "#intake24-sliding-scale-slider", helpMessages.drinkScale_sliderTitle(), helpMessages.drinkScale_sliderDescription(), "middle right", "middle left"))
			.plus(new ShepherdTour.Step("lessButton", "#intake24-sliding-scale-less-button", helpMessages.drinkScale_lessButtonTitle(), helpMessages.drinkScale_lessButtonDescription()))
			.plus(new ShepherdTour.Step("moreButton", "#intake24-sliding-scale-more-button", helpMessages.drinkScale_moreButtonTitle(), helpMessages.drinkScale_moreButtonDescription()))
			.plus(new ShepherdTour.Step("continueButton", "#intake24-sliding-scale-continue-button", helpMessages.drinkScale_continueButtonTitle(), helpMessages.drinkScale_continueButtonDescription(), "top right", "bottom right"));
	
	final private DrinkScalePromptDef def;

	public DrinkScalePrompt(DrinkScalePromptDef def) {
		this.def = def;
	}

	@Override
	public FlowPanel getInterface(final Callback1<Double> onComplete) {
		FlowPanel content = new FlowPanel();
		
		FlowPanel promptPanel = WidgetFactory.createPromptPanel(def.message, ShepherdTour.createTourButton(tour, DrinkScalePrompt.class.getSimpleName()));
		content.add(promptPanel);
		
		SlidingScaleDef ssd = new SlidingScaleDef(def.scaleDef.baseImage, def.scaleDef.overlayImage, def.scaleDef.width, def.scaleDef.height, def.scaleDef.emptyLevel, def.scaleDef.fullLevel);
		
		final Function1<Double, String> label = new Function1<Double, String>() {
			@Override
			public String apply(Double argument) {
				double volume = def.scaleDef.calcVolume(argument);
				int roundedVolume = (int) volume;
				
				NumberFormat nf = NumberFormat.getDecimalFormat();
				
				return nf.format(roundedVolume) + " " + messages.drinkScale_volumeUnit();
			}
		};
		
		final SlidingScale scale = new SlidingScale(ssd, def.limit, def.initialLevel, label);
		
		content.add(scale);

		final Button less = WidgetFactory.createButton(def.lessLabel, new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				scale.sliderBar.setValue(scale.sliderBar.getValue() + scale.sliderBar.getStep());	
				/*if (scale.sliderBar.getValue() > 0.99)
					less.setEnabled(false);
				else
					less.setEnabled(true);*/
			}
		});
		
		less.getElement().setId("intake24-sliding-scale-less-button");
		
		final Button more = WidgetFactory.createButton(def.moreLabel, new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				scale.sliderBar.setValue(scale.sliderBar.getValue() - scale.sliderBar.getStep());
				/*if (scale.sliderBar.getValue() < 0.01)
					more.setEnabled(false);
				else
					more.setEnabled(true);*/
			}
		});
		
		more.getElement().setId("intake24-sliding-scale-more-button");
		
		final Button finish = WidgetFactory.createGreenButton (def.acceptLabel, new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				onComplete.call(scale.getValue());
			}
		});
		
		finish.getElement().setId("intake24-sliding-scale-continue-button");
		
		content.add(WidgetFactory.createButtonsPanel(less, more, finish));
		
		ShepherdTour.makeShepherdTarget(promptPanel, scale.image, scale.overlayDiv, scale.sliderBar, less, more, finish);
		
		return content;
	}
}