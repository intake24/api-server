/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey.prompts.messages;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Messages;

public interface HelpMessages extends Messages {

  public static class Util {
    private static HelpMessages instance = null;

    public static HelpMessages getInstance() {
      if (instance == null)
        instance = GWT.create(HelpMessages.class);
      return instance;
    }
  }

  public String editMeal_mealNameTitle();

  public String editMeal_mealNameDescription();

  public String editMeal_foodListTitle();

  public String editMeal_foodListDescription();

  public String editMeal_drinkListDescription();

  public String editMeal_drinkListTitle();

  public String editMeal_changeTimeButtonTitle();

  public String editMeal_changeTimeButtonDescription();

  public String editMeal_deleteMealButtonDescription();

  public String editMeal_deleteMealButtonTitle();

  public String editMeal_continueButtonTitle();

  public String editMeal_continueButtonDescription();

  public String addMeal_predefNameTitle();

  public String addMeal_predefNameDescription();

  public String addMeal_customNameTitle();

  public String addMeal_customNameDescription();

  public String addMeal_acceptButtonTitle();

  public String addMeal_acceptButtonDescription();

  public String addMeal_cancelButtonTitle();

  public String addMeal_cancelButtonDescription();

  public String asServed_imageTitle();

  public String asServed_imageDescription();

  public String asServed_labelTitle();

  public String asServed_labelDescription();

  public String asServed_thumbsTitle();

  public String asServed_thumbsDescription();

  public String asServed_prevButtonTitle();

  public String asServed_nextButtonTitle();

  public String asServed_confirmButtonTitle();

  public String asServed_prevButtonDescription();

  public String asServed_nextButtonDescription();

  public String asServed_confirmButtonDescription();

  public String assocFood_yesButtonTitle();

  public String assocFood_yesButtonDescription();

  public String assocFood_noButtonTitle();

  public String assocFood_noButtonDescription();

  public String assocFood_yesExistingButtonTitle();

  public String assocFood_yesExistingButtonDescription();

  public String brandName_brandListTitle();

  public String brandName_brandListDescription();

  public String brandName_continueButtonTitle();

  public String brandName_continueButtonDescription();

  public String readyMeals_listTitle();

  public String readyMeals_listDescription();

  public String readyMeals_finishedButtonTitle();

  public String readyMeals_finishedButtonDescription();

  public String chooseMethod_panelTitle();

  public String chooseMethod_panelDescription();

  public String foodBrowser_foodsTitle();

  public String foodBrowser_foodsDescription();

  public String foodBrowser_categoriesTitle();

  public String foodBrowser_categoriesDescription();

  public String foodBrowser_cantFindButtonTitle();

  public String foodBrowser_cantFindButtonDescription();

  public String foodBrowser_skipButtonTitle();

  public String foodBrowser_skipButtonDescription();

  public String foodBrowser_browseAllTitle();

  public String foodBrowser_browseAllDescription();

  public String foodBrowser_tryAgainButtonTitle();

  public String foodBrowser_tryAgainButtonDescription();

  public String foodBrowser_missingFoodButtonTitle();

  public String foodBrowser_missingFoodButtonDescription();

  public String compoundFood_promptTitle();

  public String compoundFood_promptDescription();

  public String foodLookup_textboxTitle();

  public String foodLookup_textboxDescription();

  public String foodLookup_searchButtonTitle();

  public String foodLookup_searchButtonDescription();

  public String recipeBrowser_recipesTitle();

  public String recipeBrowser_recipesDescription();

  public String recipeBrowser_deleteButtonTitle();

  public String recipeBrowser_deleteButtonDescription();

  public String recipeBrowser_showAllButtonTitle();

  public String recipeBrowser_showAllButtonDescription();

  public String guide_imageMapTitle();

  public String guide_imageMapDescription();

  public String guide_promptTitle();

  public String guide_promptDescription();

  public String multipleChoice_questionTitle();

  public String multipleChoice_questionDescription();

  public String multipleChoice_choicesTitle();

  public String multipleChoice_choicesDescription();

  public String multipleChoice_continueButtonTitle();

  public String multipleChoice_continueButtonDescription();

  public String timeQuestion_hoursTitle();

  public String timeQuestion_hoursDescription();

  public String timeQuestion_minutesTitle();

  public String timeQuestion_minutesDescription();

  public String timeQuestion_cancelButtonTitle();

  public String timeQuestion_cancelButtonDescription();

  public String timeQuestion_deleteMealButtonTitle();

  public String timeQuestion_deleteMealButtonDescription();

  public String timeQuestion_confirmButtonTitle();

  public String timeQuestion_confirmButtonDescription();

  public String missingFood_foodNameTitle();

  public String missingFood_foodNameDescription();

  public String missingFood_descriptionTitle();

  public String missingFood_descriptionDescription();

  public String missingFood_brandTitle();

  public String missingFood_brandDescription();

  public String missingFood_portionSizeTitle();

  public String missingFood_portionSizeDescription();

  public String missingFood_leftoversTitle();

  public String missingFood_leftoversDescription();

  public String missingFood_continueButtonTitle();

  public String missingFood_continuteButtonDescription();

  public String missingFood_simpleRecipeTitle();

  public String missingFood_simpleRecipeDescription();

  public String missingFood_simpleRecipe_servedTitle();

  public String missingFood_simpleRecipe_servedDescription();

  public String quantity_wholeCounterTitle();

  public String quantity_wholeCounterDescription();

  public String quantity_fractionCounterTitle();

  public String quantity_fractionCounterDescription();

  public String quantity_continueButtonTitle();

  public String quantity_continueButtonDescription();

  public String sameAsBefore_portionSizeTitle();

  public String sameAsBefore_portionSizeDescription();

  public String sameAsBefore_leftoversTitle();

  public String sameAsBefore_leftoversDescription();

  public String sameAsBefore_assocFoodsTitle();

  public String sameAsBefore_assocFoodsDescription();

  public String sameAsBefore_yesButtonTitle();

  public String sameAsBefore_yesButtonDescription();

  public String sameAsBefore_noButtonTitle();

  public String sameAsBefore_noButtonDescription();

  public String drinkScale_imageTitle();

  public String drinkScale_imageDescription();

  public String drinkScale_overlayTitle();

  public String drinkScale_overlayDescription();

  public String drinkScale_sliderTitle();

  public String drinkScale_sliderDescription();

  public String drinkScale_lessButtonTitle();

  public String drinkScale_lessButtonDescription();

  public String drinkScale_moreButtonTitle();

  public String drinkScale_moreButtonDescription();

  public String drinkScale_continueButtonTitle();

  public String drinkScale_continueButtonDescription();

  public String drinkScale_volumeLabelTitle();

  public String drinkScale_volumeLabelDescription();

  public String missingFood_simpleRecipe_foodNameTitle();

  public String missingFood_simpleRecipe_foodNameDescription();

  public String editIngredients_foodListTitle();

  public String editIngredients_foodListDescription();

  public String editIngredients_continueButtonTitle();

  public String editIngredients_continueButtonDescription();

  public String breadLinkedFood_allButtonDescription();

  public String breadLinkedFood_someButtonDescription();

}
