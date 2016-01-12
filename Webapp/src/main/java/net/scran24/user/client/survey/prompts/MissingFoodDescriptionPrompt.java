/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey.prompts;

import net.scran24.common.client.WidgetFactory;
import net.scran24.user.client.ShepherdTour;
import net.scran24.user.client.survey.SurveyStageInterface;
import net.scran24.user.client.survey.flat.FoodOperation;
import net.scran24.user.client.survey.flat.Prompt;
import net.scran24.user.client.survey.prompts.messages.HelpMessages;
import net.scran24.user.client.survey.prompts.messages.PromptMessages;
import net.scran24.user.shared.FoodEntry;
import net.scran24.user.shared.MissingFood;
import net.scran24.user.shared.MissingFoodDescription;

import org.pcollections.client.PVector;
import org.pcollections.client.TreePVector;
import org.workcraft.gwt.shared.client.Callback1;
import org.workcraft.gwt.shared.client.Function1;
import org.workcraft.gwt.shared.client.Option;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;

public class MissingFoodDescriptionPrompt implements Prompt<FoodEntry, FoodOperation> {
	private static final PromptMessages messages = PromptMessages.Util.getInstance();
	private static final HelpMessages helpMessages = HelpMessages.Util.getInstance();
	
	private final static PVector<ShepherdTour.Step> tour = TreePVector
			.<ShepherdTour.Step> empty()
			.plus(new ShepherdTour.Step("foodName", "#intake24-missing-food-name", helpMessages.missingFood_foodNameTitle(), helpMessages.missingFood_foodNameDescription()))
			.plus(new ShepherdTour.Step("brand", "#intake24-missing-food-brand", helpMessages.missingFood_brandTitle(), helpMessages.missingFood_brandDescription()))
			.plus(new ShepherdTour.Step("description", "#intake24-missing-food-description", helpMessages.missingFood_descriptionTitle(), helpMessages.missingFood_descriptionDescription()))
			.plus(new ShepherdTour.Step("portionSize", "#intake24-missing-food-portion-size", helpMessages.missingFood_portionSizeTitle(), helpMessages.missingFood_portionSizeDescription()))
			.plus(new ShepherdTour.Step("leftovers", "#intake24-missing-food-leftovers", helpMessages.missingFood_leftoversTitle(), helpMessages.missingFood_leftoversDescription()))
			.plus(new ShepherdTour.Step("continueButton", "#intake24-missing-food-continue-button", helpMessages.missingFood_continueButtonTitle(), helpMessages.missingFood_continuteButtonDescription()));

	public final MissingFood food;

	private Option<String> mkOption(String s) {
		if (s == null || s.isEmpty())
			return Option.none();
		else
			return Option.some(s);
	}

	public MissingFoodDescriptionPrompt(MissingFood food) {
		this.food = food;
	}

	@Override
	public SurveyStageInterface getInterface(final Callback1<FoodOperation> onComplete,
			final Callback1<Function1<FoodEntry, FoodEntry>> onIntermediateStateChange) {
		final FlowPanel content = new FlowPanel();

		Panel questionPanel;

		if (food.customData.containsKey(MissingFood.KEY_ASSOC_FOOD_NAME)) {
			questionPanel = WidgetFactory.createPromptPanel(SafeHtmlUtils.fromSafeConstant(messages.missingFood_assocFoodPrompt(
					SafeHtmlUtils.htmlEscape(food.name.toLowerCase()), SafeHtmlUtils.htmlEscape(food.customData.get(MissingFood.KEY_ASSOC_FOOD_NAME).toLowerCase()))), ShepherdTour.createTourButton(tour, MissingFoodDescriptionPrompt.class.getSimpleName()));
		} else {
			questionPanel = WidgetFactory.createPromptPanel(SafeHtmlUtils.fromSafeConstant(messages.missingFood_prompt(SafeHtmlUtils
					.htmlEscape(food.name.toLowerCase()))), ShepherdTour.createTourButton(tour, MissingFoodDescriptionPrompt.class.getSimpleName()));
		}

		content.add(questionPanel);

		FlowPanel foodName = new FlowPanel();
		foodName.getElement().setId("intake24-missing-food-name");
		
		Label foodNameLabel = WidgetFactory.createLabel(messages.missingFood_nameLabel());
		content.add(foodNameLabel);
		final TextBox foodNameTextBox = new TextBox();
		foodNameTextBox.getElement().addClassName("intake24-missing-food-textbox");
		foodNameTextBox.setText(food.name);

		foodName.add(foodNameLabel);
		foodName.add(foodNameTextBox);
		
		content.add(foodName);

		if (food.name.equals("Missing food")) {
			Scheduler.get().scheduleDeferred(new ScheduledCommand() {
				@Override
				public void execute() {
					foodNameTextBox.setFocus(true);
					foodNameTextBox.selectAll();
				}
			});
		}
		
		FlowPanel brand = new FlowPanel();
		brand.getElement().setId("intake24-missing-food-brand");

		Label brandLabel = WidgetFactory.createLabel(messages.missingFood_brandLabel());
		brand.add(brandLabel);
		final TextBox brandTextBox = new TextBox();
		brandTextBox.getElement().addClassName("intake24-missing-food-textbox");
		brand.add(brandTextBox);
		content.add(brand);

		FlowPanel description = new FlowPanel();
		description.getElement().setId("intake24-missing-food-description");
		
		Label descriptionLabel = WidgetFactory.createLabel(messages.missingFood_descriptionLabel());
		description.add(descriptionLabel);
		final TextArea descriptionTextArea = new TextArea();
		descriptionTextArea.getElement().addClassName("intake24-missing-food-textarea");
		description.add(descriptionTextArea);
		content.add(description);
		
		FlowPanel portionSize = new FlowPanel();
		portionSize.getElement().setId("intake24-missing-food-portion-size");

		Label portionSizeLabel = WidgetFactory.createLabel(messages.missingFood_portionSizeLabel());
		portionSize.add(portionSizeLabel);
		final TextArea portionSizeTextArea = new TextArea();
		portionSizeTextArea.getElement().addClassName("intake24-missing-food-textarea");
		portionSize.add(portionSizeTextArea);		
		content.add(portionSize);

		FlowPanel leftovers = new FlowPanel();
		leftovers.getElement().setId("intake24-missing-food-leftovers");
		
		Label leftoversLabel = WidgetFactory.createLabel(messages.missingFood_leftoversLabel());
		leftovers.add(leftoversLabel);
		final TextArea leftoversTextArea = new TextArea();
		leftoversTextArea.getElement().addClassName("intake24-missing-food-textarea");
		leftovers.add(leftoversTextArea);
		
		content.add(leftovers);

		Button cont = WidgetFactory.createGreenButton(messages.missingFood_continueButtonLabel(), new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {

				String name = foodNameTextBox.getText();
				if (name.isEmpty())
					name = food.name;

				onComplete.call(FoodOperation.replaceWith(

				new MissingFood(food.link, name, food.isDrink, Option.some(new MissingFoodDescription(mkOption(brandTextBox.getText()),
						mkOption(descriptionTextArea.getText()), mkOption(portionSizeTextArea.getText()), mkOption(leftoversTextArea.getText()))),
						food.flags, food.customData)));

			}
		});
		
		cont.getElement().setId("intake24-missing-food-continue-button");

		content.add(WidgetFactory.createButtonsPanel(cont));
		
		ShepherdTour.makeShepherdTarget(questionPanel, foodName, description, brand, portionSize, leftovers, cont);

		return new SurveyStageInterface.Aligned(content, HasHorizontalAlignment.ALIGN_LEFT, HasVerticalAlignment.ALIGN_TOP,
				SurveyStageInterface.DEFAULT_OPTIONS);
	}

	@Override
	public String toString() {
		return "Missing food description prompt";
	}
}