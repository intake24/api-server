/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client;

import net.scran24.common.client.BrowserConsole;
import net.scran24.user.client.survey.prompts.messages.PromptMessages;
import net.scran24.user.shared.Meal;

import org.pcollections.client.PVector;
import org.pcollections.client.TreePVector;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.LocaleInfo;

public class PredefinedMeals {
  private static PromptMessages messages = GWT.create(PromptMessages.class);

  private static final PVector<Meal> defaultStartingMeals = TreePVector.<Meal>empty()
    .plus(Meal.empty(messages.predefMeal_Breakfast()))
    .plus(Meal.empty(messages.predefMeal_EarlySnack()))
    .plus(Meal.empty(messages.predefMeal_Lunch()))
    .plus(Meal.empty(messages.predefMeal_MidDaySnack()))
    .plus(Meal.empty(messages.predefMeal_EveningMeal()))
    .plus(Meal.empty(messages.predefMeal_LateSnack()));

  private static final PVector<Meal> portugueseStartingMeals = TreePVector.<Meal>empty()
    .plus(Meal.empty(messages.predefMeal_Breakfast()))
    .plus(Meal.empty(messages.predefMeal_EarlySnack()))
    .plus(Meal.empty(messages.predefMeal_Lunch()))
    .plus(Meal.empty(messages.predefMeal_MidDaySnack()))
    .plus(Meal.empty(messages.predefMeal_Dinner()))
    .plus(Meal.empty(messages.predefMeal_EveningMeal()));

  private static final String[] defaultSuggestedMealNames = {
    messages.predefMeal_EarlySnack(),
    messages.predefMeal_Breakfast(),
    messages.predefMeal_Snack(),
    messages.predefMeal_Lunch(),
    messages.predefMeal_MidDaySnack(),
    messages.predefMeal_Dinner(),
    messages.predefMeal_EveningMeal(),
    messages.predefMeal_LateSnack()
  };

  private static final String[] portugueseSuggestedMealNames = {
    messages.predefMeal_EarlySnack(),
    messages.predefMeal_Breakfast(),
    messages.predefMeal_Snack(),
    messages.predefMeal_Lunch(),
    messages.predefMeal_MidDaySnack(),
    messages.predefMeal_Dinner(),
    messages.predefMeal_EveningMeal(),
    messages.predefMeal_LateSnack()
  };

  public static PVector<Meal> getStartingMealsForCurrentLocale() {
    BrowserConsole.warn(LocaleInfo.getCurrentLocale().getLocaleName());
    switch (LocaleInfo.getCurrentLocale().getLocaleName()) {
      case "pt_PT":
        return portugueseStartingMeals;
      default:
        return defaultStartingMeals;
    }
  }

  public static String[] getSuggestedMealNamesForCurrentLocale() {
    switch(LocaleInfo.getCurrentLocale().getLocaleName()) {
      case "pt_PT":
        return portugueseSuggestedMealNames;
      default:
        return defaultSuggestedMealNames;
    }
  }
}
