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

package net.scran24.user.client.surveyscheme;

import static org.workcraft.gwt.shared.client.CollectionUtils.exists;
import static org.workcraft.gwt.shared.client.CollectionUtils.forall;
import static org.workcraft.gwt.shared.client.CollectionUtils.map;

import java.util.Map;

import net.scran24.common.client.CurrentUser;
import net.scran24.datastore.shared.Time;
import net.scran24.user.client.ProcessMilkInHotDrinks;
import net.scran24.user.client.SurveyInterfaceManager;
import net.scran24.user.client.survey.CompoundFoodTemplateManager;
import net.scran24.user.client.survey.SurveyMessages;
import net.scran24.user.client.survey.WelcomePage;
import net.scran24.user.client.survey.flat.FlatFinalPage;
import net.scran24.user.client.survey.flat.FoodOperation;
import net.scran24.user.client.survey.flat.IntakeSurvey;
import net.scran24.user.client.survey.flat.Prompt;
import net.scran24.user.client.survey.flat.PromptAvailabilityBasedSelectionManager;
import net.scran24.user.client.survey.flat.PromptRule;
import net.scran24.user.client.survey.flat.PromptUtil;
import net.scran24.user.client.survey.flat.RuleBasedPromptManager;
import net.scran24.user.client.survey.flat.Rules;
import net.scran24.user.client.survey.flat.Selection;
import net.scran24.user.client.survey.flat.SelectionRule;
import net.scran24.user.client.survey.flat.SelectionType;
import net.scran24.user.client.survey.flat.Survey;
import net.scran24.user.client.survey.flat.SurveyOperation;
import net.scran24.user.client.survey.flat.rules.AskForMealTime;
import net.scran24.user.client.survey.flat.rules.AskToLookupFood;
import net.scran24.user.client.survey.flat.rules.ChoosePortionSizeMethod;
import net.scran24.user.client.survey.flat.rules.InformFoodComplete;
import net.scran24.user.client.survey.flat.rules.SelectFoodForAssociatedPrompts;
import net.scran24.user.client.survey.flat.rules.SelectForPortionSize;
import net.scran24.user.client.survey.flat.rules.SelectIncompleteFreeEntryMeal;
import net.scran24.user.client.survey.flat.rules.SelectMealForReadyMeals;
import net.scran24.user.client.survey.flat.rules.SelectMealWithNoDrink;
import net.scran24.user.client.survey.flat.rules.SelectRawFood;
import net.scran24.user.client.survey.flat.rules.SelectUnconfirmedMeal;
import net.scran24.user.client.survey.flat.rules.ShowAssociatedFoodPrompt;
import net.scran24.user.client.survey.flat.rules.ShowBrandNamePrompt;
import net.scran24.user.client.survey.flat.rules.ShowCompoundFoodPrompt;
import net.scran24.user.client.survey.flat.rules.ShowDrinkReminderPrompt;
import net.scran24.user.client.survey.flat.rules.ShowEditMeal;
import net.scran24.user.client.survey.flat.rules.ShowEmptySurveyPrompt;
import net.scran24.user.client.survey.flat.rules.ShowEnergyValidationPrompt;
import net.scran24.user.client.survey.flat.rules.ShowReadyMealsPrompt;
import net.scran24.user.client.survey.flat.rules.ShowSameAsBeforePrompt;
import net.scran24.user.client.survey.flat.rules.ShowTimeGapPrompt;
import net.scran24.user.client.survey.flat.rules.SplitFood;
import net.scran24.user.client.survey.flat.rules.experimental.ShowNextPortionSizeStep;
import net.scran24.user.client.survey.portionsize.experimental.PortionSizeScriptManager;
import net.scran24.user.client.survey.prompts.MealOperation;
import net.scran24.user.client.survey.prompts.simple.RadioButtonPrompt;
import net.scran24.user.shared.FoodEntry;
import net.scran24.user.shared.Meal;
import net.scran24.user.shared.WithPriority;

import org.pcollections.client.HashTreePSet;
import org.pcollections.client.PSet;
import org.pcollections.client.PVector;
import org.pcollections.client.TreePVector;
import org.workcraft.gwt.shared.client.Function1;
import org.workcraft.gwt.shared.client.Option;
import org.workcraft.gwt.shared.client.Pair;

import com.google.gwt.safehtml.shared.SafeHtmlUtils;

public class YoungScot2014Scheme extends BasicScheme {
	final private static String FLAG_ENABLE_FINAL_RUN = "enable-final-run";
	
	final private static SurveyMessages surveyMessages = SurveyMessages.Util.getInstance();
	
	final private PVector<String> mealLocationOptions =
			TreePVector.<String>empty()
			.plus("Home")
			.plus("School")
			.plus("Fast food outlet e.g McDonalds")
			.plus("Café")
			.plus("Restaurant")
			.plus("Friend/Relatives")
			.plus("In transit (Walking, on train, on bus etc)");
	
	final private PVector<String> foodSourceOptions =
			TreePVector.<String>empty()
			.plus("Home")
			.plus("School")
			.plus("Convenience store")
			.plus("Supermarket")
			.plus("Fast food outlet e.g McDonalds")
			.plus("Café")
			.plus("Restaurant")
			.plus("Friend/Relatives");
	
	final private PSet<String> shopSources =
			HashTreePSet.<String>empty()
			.plus("Convenience store")
			.plus("Supermarket")
			.plus("Fast food outlet e.g McDonalds")
			.plus("Café")
			.plus("Restaurant");
			
	
	final private TreePVector<String> spendingOptions =
			TreePVector.<String>empty()
			.plus("Less than £0.50")
			.plus("£0.50 - £1.00")
			.plus("£1.01 - £2.00")
			.plus("£2.01 - £3.00")
			.plus("£3.01 - £4.00")
			.plus("£4.01 - £5.00")
			.plus("£5.01 - £6.00")
			.plus("More than £6.00");
	
	final private TreePVector<String> frequencyOptions = 
			TreePVector.<String>empty()
			.plus("Never")
			.plus("Once a week")
			.plus("2-4 times a week")
			.plus("Daily");
	
	final private TreePVector<String> reasonOptions = 
			TreePVector.<String>empty()
			.plus("Friends")
			.plus("Cost")
			.plus("Length of queues")
			.plus("Choice of food")
			.plus("Weather");
	
	final private TreePVector<String> freeMealsOptions =
			TreePVector.<String>empty()
			.plus("Yes")
			.plus("No");
	
	private boolean hasShopItems(Meal meal) {
		return exists(meal.foods, new Function1<FoodEntry, Boolean>() {
			@Override
			public Boolean apply(FoodEntry argument) {
				return shopSources.contains(argument.customData.get("foodSource"));
			}
		});
	}
	
	private boolean hasSchoolItems(Meal meal) {
		return exists(meal.foods, new Function1<FoodEntry, Boolean>() {
			@Override
			public Boolean apply(FoodEntry argument) {
				return argument.customData.get("foodSource").equals("School");
			}
		});
	}
	
	private boolean foodSourcesComplete(Meal meal) {
		return forall(meal.foods, new Function1<FoodEntry, Boolean>() {
			@Override
			public Boolean apply(FoodEntry argument) {
				return argument.customData.containsKey("foodSource");
			}
		});
	}
	
	private boolean frequencyComplete (Survey survey) {
		return 
				survey.customData.containsKey("shopFreq") &&
				survey.customData.containsKey("packFreq") &&
				survey.customData.containsKey("schoolLunchFreq") &&
				survey.customData.containsKey("homeFreq") &&
				survey.customData.containsKey("skipFreq") &&
				survey.customData.containsKey("workFreq");
	}
	
	private Rules rules(PortionSizeScriptManager scriptManager, CompoundFoodTemplateManager templateManager) {
		PromptRule<Meal, MealOperation> showMealLocationPrompt = new PromptRule<Meal, MealOperation>() {
			@Override
			public Option<Prompt<Meal, MealOperation>> apply(Meal state, SelectionType selectionType, PSet<String> surveyFlags) {
				if (state.customData.containsKey("mealLocation") || !state.encodingComplete() || !surveyFlags.contains(FLAG_ENABLE_FINAL_RUN))
					return Option.none();
				else
					return Option.some(PromptUtil.asMealPrompt(
							new RadioButtonPrompt(
									SafeHtmlUtils.fromSafeConstant("<p>Where did you eat your " + state.safeName() + "?</p>"),
									"MealLocationPrompt",
									mealLocationOptions,
									"Continue",
									"mealLocation", Option.some("Other")), new Function1<String, MealOperation>() {
										@Override
										public MealOperation apply(String location) {
											return MealOperation.setCustomDataField("mealLocation", location);
										}
							}));
			}
		};
		
		PromptRule<Meal, MealOperation> showShopPricePrompt = new PromptRule<Meal, MealOperation>() {
			@Override
			public Option<Prompt<Meal, MealOperation>> apply(Meal state, SelectionType selectionType, PSet<String> surveyFlags) {
				if (!foodSourcesComplete(state) || !hasShopItems(state) || state.customData.containsKey("shopSpending"))
					return Option.none();
				else
					return Option.some(PromptUtil.asMealPrompt(
							new RadioButtonPrompt(
									SafeHtmlUtils.fromSafeConstant("<p>How much did you spend on your " + state.safeName() + " at the shop or restaurant?</p>"),
									"ShopPricePrompt",
									spendingOptions,
									"Continue",
									"shopSpending", Option.<String>none()), new Function1<String, MealOperation>() {
										@Override
										public MealOperation apply(String spending) {
											return MealOperation.setCustomDataField("shopSpending", spending);
										}
							}));
			}
		};
		
		PromptRule<Meal, MealOperation> showSchoolPricePrompt = new PromptRule<Meal, MealOperation>() {
			@Override
			public Option<Prompt<Meal, MealOperation>> apply(Meal state, SelectionType selectionType, PSet<String> surveyFlags) {
				if (!foodSourcesComplete(state) || !hasSchoolItems(state) || state.customData.containsKey("schoolSpending"))
					return Option.none();
				else
					return Option.some(PromptUtil.asMealPrompt(
							new RadioButtonPrompt(
									SafeHtmlUtils.fromSafeConstant("<p>How much did you spend on your " + state.safeName() + " at school?</p>"),
									"SchoolPricePrompt",
									spendingOptions,
									"Continue",
									"schoolSpending", Option.<String>none()), new Function1<String, MealOperation>() {
										@Override
										public MealOperation apply(String spending) {
											return MealOperation.setCustomDataField("schoolSpending", spending);
										}
							}));
			}
		};
		
		PromptRule<Meal, MealOperation> showMealSourcePrompt = new PromptRule<Meal, MealOperation>() {
			@Override
			public Option<Prompt<Meal, MealOperation>> apply(Meal state, SelectionType selectionType, PSet<String> surveyFlags) {
				if (state.customData.containsKey("mealSource") || !state.encodingComplete() || !surveyFlags.contains(FLAG_ENABLE_FINAL_RUN))
					return Option.none();
				else
					return Option.some(PromptUtil.asMealPrompt(
							new RadioButtonPrompt(
									SafeHtmlUtils.fromSafeConstant("<p>Where did most of your food that you had for your <strong>" + state.safeName() + "</strong> come from?</p>"),
									"MealSourcePrompt",
									foodSourceOptions,
									"Continue",
									"mealSource", Option.some("Other")), new Function1<String, MealOperation>() {
										@Override
										public MealOperation apply(final String source) {
											return MealOperation.update(new Function1<Meal, Meal>() {
												@Override
												public Meal apply(Meal argument) {
													Meal withSource = argument.withCustomDataField("mealSource", source);
													
													if (argument.foods.size() == 1)
														return withSource.withFoods(map(argument.foods, new Function1<FoodEntry, FoodEntry>(){
															@Override
															public FoodEntry apply(FoodEntry argument) {
																return argument.withCustomDataField("foodSource", source);
															}
														}));
													else
														return withSource;
												}
											});
										}
							}));
			}
		};
		
		PromptRule<Meal, MealOperation> showFoodSourcePrompt = new PromptRule<Meal, MealOperation>() {
			@Override
			public Option<Prompt<Meal, MealOperation>> apply(Meal state, SelectionType selectionType, PSet<String> surveyFlags) {
				if (!state.customData.containsKey("mealSource") || !state.encodingComplete() || foodSourcesComplete(state))
					return Option.none();
					else 
						return Option.<Prompt<Meal, MealOperation>>some(new FoodSourcesPrompt(state, foodSourceOptions, state.customData.get("mealSource")));
			}
		};
		
		PromptRule<Survey, SurveyOperation> confirmCompletion = new PromptRule<Survey, SurveyOperation>() {
			@Override
			public Option<Prompt<Survey, SurveyOperation>> apply(Survey state, SelectionType selectionType, PSet<String> surveyFlags) {
				if (state.completionConfirmed() || !state.flags.contains(FLAG_ENABLE_FINAL_RUN))
					return Option.none();
				else
					return Option.<Prompt<Survey, SurveyOperation>>some(new YoungScotConfirmCompletionPrompt());
				}
		};
		
		
		return new Rules(
		// meal prompts
				TreePVector.<WithPriority<PromptRule<Meal, MealOperation>>> empty()
				.plus(AskForMealTime.withPriority(8))
				.plus(ShowEditMeal.withPriority(7))
				.plus(ShowDrinkReminderPrompt.withPriority(6))
				.plus(ShowReadyMealsPrompt.withPriority(5))
				.plus(new WithPriority<PromptRule<Meal, MealOperation>>(showMealSourcePrompt, 4))
				.plus(new WithPriority<PromptRule<Meal, MealOperation>>(showFoodSourcePrompt, 3))
				.plus(new WithPriority<PromptRule<Meal, MealOperation>>(showMealLocationPrompt, 2))
				.plus(new WithPriority<PromptRule<Meal, MealOperation>>(showShopPricePrompt, 1))
				.plus(new WithPriority<PromptRule<Meal, MealOperation>>(showSchoolPricePrompt, 0)),
				

				// food prompts
				TreePVector.<WithPriority<PromptRule<FoodEntry, FoodOperation>>> empty()
				.plus(ShowBrandNamePrompt.withPriority(-1))
				.plus(ShowNextPortionSizeStep.withPriority(scriptManager, 0))
				.plus(ChoosePortionSizeMethod.withPriority(1))				
				.plus(SplitFood.withPriority(3))
				.plus(InformFoodComplete.withPriority(-100)),

				// extended food propmts
				TreePVector.<WithPriority<PromptRule<Pair<FoodEntry, Meal>, MealOperation>>> empty()
				.plus(AskToLookupFood.withPriority(2, recipeManager))
				.plus(ShowSameAsBeforePrompt.withPriority(2, scriptManager, templateManager))
				.plus(ShowCompoundFoodPrompt.withPriority(0))
				.plus(ShowAssociatedFoodPrompt.withPriority(0))
				
				,
				// global prompts

				TreePVector.<WithPriority<PromptRule<Survey, SurveyOperation>>> empty()
				.plus(new WithPriority<PromptRule<Survey, SurveyOperation>>(confirmCompletion, 0))
				.plus(ShowEnergyValidationPrompt.withPriority(1, 500.0))
				.plus(ShowEmptySurveyPrompt.withPriority(1))
				.plus(ShowTimeGapPrompt.withPriority(2, 180, new Time(9,0), new Time(21,0)))
				
				,

				// selection rules
				TreePVector.<WithPriority<SelectionRule>> empty()
				.plus(SelectForPortionSize.withPriority(3))
				.plus(SelectRawFood.withPriority(2))
				.plus(SelectFoodForAssociatedPrompts.withPriority(1))
				.plus(SelectIncompleteFreeEntryMeal.withPriority(1))
				.plus(SelectMealWithNoDrink.withPriority(1))
				.plus(SelectUnconfirmedMeal.withPriority(1))
		    .plus(SelectMealForReadyMeals.withPriority(1)));
	}


	public YoungScot2014Scheme(final SurveyInterfaceManager interfaceManager) {
		super(interfaceManager);	
	}
	
	private boolean checkUserData() {
		Map<String, String> userData = CurrentUser.getUserInfo().userData;
		return userData.containsKey("age") && userData.containsKey("gender") && userData.containsKey("postCode") && userData.containsKey("schoolName") && userData.containsKey("townName");
	}
	
	private IntakeSurvey cachedSurveyPage = null;
	
	@Override
	public void showNextPage() {
		final Survey state = stateManager.getCurrentState(); 
		
		// Logger log = Logger.getLogger("showNextPage");
		// log.info(SurveyXmlSerialiser.toXml(state));
		
		if (!state.flags.contains(WelcomePage.FLAG_WELCOME_PAGE_SHOWN)) {
			interfaceManager.show(new WelcomePage(surveyMessages.welcomePage_welcomeText(), state));
		} else if (!checkUserData() && !state.flags.contains(UserDataQuestion.FLAG_SKIP_USERDATA_UPLOAD)) {
			interfaceManager.show(new UserDataQuestion(state));
		} else if (!state.completionConfirmed()) {

			if (state.flags.contains(Survey.FLAG_NO_MORE_PROMPTS) && !state.flags.contains(FLAG_ENABLE_FINAL_RUN))
				stateManager.updateState(state.clearFlag(Survey.FLAG_NO_MORE_PROMPTS).withFlag(FLAG_ENABLE_FINAL_RUN).withSelection(new Selection.SelectedMeal(0, SelectionType.AUTO_SELECTION)), false);

			if (cachedSurveyPage == null) {
				RuleBasedPromptManager promptManager = new RuleBasedPromptManager(rules(defaultScriptManager, defaultTemplateManager));
				PromptAvailabilityBasedSelectionManager selectionManager = new PromptAvailabilityBasedSelectionManager(promptManager);

				cachedSurveyPage = new IntakeSurvey(stateManager, promptManager, selectionManager, defaultScriptManager);
			}
			
			interfaceManager.show(cachedSurveyPage);
			
		}	else if (!state.customData.containsKey("lunchSpend")) {
			interfaceManager.show(new MultipleChoiceRadioButtonQuestion(state, 
					SafeHtmlUtils.fromSafeConstant("<p>How much do you usually spend on <strong>lunch</strong> on a school day?</p>"), 
					"Continue", 
					spendingOptions, "lunchSpend", Option.<String>none()));						
		} else if (!frequencyComplete(state)) {
			interfaceManager.show(new LunchFrequenciesQuestion(state, frequencyOptions));
		} else if (!state.customData.containsKey("reason")) {
			interfaceManager.show(new MultipleChoiceRadioButtonQuestion(state, 
					SafeHtmlUtils.fromSafeConstant("<p>What determines where you go for <strong>lunch</strong>?</p>"), 
					"Continue", 
					reasonOptions, "reason", Option.some("Other")));
		} else if (!state.customData.containsKey("freeMeals")) {
			interfaceManager.show(new MultipleChoiceRadioButtonQuestion(state, 
					SafeHtmlUtils.fromSafeConstant("<p>Are you entitled to free school meals?</p>"), 
					"Continue", 
					freeMealsOptions, "freeMeals", Option.<String>none()));
		}
		else {
			interfaceManager.show(new FlatFinalPage(surveyMessages.finalPage_text(), postProcess(state, basicPostProcess), log.log));
		}
	}
}