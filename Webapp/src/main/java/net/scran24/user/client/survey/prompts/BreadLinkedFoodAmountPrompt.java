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

import org.pcollections.client.PVector;
import org.pcollections.client.TreePVector;
import org.workcraft.gwt.shared.client.Callback1;
import org.workcraft.gwt.shared.client.Function1;
import org.workcraft.gwt.shared.client.Pair;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;

import net.scran24.common.client.GoogleAnalytics;
import net.scran24.common.client.WidgetFactory;
import net.scran24.user.client.ShepherdTour;
import net.scran24.user.client.survey.SurveyStageInterface;
import net.scran24.user.client.survey.flat.Prompt;
import net.scran24.user.client.survey.flat.PromptUtil;
import net.scran24.user.client.survey.flat.rules.ShowBreadLinkedFoodAmountPrompt;
import net.scran24.user.client.survey.portionsize.experimental.PortionSize;
import net.scran24.user.client.survey.prompts.messages.HelpMessages;
import net.scran24.user.client.survey.prompts.messages.PromptMessages;
import net.scran24.user.client.survey.prompts.widgets.QuantityCounter;
import net.scran24.user.shared.EncodedFood;
import net.scran24.user.shared.FoodEntry;
import net.scran24.user.shared.Meal;

public class BreadLinkedFoodAmountPrompt implements Prompt<Pair<FoodEntry, Meal>, MealOperation> {
  private static final PromptMessages messages = PromptMessages.Util.getInstance();
  private static final HelpMessages helpMessages = HelpMessages.Util.getInstance();

  private final static PVector<ShepherdTour.Step> collapsedTour = TreePVector.<ShepherdTour.Step>empty()
    .plus(new ShepherdTour.Step("hours", "#intake24-all-button", messages.breadLinkedFood_allButtonLabel(),
        helpMessages.breadLinkedFood_allButtonDescription()))
    .plus(new ShepherdTour.Step("minutes", "#intake24-some-button", messages.breadLinkedFood_someButtonLabel(),
        helpMessages.breadLinkedFood_someButtonDescription()));

  private final static PVector<ShepherdTour.Step> fullTour = TreePVector.<ShepherdTour.Step>empty()
    .plus(new ShepherdTour.Step("hours", "#intake24-all-button", messages.breadLinkedFood_allButtonLabel(),
        helpMessages.breadLinkedFood_allButtonDescription()))
    .plus(new ShepherdTour.Step("minutes", "#intake24-some-button", messages.breadLinkedFood_someButtonLabel(),
        helpMessages.breadLinkedFood_someButtonDescription()))
    .plus(new ShepherdTour.Step("wholeCounter", "#intake24-quantity-prompt-whole-counter", helpMessages.quantity_wholeCounterTitle(),
        helpMessages.quantity_wholeCounterDescription()))
    .plus(new ShepherdTour.Step("fracCounter", "#intake24-quantity-prompt-frac-counter", helpMessages.quantity_fractionCounterTitle(),
        helpMessages.quantity_fractionCounterDescription()))
    .plus(new ShepherdTour.Step("continueButton", "#intake24-quantity-prompt-continue-button", helpMessages.quantity_continueButtonTitle(),
        helpMessages.quantity_continueButtonDescription()));

  private final Meal meal;
  private final int foodIndex;
  private final int mainFoodIndex;
  private final double quantity;

  public BreadLinkedFoodAmountPrompt(Meal meal, int foodIndex, int mainFoodIndex, double quantity) {
    this.meal = meal;
    this.foodIndex = foodIndex;
    this.mainFoodIndex = mainFoodIndex;
    this.quantity = quantity;
  }

  @Override
  public SurveyStageInterface getInterface(final Callback1<MealOperation> onComplete,
      Callback1<Function1<Pair<FoodEntry, Meal>, Pair<FoodEntry, Meal>>> updateIntermediateState) {
    FlowPanel content = new FlowPanel();

    final EncodedFood food = meal.foods.get(foodIndex).asEncoded();
    final EncodedFood mainFood = meal.foods.get(mainFoodIndex).asEncoded();

    final String foodDescription = SafeHtmlUtils.htmlEscape(food.description().toLowerCase());
    final String mainFoodDescription = SafeHtmlUtils.htmlEscape(mainFood.description().toLowerCase());

    final String quantityStr = NumberFormat.getDecimalFormat().format(quantity);

    final FlowPanel quantityPanel = new FlowPanel();
    quantityPanel.setVisible(false);

    FlowPanel promptPanel = WidgetFactory.createPromptPanel(
        SafeHtmlUtils.fromSafeConstant(messages.breadLinkedFood_promptText(foodDescription, mainFoodDescription, quantityStr)),
        WidgetFactory.createHelpButton(new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            String promptType = BreadLinkedFoodAmountPrompt.class.getSimpleName();
            GoogleAnalytics.trackHelpButtonClicked(promptType);
            if (quantityPanel.isVisible())
              ShepherdTour.startTour(fullTour, promptType);
            else
              ShepherdTour.startTour(collapsedTour, promptType);
          }
        }));

    PromptUtil.addBackLink(content);
    content.add(promptPanel);
    ShepherdTour.makeShepherdTarget(promptPanel);

    Button allButton = WidgetFactory.createButton(messages.breadLinkedFood_allButtonLabel(), new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        onComplete.call(MealOperation.updateFood(foodIndex, new Function1<FoodEntry, FoodEntry>() {
          @Override
          public FoodEntry apply(FoodEntry argument) {
            EncodedFood f = argument.asEncoded();
            return f.withPortionSize(PortionSize.complete(f.completedPortionSize().multiply(quantity)))
              .withFlag(ShowBreadLinkedFoodAmountPrompt.FLAG_BREAD_LINKED_FOOD_AMOUNT_SHOWN);
          }
        }));
      }
    });

    allButton.getElement().setId("intake24-all-button");
    ShepherdTour.makeShepherdTarget(allButton);

    Button someButton = WidgetFactory.createButton(messages.breadLinkedFood_someButtonLabel(), new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        quantityPanel.setVisible(true);
      }
    });

    someButton.getElement().setId("intake24-some-button");
    ShepherdTour.makeShepherdTarget(someButton);

    content.add(WidgetFactory.createButtonsPanel(allButton, someButton));

    final QuantityCounter counter = new QuantityCounter(0.25, quantity, Math.max(1.0, quantity));

    ShepherdTour.makeShepherdTarget(counter.fractionalCounter);
    ShepherdTour.makeShepherdTarget(counter.wholeLabel);
    ShepherdTour.makeShepherdTarget(counter.wholeCounter);

    Button confirmQuantityButton = WidgetFactory.createGreenButton(messages.quantity_continueButtonLabel(), new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        onComplete.call(MealOperation.updateFood(foodIndex, new Function1<FoodEntry, FoodEntry>() {
          @Override
          public FoodEntry apply(FoodEntry argument) {
            EncodedFood f = argument.asEncoded();
            return f.withPortionSize(PortionSize.complete(f.completedPortionSize().multiply(counter.getValue())))
              .withFlag(ShowBreadLinkedFoodAmountPrompt.FLAG_BREAD_LINKED_FOOD_AMOUNT_SHOWN);
          }
        }));
      }
    });

    confirmQuantityButton.getElement().setId("intake24-quantity-prompt-continue-button");

    quantityPanel.add(counter);
    quantityPanel.add(confirmQuantityButton);

    ShepherdTour.makeShepherdTarget(confirmQuantityButton);

    content.add(quantityPanel);

    return new SurveyStageInterface.Aligned(content, HasHorizontalAlignment.ALIGN_LEFT, HasVerticalAlignment.ALIGN_TOP,
        SurveyStageInterface.DEFAULT_OPTIONS);
  }

  @Override
  public String toString() {
    return "Confirm meal prompt";
  }
}