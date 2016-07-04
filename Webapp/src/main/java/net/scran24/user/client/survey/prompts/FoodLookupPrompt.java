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

import net.scran24.common.client.AsyncRequest;
import net.scran24.common.client.AsyncRequestAuthHandler;
import net.scran24.common.client.GoogleAnalytics;
import net.scran24.common.client.IEHack;
import net.scran24.common.client.LoadingPanel;
import net.scran24.common.client.WidgetFactory;
import net.scran24.user.client.ShepherdTour;
import net.scran24.user.client.services.FoodLookupServiceAsync;
import net.scran24.user.client.survey.FoodTemplates;
import net.scran24.user.client.survey.RecipeManager;
import net.scran24.user.client.survey.SurveyStageInterface;
import net.scran24.user.client.survey.SurveyStageInterface.Aligned;
import net.scran24.user.client.survey.flat.Prompt;
import net.scran24.user.client.survey.prompts.messages.HelpMessages;
import net.scran24.user.client.survey.prompts.messages.PromptMessages;
import net.scran24.user.shared.EncodedFood;
import net.scran24.user.shared.FoodData;
import net.scran24.user.shared.FoodEntry;
import net.scran24.user.shared.FoodLink;
import net.scran24.user.shared.Meal;
import net.scran24.user.shared.MissingFood;
import net.scran24.user.shared.MissingFoodDescription;
import net.scran24.user.shared.RawFood;
import net.scran24.user.shared.Recipe;
import net.scran24.user.shared.SpecialData;
import net.scran24.user.shared.TemplateFood;
import net.scran24.user.shared.UUID;
import net.scran24.user.shared.lookup.LookupResult;

import org.pcollections.client.PVector;
import org.pcollections.client.TreePVector;
import org.workcraft.gwt.shared.client.Callback;
import org.workcraft.gwt.shared.client.Callback1;
import org.workcraft.gwt.shared.client.Function1;
import org.workcraft.gwt.shared.client.Option;
import org.workcraft.gwt.shared.client.Pair;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public class FoodLookupPrompt implements Prompt<Pair<FoodEntry, Meal>, MealOperation> {
	private final static int MAX_RESULTS = 50;
	private final static PromptMessages messages = PromptMessages.Util.getInstance();
	private final static HelpMessages helpMessages = HelpMessages.Util.getInstance();

	private final FoodEntry food;
	private final Meal meal;
	private final RecipeManager recipeManager;

	private final String locale;

	public FoodLookupPrompt(final String locale, final FoodEntry food, final Meal meal, RecipeManager recipeManager) {
		this.locale = locale;
		this.food = food;
		this.meal = meal;
		this.recipeManager = recipeManager;
	}

	private class LookupInterface extends Aligned {
		private final FoodLookupServiceAsync lookupService = FoodLookupServiceAsync.Util.getInstance();
		private final TextBox searchText;
		private final Button searchButton;
		private final RecipeBrowser recipeBrowser;
		private final FoodBrowser foodBrowser;

		private String lastSearchTerm = "";

		private void showLoading(String description) {
			content.clear();
			content.add(new LoadingPanel(messages.foodLookup_loadingMessage(SafeHtmlUtils.htmlEscape(description))));
		}

		private void lookup(final String description) {
			showLoading(description);

			lastSearchTerm = description;

			final SafeHtml headerText = SafeHtmlUtils
					.fromSafeConstant(messages.foodLookup_searchResultsHeader(SafeHtmlUtils.htmlEscape(description)));

			AsyncRequestAuthHandler.execute(new AsyncRequest<LookupResult>() {
				@Override
				public void execute(AsyncCallback<LookupResult> callback) {

					if (food.customData.containsKey(RawFood.KEY_LIMIT_LOOKUP_TO_CATEGORY))
						lookupService.lookupInCategory(description, food.customData.get(RawFood.KEY_LIMIT_LOOKUP_TO_CATEGORY), locale,
								MAX_RESULTS, callback);
					else
						lookupService.lookup(description, locale, MAX_RESULTS, callback);
				}
			}, new AsyncCallback<LookupResult>() {
				@Override
				public void onFailure(Throwable caught) {
					showWithSearchHeader(headerText, WidgetFactory.createDefaultErrorMessage());
				}

				@Override
				public void onSuccess(LookupResult result) {
					recipeBrowser.lookup(description);
					foodBrowser.showLookupResult(result, messages.foodLookup_resultsDataSetName());

					FlowPanel div = new FlowPanel();

					div.add(recipeBrowser);
					div.add(foodBrowser);

					showWithSearchHeader(headerText, div);
				}
			});
		}

		private void showWithSearchHeader(SafeHtml headerText, Widget stuff) {
			final FlowPanel searchPanel = new FlowPanel();

			searchPanel.addStyleName("intake24-food-lookup-search-panel");

			final FlowPanel textboxContainer = new FlowPanel();
			textboxContainer.addStyleName("intake24-food-lookup-textbox-container");
			textboxContainer.add(searchText);

			searchPanel.add(searchButton);
			searchPanel.add(textboxContainer);

			final FlowPanel promptPanel = WidgetFactory.createPromptPanel(headerText, WidgetFactory.createHelpButton(new ClickHandler() {
				@Override
				public void onClick(ClickEvent arg0) {
					String promptType = FoodLookupPrompt.class.getSimpleName();
					GoogleAnalytics.trackHelpButtonClicked(promptType);
					ShepherdTour.startTour(buildShepherdTour(), promptType);
				}
			}));

			ShepherdTour.makeShepherdTarget(promptPanel);

			content.clear();
			content.add(promptPanel);
			content.add(searchPanel);
			content.add(stuff);

			IEHack.forceReflowDeferred();
		}

		LookupInterface(final Meal meal, final FoodEntry food, final Callback1<MealOperation> onComplete) {
			super(new FlowPanel(), HasHorizontalAlignment.ALIGN_LEFT, HasVerticalAlignment.ALIGN_TOP, SurveyStageInterface.DEFAULT_OPTIONS);

			searchText = new TextBox();
			searchText.setText(food.description());
			searchText.addStyleName("intake24-food-lookup-textbox");

			searchText.getElement().setId("intake24-food-lookup-textbox");
			ShepherdTour.makeShepherdTarget(searchText);

			searchText.addKeyPressHandler(new KeyPressHandler() {
				@Override
				public void onKeyPress(KeyPressEvent event) {
					if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ENTER)
						lookup(searchText.getText());
				}
			});

			searchButton = WidgetFactory.createButton(messages.foodLookup_searchButtonLabel(), new ClickHandler() {
				@Override
				public void onClick(ClickEvent arg0) {
					lookup(searchText.getText());
				}
			});

			searchButton.addStyleName("intake24-food-lookup-search-button");

			searchButton.getElement().setId("intake24-food-lookup-search-button");
			ShepherdTour.makeShepherdTarget(searchButton);

			Option<Pair<String, String>> limitToCategory;

			if (food.customData.containsKey(RawFood.KEY_LIMIT_LOOKUP_TO_CATEGORY))
				limitToCategory = Option.some(Pair.create(SpecialData.CATEGORY_CODE_RECIPE_INGREDIENTS, "All recipe ingredients"));
			else
				limitToCategory = Option.none();

			foodBrowser = new FoodBrowser(locale, new Callback1<FoodData>() {
				@Override
				public void call(final FoodData foodData) {
					// filter portion size methods if the food is an ingredient

					FoodData data = food.link.linkedTo.accept(new Option.Visitor<UUID, FoodData>() {
						@Override
						public FoodData visitSome(UUID id) {
							FoodEntry parentFood = meal.foods.get(meal.foodIndex(id));

							if (parentFood.isCompound())
								return foodData.withRecipePortionSizeMethods();
							else
								return foodData;
						}

						@Override
						public FoodData visitNone() {
							return foodData;
						}
					});

					onComplete.call(MealOperation.replaceFood(meal.foodIndex(food), new EncodedFood(data, food.link, lastSearchTerm)));

				}
			}, new Callback1<String>() {
				@Override
				public void call(String code) {
					if (code.equals(SpecialData.FOOD_CODE_SANDWICH))
						onComplete.call(MealOperation.replaceFood(meal.foodIndex(food),
								new TemplateFood(FoodLink.newUnlinked(), SafeHtmlUtils.htmlEscape("Sandwich"), false, FoodTemplates.sandwich)));
					else if (code.equals(SpecialData.FOOD_CODE_SALAD))
						onComplete.call(MealOperation.replaceFood(meal.foodIndex(food),
								new TemplateFood(FoodLink.newUnlinked(), SafeHtmlUtils.htmlEscape("Salad"), false, FoodTemplates.salad)));

				}
			}, new Callback() {
				@Override
				public void call() {
					MissingFood missingFood = new MissingFood(food.link, food.description(), food.isDrink(), Option.<MissingFoodDescription> none());
					onComplete.call(MealOperation.replaceFood(meal.foodIndex(food),
							food.link.linkedTo.isEmpty() ? missingFood : missingFood.withFlag(MissingFood.NOT_HOME_RECIPE_FLAG)));
				}

			}, Option.<SkipFoodHandler> none(), true, limitToCategory);

			recipeBrowser = new RecipeBrowser(new Callback1<Recipe>() {
				@Override
				public void call(final Recipe recipe) {
					onComplete.call(MealOperation.update(new Function1<Meal, Meal>() {
						@Override
						public Meal apply(Meal meal) {

							Pair<TemplateFood, PVector<FoodEntry>> cloned = TemplateFood.clone(Pair.create(recipe.mainFood, recipe.ingredients));

							Meal result = meal.updateFood(meal.foodIndex(food), cloned.left);

							for (FoodEntry e : cloned.right)
								result = result.plusFood(e);

							return result;
						}
					}));
				}
			}, recipeManager);
		}

		private PVector<ShepherdTour.Step> buildShepherdTour() {
			TreePVector<ShepherdTour.Step> result = TreePVector.empty();

			result = result.plus(
					new ShepherdTour.Step("searchText", "#intake24-food-lookup-textbox", helpMessages.foodLookup_textboxTitle(), helpMessages
							.foodLookup_textboxDescription())).plus(
					new ShepherdTour.Step("searchButton", "#intake24-food-lookup-search-button", helpMessages.foodLookup_searchButtonTitle(),
							helpMessages.foodLookup_searchButtonDescription(), "top right", "bottom right"));

			result = result.plusAll(recipeBrowser.getShepherdTourSteps()).plusAll(foodBrowser.getShepherdTourSteps());

			return result;
		}
	}

	@Override
	public SurveyStageInterface getInterface(final Callback1<MealOperation> onComplete,
			Callback1<Function1<Pair<FoodEntry, Meal>, Pair<FoodEntry, Meal>>> updateIntermediateState) {
		LookupInterface ui = new LookupInterface(meal, food, onComplete);
		ui.lookup(food.description());
		return ui;
	}

	@Override
	public String toString() {
		return "Food lookup";
	}
}
