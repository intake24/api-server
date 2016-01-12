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
import org.workcraft.gwt.imagechooser.client.ImageChooser;
import org.workcraft.gwt.shared.client.Callback1;

import com.google.gwt.user.client.ui.FlowPanel;

public class AsServedPrompt implements SimplePrompt<Integer> {
	private final AsServedPromptDef def;

	public AsServedPrompt(AsServedPromptDef def) {
		this.def = def;
	}

	private final static HelpMessages helpMessages = HelpMessages.Util.getInstance();

	private final static PVector<ShepherdTour.Step> tour = TreePVector
			.<ShepherdTour.Step> empty()
			.plus(new ShepherdTour.Step("image", "#intake24-as-served-image-container", helpMessages.asServed_imageTitle(), helpMessages
					.asServed_imageDescription()))
			.plus(new ShepherdTour.Step("label", "#intake24-as-served-image-container", helpMessages.asServed_labelTitle(), helpMessages
					.asServed_labelDescription(), "top right", "bottom right"))
			.plus(new ShepherdTour.Step("thumbs", "#intake24-as-served-thumbs-container", helpMessages.asServed_thumbsTitle(), helpMessages
					.asServed_thumbsDescription()))
			.plus(new ShepherdTour.Step("hadLess", "#intake24-as-served-prev-button", helpMessages.asServed_prevButtonTitle(), helpMessages
					.asServed_prevButtonDescription(), "bottom left", "top left"))
			.plus(new ShepherdTour.Step("hadMore", "#intake24-as-served-next-button", helpMessages.asServed_nextButtonTitle(), helpMessages
					.asServed_nextButtonDescription(), "bottom left", "top left"))
			.plus(new ShepherdTour.Step("hadThisMuch", "#intake24-as-served-confirm-button", helpMessages.asServed_confirmButtonTitle(), helpMessages
					.asServed_confirmButtonDescription(), "bottom right", "top right"));

	@Override
	public FlowPanel getInterface(final Callback1<Integer> onComplete) {
		final FlowPanel content = new FlowPanel();

		FlowPanel promptPanel = WidgetFactory.createPromptPanel(def.promptText, ShepherdTour.createTourButton(tour, AsServedPrompt.class.getSimpleName()));
		ShepherdTour.makeShepherdTarget(promptPanel);
		content.add(promptPanel);

		ImageChooser imageChooser = new ImageChooser(def.images, def.prevLabel, def.nextLabel, def.acceptLabel, def.images.length / 2,
				new ImageChooser.ResultHandler() {
					@Override
					public void handleResult(final int index) {
						onComplete.call(index);
					};
				});

		content.add(imageChooser);

		ShepherdTour.makeShepherdTarget(imageChooser.imageContainer, imageChooser.thumbsContainer, imageChooser.nextButton, imageChooser.prevButton,
				imageChooser.confirmButton);

		return content;
	}

	@Override
	public String toString() {
		return "As served portion size prompt";
	}
}