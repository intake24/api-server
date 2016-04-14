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

import java.util.ArrayList;
import java.util.List;

import net.scran24.common.client.AsyncRequest;
import net.scran24.common.client.AsyncRequestAuthHandler;
import net.scran24.common.client.GoogleAnalytics;
import net.scran24.common.client.IEHack;
import net.scran24.common.client.LoadingPanel;
import net.scran24.common.client.WidgetFactory;
import net.scran24.user.client.ShepherdTour;
import net.scran24.user.client.services.FoodLookupServiceAsync;
import net.scran24.user.client.survey.flat.PromptInterfaceManager;
import net.scran24.user.client.survey.prompts.messages.HelpMessages;
import net.scran24.user.client.survey.prompts.messages.PromptMessages;
import net.scran24.user.shared.CategoryHeader;
import net.scran24.user.shared.FoodData;
import net.scran24.user.shared.FoodHeader;
import net.scran24.user.shared.SpecialData;
import net.scran24.user.shared.lookup.LookupResult;

import org.pcollections.client.ConsPStack;
import org.pcollections.client.PStack;
import org.pcollections.client.PVector;
import org.pcollections.client.TreePVector;
import org.workcraft.gwt.shared.client.Callback;
import org.workcraft.gwt.shared.client.Callback1;
import org.workcraft.gwt.shared.client.Option;
import org.workcraft.gwt.shared.client.Pair;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;

public class FoodBrowser extends Composite {
	private final static HelpMessages helpMessages = HelpMessages.Util.getInstance();

	private class HistoryState {
		final LookupResult result;
		final String dataSetName;
		final String foodHeader;
		final String categoryHeader;

		public HistoryState(LookupResult result, String dataSetName, String foodHeader, String categoryHeader) {
			this.result = result;
			this.dataSetName = dataSetName;
			this.foodHeader = foodHeader;
			this.categoryHeader = categoryHeader;
		}
	};

	private final FoodLookupServiceAsync lookupService = FoodLookupServiceAsync.Util.getInstance();
	private final PromptMessages messages = GWT.create(PromptMessages.class);

	private final FlowPanel contents = new FlowPanel();
	private final Callback1<FoodData> onFoodChosen;
	private final Callback1<String> onSpecialFoodChosen;
	private final Callback onMissingFoodReported;
	private final Option<SkipFoodHandler> skipFoodHandler;
	private final boolean allowBrowsingAllFoods;
	private final Option<Pair<String, String>> limitBrowseAllCategory;

	private Button browseAllFoodsButton;
	private Button cantFindButton;
	private Button skipFoodButton;
	private Button missingFoodButton;
	private Button tryAgainButton;
	private FlowPanel foodsContainer;
	private FlowPanel categoriesContainer;
	private HTMLPanel cantFindPopupPrompt;

	private final String currentLocale = "en_GB"; // LocaleInfo.getCurrentLocale().getLocaleName();

	private PStack<HistoryState> browseHistory = ConsPStack.<HistoryState> empty();

	public FoodBrowser(final Callback1<FoodData> onFoodChosen, final Callback1<String> onSpecialFoodChosen, final Callback onMissingFoodReported,
			final Option<SkipFoodHandler> skipFoodHandler, boolean allowBrowsingAllFoods, Option<Pair<String, String>> limitBrowseAllCategory) {

		contents.addStyleName("intake24-food-browser");

		this.onFoodChosen = onFoodChosen;
		this.onSpecialFoodChosen = onSpecialFoodChosen;
		this.allowBrowsingAllFoods = allowBrowsingAllFoods;
		this.onMissingFoodReported = onMissingFoodReported;
		this.skipFoodHandler = skipFoodHandler;
		this.limitBrowseAllCategory = limitBrowseAllCategory;

		initWidget(contents);
	}

	private void pushHistory(LookupResult result, String name, String foodHeader, String categoryHeader) {
		browseHistory = browseHistory.plus(new HistoryState(result, name, foodHeader, categoryHeader));
	}

	private Widget createFoodButton(final FoodHeader foodHeader) {
		String description;

		if (foodHeader.code.equals(SpecialData.FOOD_CODE_SANDWICH))
			description = messages.foodBrowser_homemadeSandwich();
		else if (foodHeader.code.equals(SpecialData.FOOD_CODE_SALAD))
			description = messages.foodBrowser_homemadeSalad();
		else
			description = foodHeader.description();

		Label item = new Label(description);
		item.addStyleName("intake24-food-browser-food");

		if (foodHeader.code.equals(SpecialData.FOOD_CODE_SANDWICH))
			item.addStyleName("intake24-food-browser-sandwich-wizard");
		else if (foodHeader.code.equals(SpecialData.FOOD_CODE_SALAD))
			item.addStyleName("intake24-food-browser-salad-wizard");

		item.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent arg0) {
				if (foodHeader.code.equals(SpecialData.FOOD_CODE_SANDWICH) || foodHeader.code.equals(SpecialData.FOOD_CODE_SALAD)) {
					onSpecialFoodChosen.call(foodHeader.code);
				} else
					lookupService.getFoodData(foodHeader.code, currentLocale, new AsyncCallback<FoodData>() {
						@Override
						public void onFailure(Throwable caught) {
							contents.clear();
							contents.add(WidgetFactory.createDefaultErrorMessage());
						}

						@Override
						public void onSuccess(final FoodData result) {
							onFoodChosen.call(result);
						}
					});
			}
		});

		return item;
	}

	private FlowPanel historyBackLink() {
		FlowPanel p = new FlowPanel();
		p.addStyleName("intake24-food-browser-back-link-container");

		if (!browseHistory.isEmpty()) {
			final HistoryState parentResult = browseHistory.get(0);

			Anchor back = new Anchor(messages.foodBrowser_backToParent(parentResult.dataSetName.toLowerCase()));
			back.addClickHandler(new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					browseHistory = browseHistory.minus(0);
					show(parentResult.result, parentResult.dataSetName, parentResult.foodHeader, parentResult.categoryHeader);
				}
			});

			back.addStyleName("intake24-food-browser-back-link");
			p.add(back);
		}

		return p;
	}

	private void show(final LookupResult result, final String resultName, final String foodHeader, final String categoryHeader) {
		final FlowPanel ui = new FlowPanel();
		ui.addStyleName("intake24-food-browser-ui");

		ui.add(historyBackLink());

		if (!result.foods.isEmpty()) {
			foodsContainer = new FlowPanel();
			foodsContainer.addStyleName("intake24-food-browser-foods-container");
			foodsContainer.getElement().setId("intake24-food-browser-foods-container");

			for (final FoodHeader food : result.foods)
				if (food.code.equals(SpecialData.FOOD_CODE_SANDWICH) || food.code.equals(SpecialData.FOOD_CODE_SALAD))
					foodsContainer.add(createFoodButton(food));

			HTMLPanel header = new HTMLPanel("h2", foodHeader);
			foodsContainer.add(header);

			for (final FoodHeader food : result.foods)
				if (!(food.code.equals(SpecialData.FOOD_CODE_SANDWICH) || food.code.equals(SpecialData.FOOD_CODE_SALAD)))
					foodsContainer.add(createFoodButton(food));

			ui.add(foodsContainer);
		} else {
			foodsContainer = null;
		}

		if (!result.categories.isEmpty()) {
			categoriesContainer = new FlowPanel();
			categoriesContainer.addStyleName("intake24-food-browser-categories-container");
			categoriesContainer.getElement().setId("intake24-food-browser-categories-container");
			HTMLPanel header = new HTMLPanel("h2", categoryHeader);
			categoriesContainer.add(header);

			for (final CategoryHeader categoryData : result.categories) {
				Label item = new Label(categoryData.description());
				item.addStyleName("intake24-food-browser-category");
				item.addClickHandler(new ClickHandler() {
					@Override
					public void onClick(ClickEvent arg0) {
						pushHistory(result, resultName, foodHeader, categoryHeader);
						browse(categoryData.code, categoryData.description());
					}
				});
				categoriesContainer.add(item);
			}

			ui.add(categoriesContainer);
		} else {
			categoriesContainer = null;
		}

		if (result.categories.isEmpty() && result.foods.isEmpty()) {
			FlowPanel div = new FlowPanel();
			div.addStyleName("intake24-food-lookup-no-results");
			div.add(new HTMLPanel(messages.foodBrowser_searchResultsEmpty()));
			ui.add(div);
		}

		FlowPanel div = new FlowPanel();
		div.addStyleName("intake24-food-browser-browse-all-container");

		final Panel buttonsPanel = WidgetFactory.createButtonsPanel();
		div.add(buttonsPanel);

		if (allowBrowsingAllFoods) {
			browseAllFoodsButton = WidgetFactory.createButton(messages.foodBrowser_browseAllFoodsLabel());
			browseAllFoodsButton.addStyleName("intake24-food-browser-browse-all-button");
			browseAllFoodsButton.getElement().setId("intake24-food-browser-browse-all-button");
			browseAllFoodsButton.addClickHandler(new ClickHandler() {
				@Override
				public void onClick(ClickEvent arg0) {
					pushHistory(result, resultName, foodHeader, categoryHeader);

					limitBrowseAllCategory.accept(new Option.SideEffectVisitor<Pair<String, String>>() {
						@Override
						public void visitSome(Pair<String, String> item) {
							browse(item.left, item.right);
						}

						@Override
						public void visitNone() {
							browseAll();
						}
					});

				}
			});

			buttonsPanel.add(browseAllFoodsButton);
		} else {
			browseAllFoodsButton = null;
		}

		cantFindButton = WidgetFactory.createButton(messages.foodBrowser_cantFindButtonLabel());
		cantFindButton.getElement().setId("intake24-food-browser-cant-find-button");

		tryAgainButton = null;
		missingFoodButton = null;
		cantFindPopupPrompt = null;

		cantFindButton.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent arg0) {

				cantFindButton.setEnabled(false);

				final FlowPanel fadeDiv = new FlowPanel();
				fadeDiv.addStyleName("intake24-food-browser-overlay");

				final FlowPanel popupDiv = new FlowPanel();
				popupDiv.addStyleName("intake24-food-browser-popup");
				popupDiv.getElement().setId("cant-find-food-popup");

				cantFindPopupPrompt = new HTMLPanel(allowBrowsingAllFoods ? SafeHtmlUtils.fromSafeConstant(messages
						.foodBrowser_cantFindFullPopupContents()) : SafeHtmlUtils.fromSafeConstant(messages
						.foodBrowser_cantFindBrowseOnlyPopupContents()));

				tryAgainButton = WidgetFactory.createButton(messages.foodBrowser_cantFindTryAgainButtonLabel(), new ClickHandler() {
					@Override
					public void onClick(ClickEvent event) {
						cantFindButton.setEnabled(true);
						popupDiv.removeFromParent();

						PromptInterfaceManager.scrollPromptIntoView();

						tryAgainButton = null;
						missingFoodButton = null;
						cantFindPopupPrompt = null;

						// fadeDiv.removeFromParent();
					}
				});

				tryAgainButton.getElement().setId("intake24-food-browser-try-again-button");

				missingFoodButton = WidgetFactory.createButton(messages.foodBrowser_reportMissingFoodButtonLabel(), new ClickHandler() {

					@Override
					public void onClick(ClickEvent event) {
						GoogleAnalytics.trackMissingFoodReported();
						onMissingFoodReported.call();

					}
				});

				missingFoodButton.getElement().setId("intake24-food-browser-missing-food-button");

				popupDiv.add(cantFindPopupPrompt);
				popupDiv.add(WidgetFactory.createButtonsPanel(tryAgainButton, missingFoodButton));

				// ui.add(fadeDiv);
				ui.add(popupDiv);

				Scheduler.get().scheduleDeferred(new ScheduledCommand() {

					public native void scrollIntoView() /*-{
		$wnd.$('html, body').animate({
			scrollTop : $wnd.$("#cant-find-food-popup").offset().top
		}, 500);
	}-*/;

					@Override
					public void execute() {
						scrollIntoView();
					}
				});
			}
		});

		skipFoodHandler.accept(new Option.SideEffectVisitor<SkipFoodHandler>() {

			@Override
			public void visitSome(final SkipFoodHandler handler) {
				skipFoodButton = WidgetFactory.createButton(handler.skipButtonLabel, new ClickHandler() {
					@Override
					public void onClick(ClickEvent event) {
						handler.onFoodSkipped.call();

					}
				});

				skipFoodButton.getElement().setId("intake24-food-browser-skip-food-button");

				buttonsPanel.add(skipFoodButton);
			}

			@Override
			public void visitNone() {
				skipFoodButton = null;
			}
		});

		buttonsPanel.add(cantFindButton);

		ui.add(div);

		contents.clear();
		contents.add(ui);

		IEHack.forceReflowDeferred();
	}

	public void showLookupResult(final LookupResult data, final String dataSetName) {
		show(data, dataSetName, messages.foodBrowser_matchingFoodsHeader(), messages.foodBrowser_matchingCategoriesHeader());
	}

	public void browse(final String categoryCode, final String dataSetName, final String foodsHeader, final String categoryHeader) {
		contents.clear();
		contents.add(new LoadingPanel(messages.foodBrowser_loadingMessage()));

		AsyncRequestAuthHandler.execute(new AsyncRequest<LookupResult>() {
			@Override
			public void execute(AsyncCallback<LookupResult> callback) {
				lookupService.browseCategory(categoryCode, currentLocale, callback);
			}
		}, new AsyncCallback<LookupResult>() {
			@Override
			public void onFailure(Throwable caught) {
				contents.clear();
				contents.add(WidgetFactory.createDefaultErrorMessage());

				if (!browseHistory.isEmpty())
					contents.add(historyBackLink());
				else
					contents.add(WidgetFactory.createBackLink());
			}

			@Override
			public void onSuccess(LookupResult result) {
				show(result, dataSetName, foodsHeader, categoryHeader);
			}
		});
	}

	public void browse(final String categoryCode, final String dataSetName) {
		browse(categoryCode, dataSetName, messages.foodBrowser_browseFoodsHeader(), messages.foodBrowser_browseCategoriesHeader());
	}

	private void browseAll() {
		contents.clear();
		contents.add(new LoadingPanel(messages.foodBrowser_loadingMessage()));

		AsyncRequestAuthHandler.execute(new AsyncRequest<List<CategoryHeader>>() {
			@Override
			public void execute(AsyncCallback<List<CategoryHeader>> callback) {
				lookupService.getRootCategories(currentLocale, callback);
			}
		}, new AsyncCallback<List<CategoryHeader>>() {
			@Override
			public void onFailure(Throwable caught) {
				contents.clear();
				contents.add(WidgetFactory.createDefaultErrorMessage());
				contents.add(historyBackLink());
			}

			@Override
			public void onSuccess(List<CategoryHeader> result) {
				show(new LookupResult(new ArrayList<FoodHeader>(), result), messages.foodBrowser_allFoodsDataSetName(), "",
						messages.foodBrowser_allCategoriesHeader());
			}
		});
	}

	public PVector<ShepherdTour.Step> getShepherdTourSteps() {

		PVector<ShepherdTour.Step> result = TreePVector.<ShepherdTour.Step> empty();

		// Optional foods list
		if (foodsContainer != null) {
			ShepherdTour.makeShepherdTarget(foodsContainer);
			result = result.plus(new ShepherdTour.Step("foodBrowser_foods", "#intake24-food-browser-foods-container", helpMessages
					.foodBrowser_foodsTitle(), helpMessages.foodBrowser_foodsDescription()));
		}

		// Optional categories list

		if (categoriesContainer != null) {
			ShepherdTour.makeShepherdTarget(categoriesContainer);
			result = result.plus(new ShepherdTour.Step("foodBrowser_categories", "#intake24-food-browser-categories-container", helpMessages
					.foodBrowser_categoriesTitle(), helpMessages.foodBrowser_categoriesDescription()));
		}

		// Optional browse all foods button

		if (browseAllFoodsButton != null) {
			ShepherdTour.makeShepherdTarget(browseAllFoodsButton);
			result = result.plus(new ShepherdTour.Step("foodBrowser_browseAllButton", "#intake24-food-browser-browse-all-button", helpMessages
					.foodBrowser_browseAllTitle(), helpMessages.foodBrowser_browseAllDescription()));
		}

		// Optional skip food button

		if (skipFoodButton != null) {
			ShepherdTour.makeShepherdTarget(skipFoodButton);
			result = result.plus(new ShepherdTour.Step("foodBrowser_skipFoodButton", "#intake24-food-browser-skip-food-button", helpMessages
					.foodBrowser_skipButtonTitle(), helpMessages.foodBrowser_skipButtonDescription()));
		}

		// Can't find button is always there

		ShepherdTour.makeShepherdTarget(cantFindButton);
		result = result.plus(new ShepherdTour.Step("foodBrowser_cantFindButton", "#intake24-food-browser-cant-find-button", helpMessages
				.foodBrowser_cantFindButtonTitle(), helpMessages.foodBrowser_cantFindButtonDescription()));

		// Try again and missing food buttons are there when
		// "I can't find my food" button has been clicked

		if (tryAgainButton != null) {
			ShepherdTour.makeShepherdTarget(tryAgainButton);
			result = result.plus(new ShepherdTour.Step("foodBrowser_tryAgainButton", "#intake24-food-browser-try-again-button", helpMessages
					.foodBrowser_tryAgainButtonTitle(), helpMessages.foodBrowser_tryAgainButtonDescription()));
		}

		if (cantFindPopupPrompt != null) {
			ShepherdTour.makeShepherdTarget(cantFindPopupPrompt);
		}

		if (missingFoodButton != null) {
			ShepherdTour.makeShepherdTarget(missingFoodButton);
			result = result.plus(new ShepherdTour.Step("foodBrowser_tryAgainButton", "#intake24-food-browser-missing-food-button", helpMessages
					.foodBrowser_missingFoodButtonTitle(), helpMessages.foodBrowser_missingFoodButtonDescription()));
		}

		return result;

	}
}