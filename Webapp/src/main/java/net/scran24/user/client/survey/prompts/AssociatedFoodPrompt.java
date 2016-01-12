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
import java.util.HashMap;
import java.util.List;

import net.scran24.common.client.AsyncRequest;
import net.scran24.common.client.AsyncRequestAuthHandler;
import net.scran24.common.client.GoogleAnalytics;
import net.scran24.common.client.LoadingPanel;
import net.scran24.common.client.WidgetFactory;
import net.scran24.datastore.shared.CompletedPortionSize;
import net.scran24.user.client.ShepherdTour;
import net.scran24.user.client.services.FoodLookupServiceAsync;
import net.scran24.user.client.survey.SurveyStageInterface;
import net.scran24.user.client.survey.flat.Prompt;
import net.scran24.user.client.survey.flat.PromptUtil;
import net.scran24.user.client.survey.portionsize.experimental.PortionSize;
import net.scran24.user.client.survey.prompts.messages.HelpMessages;
import net.scran24.user.client.survey.prompts.messages.PromptMessages;
import net.scran24.user.shared.CompoundFood;
import net.scran24.user.shared.TemplateFood;
import net.scran24.user.shared.EncodedFood;
import net.scran24.user.shared.FoodData;
import net.scran24.user.shared.FoodEntry;
import net.scran24.user.shared.FoodLink;
import net.scran24.user.shared.FoodPrompt;
import net.scran24.user.shared.Meal;
import net.scran24.user.shared.MissingFood;
import net.scran24.user.shared.MissingFoodDescription;
import net.scran24.user.shared.RawFood;
import net.scran24.user.shared.lookup.PortionSizeMethod;

import org.pcollections.client.PVector;
import org.pcollections.client.TreePVector;
import org.workcraft.gwt.shared.client.Callback;
import org.workcraft.gwt.shared.client.Callback1;
import org.workcraft.gwt.shared.client.CollectionUtils;
import org.workcraft.gwt.shared.client.Either;
import org.workcraft.gwt.shared.client.Function1;
import org.workcraft.gwt.shared.client.Option;
import org.workcraft.gwt.shared.client.Pair;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Panel;

public class AssociatedFoodPrompt implements Prompt<Pair<FoodEntry, Meal>, MealOperation> {
	private final static PromptMessages messages = PromptMessages.Util.getInstance();
	private final static HelpMessages helpMessages = HelpMessages.Util.getInstance();

	private final FoodLookupServiceAsync lookupService = FoodLookupServiceAsync.Util.getInstance();

	private final Pair<FoodEntry, Meal> pair;
	private final int foodIndex;
	private final int promptIndex;
	private FlowPanel interf;
	private Panel buttonsPanel;

	private PVector<ShepherdTour.Step> tour;

	private FoodBrowser foodBrowser;
	private boolean isInBrowserMode = false;
	private final String currentLocale = "en_GB"; // LocaleInfo.getCurrentLocale().getLocaleName();

	public AssociatedFoodPrompt(final Pair<FoodEntry, Meal> pair, final int foodIndex, final int promptIndex) {
		this.pair = pair;
		this.foodIndex = foodIndex;
		this.promptIndex = promptIndex;
	}

	private Option<String> getParamValue(EncodedFood food, final String id) {
		return CollectionUtils.flattenOption(food.portionSize.map(new Function1<Either<PortionSize, CompletedPortionSize>, Option<String>>() {
			@Override
			public Option<String> apply(Either<PortionSize, CompletedPortionSize> argument) {
				return argument.accept(new Either.Visitor<PortionSize, CompletedPortionSize, Option<String>>() {
					@Override
					public Option<String> visitRight(CompletedPortionSize value) {
						if (value.data.containsKey(id))
							return Option.some(value.data.get(id));
						else
							return Option.none();
					}

					@Override
					public Option<String> visitLeft(PortionSize value) {
						if (value.data.containsKey(id))
							return Option.some(value.data.get(id));
						else
							return Option.none();
					}
				});
			}
		}));
	}

	private List<PortionSizeMethod> appendPotionSizeParameter(List<PortionSizeMethod> methods, String id, String value) {
		ArrayList<PortionSizeMethod> result = new ArrayList<PortionSizeMethod>();
		for (PortionSizeMethod m : methods) {
			HashMap<String, String> parameters = new HashMap<String, String>();
			parameters.putAll(m.params);
			parameters.put(id, value);
			result.add(new PortionSizeMethod(m.name, m.description, m.imageUrl, m.useForRecipes, parameters));
		}
		return result;
	}

	private Meal linkAssociatedFood(Meal meal, FoodEntry forFood, final FoodEntry assocFood, boolean linkAsMain) {
		if (linkAsMain) {
			final int forIndex = meal.foodIndex(forFood);
			final int assocIndex = meal.foodIndex(assocFood);

			Meal result = meal.updateFood(assocIndex, forFood).updateFood(forIndex, assocFood);
			final PVector<FoodEntry> foodsToRelink = Meal.linkedFoods(result.foods, forFood).plus(0, forFood);

			for (FoodEntry e : foodsToRelink) {
				int index = result.foodIndex(e);
				result = result.minusFood(index).plusFood(e.relink(FoodLink.newLinked(assocFood.link.id)));
			}

			return result;
		} else {
			final int index = meal.foodIndex(assocFood);
			return meal.minusFood(index).plusFood(assocFood.relink(FoodLink.newLinked(forFood.link.id)));
		}
	}

	@Override
	public SurveyStageInterface getInterface(final Callback1<MealOperation> onComplete,
			Callback1<Function1<Pair<FoodEntry, Meal>, Pair<FoodEntry, Meal>>> updateIntermediateState) {
		final EncodedFood food = (EncodedFood) pair.left;
		final FoodPrompt prompt = food.enabledPrompts.get(promptIndex);

		final FlowPanel content = new FlowPanel();
		PromptUtil.addBackLink(content);
		final Panel promptPanel = WidgetFactory.createPromptPanel(
				SafeHtmlUtils.fromSafeConstant("<p>" + SafeHtmlUtils.htmlEscape(prompt.text) + "</p>"),
				WidgetFactory.createHelpButton(new ClickHandler() {
					@Override
					public void onClick(ClickEvent arg0) {
						String promptType = AssociatedFoodPrompt.class.getSimpleName();
						GoogleAnalytics.trackHelpButtonClicked(promptType);
						ShepherdTour.startTour(getShepherdTourSteps(), promptType);
					}
				}));
		content.add(promptPanel);
		ShepherdTour.makeShepherdTarget(promptPanel);

		final Callback1<FoodData> addNewFood = new Callback1<FoodData>() {
			@Override
			public void call(final FoodData result) {
				onComplete.call(MealOperation.update(new Function1<Meal, Meal>() {
					@Override
					public Meal apply(final Meal meal) {
						// Special case for cereal:
						// if a "milk on cereal" food is linked to a cereal food
						// copy bowl type from the parent food
						Option<String> bowl_id = getParamValue(food, "bowl");

						FoodData foodData = bowl_id.accept(new Option.Visitor<String, FoodData>() {
							@Override
							public FoodData visitSome(String bowl_id) {
								return result.withPortionSizeMethods(appendPotionSizeParameter(result.portionSizeMethods, "bowl", bowl_id));
							}

							@Override
							public FoodData visitNone() {
								return result;
							}
						});

						EncodedFood assocFood = new EncodedFood(foodData, FoodLink.newUnlinked(), "associated food prompt");

						return linkAssociatedFood(meal.plusFood(assocFood), food, assocFood, prompt.linkAsMain);
					};
				}));
			}
		};

		final Callback addMissingFood = new Callback() {
			@Override
			public void call() {
				onComplete.call(MealOperation.update(new Function1<Meal, Meal>() {
					@Override
					public Meal apply(final Meal meal) {
						FoodEntry missingFood = new MissingFood(FoodLink.newUnlinked(), prompt.genericName.substring(0, 1).toUpperCase()
								+ prompt.genericName.substring(1), false, Option.<MissingFoodDescription> none()).withCustomDataField(
								MissingFood.KEY_ASSOC_FOOD_NAME, food.description()).withCustomDataField(MissingFood.KEY_ASSOC_FOOD_CATEGORY,
								prompt.code);

						return linkAssociatedFood(meal.plusFood(missingFood), food, missingFood, prompt.linkAsMain);
					}
				}));
			}
		};

		final Callback1<FoodEntry> addExistingFood = new Callback1<FoodEntry>() {
			@Override
			public void call(final FoodEntry existing) {
				onComplete.call(MealOperation.update(new Function1<Meal, Meal>() {
					@Override
					public Meal apply(final Meal meal) {
						return linkAssociatedFood(meal, food, existing, prompt.linkAsMain);
					};
				}));
			}
		};

		foodBrowser = new FoodBrowser(new Callback1<FoodData>() {
			@Override
			public void call(FoodData result) {
				addNewFood.call(result);
			}
		}, new Callback1<String>() {
			@Override
			public void call(String code) {
				throw new RuntimeException("Special foods are not allowed as associated foods");
			}
		}, new Callback() {
			@Override
			public void call() {
				addMissingFood.call();
			}
		}, Option.<SkipFoodHandler> none(), false, Option.<Pair<String, String>> none());

		Button no = WidgetFactory.createButton(messages.assocFoods_noButtonLabel(), new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				onComplete.call(MealOperation.updateEncodedFood(foodIndex, new Function1<EncodedFood, EncodedFood>() {
					@Override
					public EncodedFood apply(EncodedFood argument) {
						return argument.minusPrompt(promptIndex);
					}
				}));
			}
		});

		Button yes = WidgetFactory.createButton(messages.assocFoods_yesButtonLabel(), new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				if (prompt.isCategoryCode) {
					content.clear();
					PromptUtil.addBackLink(content);
					content.add(promptPanel);
					content.add(new HTMLPanel(SafeHtmlUtils.fromSafeConstant(messages.assocFoods_specificFoodPrompt())));
					content.add(interf);

					content.add(foodBrowser);
					isInBrowserMode = true;

					foodBrowser.browse(prompt.code, messages.assocFoods_allFoodsDataSetName());
				} else {
					content.clear();
					content.add(new LoadingPanel(messages.foodBrowser_loadingMessage()));

					AsyncRequestAuthHandler.execute(new AsyncRequest<FoodData>() {
						@Override
						public void execute(AsyncCallback<FoodData> callback) {
							lookupService.getFoodData(prompt.code, currentLocale, callback);
						}
					}, new AsyncCallback<FoodData>() {
						@Override
						public void onFailure(Throwable caught) {
							content.clear();
							content.add(WidgetFactory.createDefaultErrorMessage());
							content.add(WidgetFactory.createBackLink());
						}

						@Override
						public void onSuccess(FoodData result) {
							addNewFood.call(result);
						}
					});
				}
			}
		});

		yes.getElement().setId("intake24-assoc-food-yes-button");

		final int existingIndex = CollectionUtils.indexOf(pair.right.foods, new Function1<FoodEntry, Boolean>() {
			@Override
			public Boolean apply(FoodEntry argument) {
				return argument.accept(new FoodEntry.Visitor<Boolean>() {
					@Override
					public Boolean visitRaw(RawFood food) {
						return false;
					}

					@Override
					public Boolean visitEncoded(EncodedFood food) {
						// don't suggest foods that are already linked to other
						// foods
						if (food.link.isLinked())
							return false;
						// don't suggest linking the food to itself
						else if (food.link.id.equals(pair.left.link.id))
							return false;
						// don't suggest if the food has foods linked to it
						else if (!Meal.linkedFoods(pair.right.foods, food).isEmpty())
							return false;
						else if (prompt.isCategoryCode)
							return food.isInCategory(prompt.code);
						else
							return food.data.code.equals(prompt.code);
					}

					@Override
					public Boolean visitTemplate(TemplateFood food) {
						return false;
					}

					@Override
					public Boolean visitMissing(MissingFood food) {
						return false;
					}

					@Override
					public Boolean visitCompound(CompoundFood food) {
						return false;
					}
				});
			}
		});

		no.getElement().setId("intake24-assoc-food-no-button");

		tour = TreePVector
				.<ShepherdTour.Step> empty()
				.plus(new ShepherdTour.Step("noButton", "#intake24-assoc-food-no-button", helpMessages.assocFood_noButtonTitle(), helpMessages
						.assocFood_noButtonDescription()))
				.plus(new ShepherdTour.Step("yesButton", "#intake24-assoc-food-yes-button", helpMessages.assocFood_yesButtonTitle(), helpMessages
						.assocFood_yesButtonDescription()));

		if (existingIndex != -1) {
			Button yesExisting = WidgetFactory.createButton(messages.assocFoods_alreadyEntered(), new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					addExistingFood.call(pair.right.foods.get(existingIndex));
				}
			});

			yesExisting.getElement().setId("intake24-assoc-food-yes-existing-button");

			tour = tour.plus(new ShepherdTour.Step("yesButton", "#intake24-assoc-food-yes-existing-button", helpMessages
					.assocFood_yesExistingButtonTitle(), helpMessages.assocFood_yesExistingButtonDescription(), "top right", "bottom right"));

			ShepherdTour.makeShepherdTarget(yesExisting);

			buttonsPanel = WidgetFactory.createButtonsPanel(no, yes, yesExisting);
		} else {
			buttonsPanel = WidgetFactory.createButtonsPanel(no, yes);
		}

		content.add(buttonsPanel);

		ShepherdTour.makeShepherdTarget(yes, no);

		interf = new FlowPanel();

		return new SurveyStageInterface.Aligned(content, HasHorizontalAlignment.ALIGN_LEFT, HasVerticalAlignment.ALIGN_TOP,
				SurveyStageInterface.DEFAULT_OPTIONS);
	}

	public PVector<ShepherdTour.Step> getShepherdTourSteps() {
		if (isInBrowserMode)
			return foodBrowser.getShepherdTourSteps();
		else
			return tour;
	}

	@Override
	public String toString() {
		return "Food reminder prompt";
	}
}