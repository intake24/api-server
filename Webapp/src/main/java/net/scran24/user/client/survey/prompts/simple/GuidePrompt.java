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
import net.scran24.user.client.survey.prompts.messages.HelpMessages;

import org.pcollections.client.PVector;
import org.pcollections.client.TreePVector;
import org.workcraft.gwt.imagemap.client.ImageMap;
import org.workcraft.gwt.imagemap.shared.ImageMapDefinition;
import org.workcraft.gwt.shared.client.Callback1;
import org.workcraft.gwt.shared.client.Function1;
import org.workcraft.gwt.shared.client.Option;

import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Panel;

public class GuidePrompt implements SimplePrompt<Integer> {
	private final static HelpMessages helpMessages = HelpMessages.Util.getInstance();
	
	private final static PVector<ShepherdTour.Step> tour = TreePVector.<ShepherdTour.Step>empty()
			.plus(new ShepherdTour.Step("guidePrompt", "#intake24-guide-prompt-panel", helpMessages.guide_promptTitle(), helpMessages.guide_promptDescription()))
			.plus(new ShepherdTour.Step("guideImage", "#intake24-guide-image-map", helpMessages.guide_imageMapTitle(), helpMessages.guide_imageMapDescription(), false));
	
	private final SafeHtml promptText;
	private final ImageMapDefinition imageMapDef;
	private final Option<Function1<Callback1<Integer>, Panel>> additionalControlsCtor;
	
	public GuidePrompt(SafeHtml promptText, ImageMapDefinition imageMapDef) {
		this (promptText, imageMapDef, Option.<Function1<Callback1<Integer>, Panel>>none());
	}
	
	public GuidePrompt(SafeHtml promptText, ImageMapDefinition imageMapDef, Function1<Callback1<Integer>, Panel> additionalControlsCtor) {
		this (promptText, imageMapDef, Option.some(additionalControlsCtor));				
	}

	private GuidePrompt(SafeHtml promptText, ImageMapDefinition imageMapDef, Option<Function1<Callback1<Integer>, Panel>> additionalControlsCtor) {
		this.promptText = promptText;
		this.imageMapDef = imageMapDef;
		this.additionalControlsCtor = additionalControlsCtor;
	}

	@Override
	public FlowPanel getInterface(final Callback1<Integer> onComplete) {
		final FlowPanel content = new FlowPanel();
		
		FlowPanel promptPanel = WidgetFactory.createPromptPanel(promptText, ShepherdTour.createTourButton(tour, GuidePrompt.class.getSimpleName()));
		ShepherdTour.makeShepherdTarget(promptPanel);
		promptPanel.getElement().setId("intake24-guide-prompt-panel");
		content.add(promptPanel);
		
		additionalControlsCtor.accept(new Option.SideEffectVisitor<Function1<Callback1<Integer>, Panel>>() {
			@Override
			public void visitSome(Function1<Callback1<Integer>, Panel> ctor) {
				content.add(ctor.apply(onComplete));								
			}

			@Override
			public void visitNone() {
			}
		});

		ImageMap imageMap = new ImageMap(imageMapDef, new ImageMap.ResultHandler() {
					@Override
					public void handleResult(final int choice) {
						onComplete.call(choice);
					}
				});
		
		ShepherdTour.makeShepherdTarget(imageMap);
		imageMap.getElement().setId("intake24-guide-image-map");
		
		content.add(imageMap);
		
		return content;
	}
}