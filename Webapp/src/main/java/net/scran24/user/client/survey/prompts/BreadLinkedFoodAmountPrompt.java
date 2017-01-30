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

import net.scran24.common.client.GoogleAnalytics;
import net.scran24.common.client.WidgetFactory;
import net.scran24.datastore.shared.Time;

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
import net.scran24.user.shared.FoodPrompt;
import net.scran24.user.shared.Meal;
import uk.ac.ncl.openlab.intake24.datastoresql.CompletedPortionSize;

import org.pcollections.client.PVector;
import org.pcollections.client.TreePVector;
import org.workcraft.gwt.shared.client.Callback1;
import org.workcraft.gwt.shared.client.Either;
import org.workcraft.gwt.shared.client.Function1;
import org.workcraft.gwt.shared.client.Pair;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Panel;

import static net.scran24.common.client.LocaleUtil.*;

public class BreadLinkedFoodAmountPrompt implements Prompt<Pair<FoodEntry, Meal>, MealOperation> {
  private static final PromptMessages messages = PromptMessages.Util.getInstance();
  private static final HelpMessages helpMessages = HelpMessages.Util.getInstance();

  private final static PVector<ShepherdTour.Step> collapsedTour = TreePVector.<ShepherdTour.Step>empty()
    .plus(new ShepherdTour.Step("hours", "#intake24-all-button", helpMessages.timeQuestion_hoursTitle(),
        helpMessages.timeQuestion_hoursDescription()))
    .plus(new ShepherdTour.Step("minutes", "#intake24-specify-button", helpMessages.timeQuestion_minutesTitle(),
        helpMessages.timeQuestion_minutesDescription()))
    .plus(new ShepherdTour.Step("skipButton", "#intake24-time-question-skip-button", helpMessages.timeQuestion_deleteMealButtonTitle(),
        helpMessages.timeQuestion_deleteMealButtonDescription()))
    .plus(new ShepherdTour.Step("confirmButton", "#intake24-time-question-confirm-button", helpMessages.timeQuestion_confirmButtonTitle(),
        helpMessages.timeQuestion_confirmButtonDescription(), "top right", "bottom right"));
  
  private final static PVector<ShepherdTour.Step> fullTour = TreePVector.<ShepherdTour.Step>empty()
      .plus(new ShepherdTour.Step("hours", "#intake24-time-question-hours", helpMessages.timeQuestion_hoursTitle(),
          helpMessages.timeQuestion_hoursDescription()))
      .plus(new ShepherdTour.Step("minutes", "#intake24-time-question-minutes", helpMessages.timeQuestion_minutesTitle(),
          helpMessages.timeQuestion_minutesDescription()))
      .plus(new ShepherdTour.Step("skipButton", "#intake24-time-question-skip-button", helpMessages.timeQuestion_deleteMealButtonTitle(),
          helpMessages.timeQuestion_deleteMealButtonDescription()))
      .plus(new ShepherdTour.Step("confirmButton", "#intake24-time-question-confirm-button", helpMessages.timeQuestion_confirmButtonTitle(),
          helpMessages.timeQuestion_confirmButtonDescription(), "top right", "bottom right"));
  

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

    FlowPanel promptPanel = WidgetFactory.createPromptPanel(
        SafeHtmlUtils.fromSafeConstant(messages.breadLinkedFood_promptText(foodDescription, mainFoodDescription, quantityStr)),
        ShepherdTour.createTourButton(null, BreadLinkedFoodAmountPrompt.class.getSimpleName()));

    PromptUtil.addBackLink(content);
    content.add(promptPanel);

    final FlowPanel quantityPanel = new FlowPanel();
    quantityPanel.setVisible(false);

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

    Button specifyButton = WidgetFactory.createButton(messages.breadLinkedFood_someButtonLabel(), new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        quantityPanel.setVisible(true);
      }
    });
    
    specifyButton.getElement().setId("intake24-specify-button");

    content.add(WidgetFactory.createButtonsPanel(allButton, specifyButton));

    final QuantityCounter counter = new QuantityCounter(0.25, quantity, Math.max(1.0, quantity));

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

    content.add(quantityPanel);

    return new SurveyStageInterface.Aligned(content, HasHorizontalAlignment.ALIGN_LEFT, HasVerticalAlignment.ALIGN_TOP,
        SurveyStageInterface.DEFAULT_OPTIONS);
  }

  @Override
  public String toString() {
    return "Confirm meal prompt";
  }
}