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

package net.scran24.user.client.survey.prompts.messages;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Messages;

public interface PromptMessages extends Messages {

	public static class Util {
		private static PromptMessages instance = null;

		public static PromptMessages getInstance() {
			if (instance == null)
				instance = GWT.create(PromptMessages.class);
			return instance;
		}
	}

	public String addMeal_promptText();

	public String addMeal_predefLabel();

	public String addMeal_customLabel();

	public String addMeal_addButtonLabel();

	public String addMeal_cancelButtonLabel();

	public String predefMeal_LateSnack();

	public String predefMeal_EveningMeal();

	public String predefMeal_Dinner();

	public String predefMeal_MidDaySnack();

	public String predefMeal_Lunch();

	public String predefMeal_Snack();

	public String predefMeal_Breakfast();

	public String predefMeal_EarlySnack();

	public String assocFoods_noButtonLabel();

	public String assocFoods_yesButtonLabel();

	public String assocFoods_alreadyEntered();

	public String assocFoods_specificFoodPrompt();

	public String assocFoods_allFoodsDataSetName();

	public String choosePortionMethod_promptText(@Optional String foodDescription);

	public String compFood_missingIngredient();
	
	public String compFood_sandwich_bread();

	public String compFood_sandwich_bread_primary();

	public String compFood_sandwich_bread_secondary();

	public String compFood_sandwich_bread_primary_negative();

	public String compFood_sandwich_bread_secondary_negative();

	public String compFood_sandwich_bread_foods_label();

	public String compFood_sandwich_bread_categories_label();

	public String compFood_sandwich_bread_dataset_label();	
	
	public String compFood_sandwich_spread();

	public String compFood_sandwich_spread_primary();

	public String compFood_sandwich_spread_secondary();

	public String compFood_sandwich_spread_primary_negative();

	public String compFood_sandwich_spread_secondary_negative();

	public String compFood_sandwich_spread_foods_label();

	public String compFood_sandwich_spread_categories_label();

	public String compFood_sandwich_spread_dataset_label();
	
	public String compFood_sandwich_meat_or_fish();

	public String compFood_sandwich_meat_or_fish_primary();

	public String compFood_sandwich_meat_or_fish_secondary();

	public String compFood_sandwich_meat_or_fish_primary_negative();

	public String compFood_sandwich_meat_or_fish_secondary_negative();

	public String compFood_sandwich_meat_or_fish_foods_label();

	public String compFood_sandwich_meat_or_fish_categories_label();

	public String compFood_sandwich_meat_or_fish_dataset_label();
	
	public String compFood_sandwich_cheese_or_dairy();

	public String compFood_sandwich_cheese_or_dairy_primary();

	public String compFood_sandwich_cheese_or_dairy_secondary();

	public String compFood_sandwich_cheese_or_dairy_primary_negative();

	public String compFood_sandwich_cheese_or_dairy_secondary_negative();

	public String compFood_sandwich_cheese_or_dairy_foods_label();

	public String compFood_sandwich_cheese_or_dairy_categories_label();

	public String compFood_sandwich_cheese_or_dairy_dataset_label();
	
	public String compFood_sandwich_extra_filling();

	public String compFood_sandwich_extra_filling_primary();

	public String compFood_sandwich_extra_filling_secondary();

	public String compFood_sandwich_extra_filling_primary_negative();

	public String compFood_sandwich_extra_filling_secondary_negative();

	public String compFood_sandwich_extra_filling_foods_label();

	public String compFood_sandwich_extra_filling_categories_label();

	public String compFood_sandwich_extra_filling_dataset_label();
	
	public String compFood_sandwich_sauce_or_dressing();

	public String compFood_sandwich_sauce_or_dressing_primary();

	public String compFood_sandwich_sauce_or_dressing_secondary();

	public String compFood_sandwich_sauce_or_dressing_primary_negative();

	public String compFood_sandwich_sauce_or_dressing_secondary_negative();

	public String compFood_sandwich_sauce_or_dressing_foods_label();

	public String compFood_sandwich_sauce_or_dressing_categories_label();

	public String compFood_sandwich_sauce_or_dressing_dataset_label();
	
	
	public String compFood_salad_ingredient();

	public String compFood_salad_ingredient_primary();

	public String compFood_salad_ingredient_secondary();

	public String compFood_salad_ingredient_primary_negative();

	public String compFood_salad_ingredient_secondary_negative();

	public String compFood_salad_ingredient_foods_label();

	public String compFood_salad_ingredient_categories_label();

	public String compFood_salad_ingredient_dataset_label();
	
	public String compFood_salad_sauce_or_dressing();

	public String compFood_salad_sauce_or_dressing_primary();

	public String compFood_salad_sauce_or_dressing_secondary();

	public String compFood_salad_sauce_or_dressing_primary_negative();

	public String compFood_salad_sauce_or_dressing_secondary_negative();

	public String compFood_salad_sauce_or_dressing_foods_label();

	public String compFood_salad_sauce_or_dressing_categories_label();

	public String compFood_salad_sauce_or_dressing_dataset_label();
	
	public String completion_promptText();

	public String completion_submitButtonLabel();

	public String timeGap_promptText_beforeMeal(String mealName, String mealTime);

	public String timeGap_promptText_afterMeal(String mealName, String mealTime);

	public String timeGap_promptText_betweenMeals(String mealName1, String mealTime1, String mealName2, String mealTime2);

	public String timeGap_addMealButtonLabel();

	public String timeGap_confirmTimeGapButtonLabel();

	public String confirmMeal_promptText_breakfast();      
	                                                        
	public String confirmMeal_promptText_earlySnack();     
	
	public String confirmMeal_promptText_snack();
	                                                       
	public String confirmMeal_promptText_lunch();          
	                                                        
	public String confirmMeal_promptText_midDaySnack();    
	                                                        
	public String confirmMeal_promptText_dinner();         
	                                                        
	public String confirmMeal_promptText_eveningMeal();    
	                                                        
	public String confirmMeal_promptText_lateSnack();      	
	
	public String confirmMeal_promptText_generic(@Optional String mealNameLowercase, @Optional String mealNameCapitalised);
	
	public String confirmMeal_skipButtonLabel_breakfast();
	
	public String confirmMeal_skipButtonLabel_earlySnack();
	
	public String confirmMeal_skipButtonLabel_snack();
	
	public String confirmMeal_skipButtonLabel_lunch();
	
	public String confirmMeal_skipButtonLabel_midDaySnack();
	
	public String confirmMeal_skipButtonLabel_dinner();
	
	public String confirmMeal_skipButtonLabel_eveningMeal();
	
	public String confirmMeal_skipButtonLabel_lateSnack();
	
	public String confirmMeal_skipButtonLabel_generic(@Optional String mealName);

	public String confirmMeal_confirmButtonLabel();
	
	public String deleteMeal_promptText(@Optional String mealName);

	public String deleteMeal_deleteButtonLabel();

	public String deleteMeal_keepButtonLabel();

	public String editMeal_promptText(@Optional String mealName);

	public String editMeal_foodPlaceholder();

	public String editMeal_drinkPlaceholder();

	public String editMeal_addDrinkButtonLabel();

	public String editMeal_addFoodButtonLabel();

	public String editMeal_foodHeader();

	public String editMeal_drinksHeader();

	public String editMeal_finishButtonLabel();

	public String editMeal_changeTimeButtonLabel();

	public String editMeal_deleteMealButtonLabel();

	public String editMeal_listItemPlaceholder();

	public String editMeal_listItem_deleteButtonLabel();

	public String editTime_cancelButtonLabel();

	public String editTime_confirmButtonLabel();

	public String foodBrowser_searchResultsEmpty();

	public String foodBrowser_matchingFoodsHeader();

	public String foodBrowser_matchingCategoriesHeader();

	public String foodBrowser_loadingMessage();

	public String foodBrowser_browseCategoriesHeader();

	public String foodBrowser_browseFoodsHeader();

	public String foodBrowser_browseLoadingMessage(String category);

	public String foodBrowser_currentlyBrowsing(String category);

	public String foodBrowser_allCategoriesHeader();

	public String foodBrowser_allFoodsDataSetName();

	public String foodBrowser_backToParent(String name);

	public String foodBrowser_cantFindButtonLabel();

	public String foodBrowser_reportMissingFoodButtonLabel();

	public String foodBrowser_cantFindFullPopupContents();

	public String foodBrowser_cantFindBrowseOnlyPopupContents();

	public String foodBrowser_cantFindTryAgainButtonLabel();

	public String foodBrowser_homemadeSandwich();

	public String foodBrowser_homemadeSalad();
	
	public String foodBrowser_sandwichShortName();
	
	public String foodBrowser_saladShortName();
	
	public String recipeBrowser_showAllRecipes();
	
	public String recipeBrowser_deleteRecipes();
	
	public String recipeBrowser_yourRecipes();
	
	public String recipeBrowser_done();	

	public String foodComplete_promptText(String foodName);

	public String foodComplete_continueButtonLabel();

	public String foodComplete_editMealButtonLabel();

	public String foodComplete_deleteFoodButtonLabel();
	
	public String foodComplete_editIngredients();

	public String foodLookup_loadingMessage(String searchTerm);

	public String foodLookup_searchResultsHeader(String htmlEscape);

	public String foodLookup_serverError();

	public String foodLookup_searchButtonLabel();

	public String foodBrowser_browseAllFoodsLabel();

	public String foodLookup_resultsDataSetName();

	public String mealComplete_promptText(String mealName);

	public String mealComplete_continueButtonLabel();

	public String mealComplete_editMealButtonLabel();

	public String mealComplete_editTimeButtonLabel();

	public String mealComplete_deleteButtonLabel();

	public String splitFood_promptText();
	
	public String splitFood_split();
	
	public String splitFood_keep();
	
	public String splitFood_separateSuggestion();

	public String splitFood_yesButtonLabel();

	public String splitFood_noButtonLabel();

	public String drinkReminder_promptText(@Optional String mealName);

	public String drinkReminder_addDrinkButtonLabel();

	public String drinkReminder_noDrinkButtonLabel();

	public String noPortionMethod_promptText(String desc);

	public String noPortionMethod_continueButtonLabel();

	public String yesNoQuestion_defaultYesLabel();

	public String yesNoQuestion_defaultNoLabel();

	public String drinkScale_volumeUnit();

	public String quantity_zeroLabel();

	public String quantity_oneLabel();

	public String quantity_twoLabel();

	public String quantity_threeLabel();

	public String quantity_fourLabel();

	public String quantity_fiveLabel();

	public String quantity_sixLabel();

	public String quantity_sevenLabel();

	public String quantity_eightLabel();

	public String quantity_nineLabel();

	public String quantity_tenLabel();

	public String quantity_noFraction();

	public String quantity_oneFourth();

	public String quantity_threeFourths();

	public String quantity_oneHalf();

	public String quantity_wholeItemsLabel();

	public String quantity_continueButtonLabel();

	public String standardUnit_choiceLabel(String htmlEscape);

	public String asServed_servedLessButtonLabel();

	public String asServed_servedMoreButtonLabel();

	public String asServed_servedContinueButtonLabel();

	public String asServed_leftLessButtonLabel();

	public String asServed_leftMoreButtonLabel();

	public String asServed_leftContinueButtonLabel();
	
	public String asServed_weightUnitLabel();

	public String asServed_servingPromptText(String foodDescription);

	public String asServed_leftoversPromptText(String foodDescription);

	public String asServed_leftoversQuestionPromptText(String foodDescription);

	public String cereal_bowlPromptText();

	public String drinkScale_containerPromptText(@Optional String foodDesc);

	public String drinkScale_servedPromptText();

	public String drinkScale_leftoversQuestionPromptText(@Optional String escapedFoodDesc);

	public String drinkScale_servedLessButtonLabel();

	public String drinkScale_servedMoreButtonLabel();

	public String drinkScale_servedContinueButtonLabel();

	public String drinkScale_leftLessButtonLabel();

	public String drinkScale_leftMoreButtonLabel();

	public String drinkScale_leftContinueButtonLabel();

	public String drinkScale_leftPromptText(@Optional String escapedFoodDesc);

	public String guide_choicePromptText();

	public String guide_quantityPromptText();

	public String guide_quantityContinueButtonLabel();

	public String cereal_milkLevelPromptText();

	public String pizza_typePromptText();

	public String pizza_sliceSizePromptText();

	public String pizza_sliceWholePizza();

	public String pizza_sliceQuantityPromptText();

	public String pizza_sliceQuantityContinueButtonLabel();

	public String pizza_thicknessPromptText();

	public String standardUnit_quantityPromptText_omitFood(String unit);

	public String standardUnit_quantityPromptText_includeFood(String unit, String foodDesc);

	public String standardUnit_quantityContinueButtonLabel();

	public String standardUnit_unitChoicePromptText();

	public String standardUnit_unitChoiceContinueButtonLabel();

	public String readyMeals_promptText(String mealName);

	public String milkInHotDrink_promptText(String milkName, String drinkName);

	public String milkInHotDrink_confirmButtonLabel();

	public String milkInHotDrink_amountLittle();

	public String milkInHotDrink_amountAverage();

	public String milkInHotDrink_amountLot();

	public String sameAsBefore_promptText(String foodDescription);

	public String sameAsBefore_servingSize(String weight);

	public String sameAsBefore_noLeftoversDrink();

	public String sameAsBefore_noLeftoversFood();

	public String sameAsBefore_leftovers(String leftovers);

	public String sameAsBefore_hadItWith();

	public String sameAsBefore_noAddedFoods();

	public String sameAsBefore_confirmButtonLabel();

	public String sameAsBefore_rejectButtonLabel();

	public String energyValidation_promptText();

	public String energyValidation_confirmButtonLabel();

	public String energyValidation_addMealButtonLabel();

	public String emptySurvey_promptText();

	public String brandName_promptText(String foodDescription);

	public String brandName_continueButtonLabel();

	public String loadingPortionSize();

	public String submitPage_success();

	public String submitPage_timeout();

	public String submitPage_loadingMessage();

	public String submitPage_tryAgainButton();

	public String submitPage_error();

	public String missingFood_assocFoodPrompt(String foodName, String assocFoodName);

	public String missingFood_prompt(String foodName);

	public String missingFood_nameLabel();

	public String missingFood_brandLabel();

	public String missingFood_descriptionLabel();

	public String missingFood_portionSizeLabel();

	public String missingFood_leftoversLabel();

	public String missingFood_continueButtonLabel();
	
	public String missingFood_recipeLabel();

	public String homeRecipe_promptText(String foodName);

	public String homeRecipe_haveRecipeChoice();

	public String homeRecipe_noRecipeChoice();

	public String homeRecipe_continueButtonLabel();

	public String homeRecipe_servingsPromptText(String foodName);

	public String homeRecipe_servingsButtonLabel();

	public String homeRecipe_savePromptText(String foodName);

	public String homeRecipe_recipeNameLabel();

	public String missingFood_simpleRecipe_assocFoodPrompt(String foodName, String assocFoodName);

	public String missingFood_simpleRecipe_prompt(String foodName);

	public String missingFood_simpleRecipe_servedLabel();
	
	public String missingFood_simpleRecipe_leftoversLabel();

	public String missingFood_simpleRecipe_nameLabel();

	public String editRecipeIngredientsPrompt_promptText(String foodName);

	public String editRecipeIngredientsPrompt_ingredientsHeader();

	public String editRecipeIngredientsPrompt_continueButtonLabel();

	public String weightTypeIn_rangeError();

	public String weightTypeIn_formatError();

	public String weightTypeIn_promptText(String foodName);

	public String weightTypeIn_unitLabel();

	public String weightTypeIn_continueLabel();

  public String breadLinkedFood_promptText(String foodDescription, String mainFoodDescription, String quantity);
  
  public String breadLinkedFood_allButtonLabel();
  
  public String breadLinkedFood_someButtonLabel();

}
