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

import java.util.logging.Logger;

import net.scran24.common.client.GoogleAnalytics;
import net.scran24.common.client.WidgetFactory;
import net.scran24.user.client.ShepherdTour;
import net.scran24.user.client.survey.FoodTemplates;
import net.scran24.user.client.survey.SurveyStageInterface;
import net.scran24.user.client.survey.flat.Prompt;
import net.scran24.user.client.survey.flat.PromptUtil;
import net.scran24.user.client.survey.prompts.messages.HelpMessages;
import net.scran24.user.client.survey.prompts.messages.PromptMessages;
import net.scran24.user.shared.TemplateFood;
import net.scran24.user.shared.TemplateFoodData.ComponentDef;
import net.scran24.user.shared.EncodedFood;
import net.scran24.user.shared.FoodData;
import net.scran24.user.shared.FoodEntry;
import net.scran24.user.shared.FoodLink;
import net.scran24.user.shared.Meal;
import net.scran24.user.shared.MissingFood;

import org.pcollections.client.PVector;
import org.pcollections.client.TreePVector;
import org.workcraft.gwt.shared.client.Callback;
import org.workcraft.gwt.shared.client.Callback1;
import org.workcraft.gwt.shared.client.Function0;
import org.workcraft.gwt.shared.client.Function1;
import org.workcraft.gwt.shared.client.Option;
import org.workcraft.gwt.shared.client.Pair;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;

public class CompoundFoodPrompt implements Prompt<Pair<FoodEntry, Meal>, MealOperation> {
	private final static PromptMessages messages = PromptMessages.Util.getInstance();
	private final static HelpMessages helpMessages = HelpMessages.Util.getInstance();

	private final static PVector<ShepherdTour.Step> tour = TreePVector.<ShepherdTour.Step> empty().plus(
			new ShepherdTour.Step("prompt", "#intake24-compound-food-prompt", helpMessages.compoundFood_promptTitle(), helpMessages
					.compoundFood_promptDescription()));;

	private final Meal meal;
	private final int foodIndex;
	private final int componentIndex;
	private final boolean allowSkip;

	private Panel buttonsPanel;
	private boolean isFirst;
	private static Logger log = Logger.getLogger(CompoundFoodPrompt.class.getSimpleName());

	private FoodBrowser foodBrowser;

	public CompoundFoodPrompt(final Meal meal, final int foodIndex, final int componentIndex, boolean isFirst, boolean allowSkip) {
		this.meal = meal;
		this.foodIndex = foodIndex;
		this.componentIndex = componentIndex;
		this.isFirst = isFirst;
		this.allowSkip = allowSkip;
	}

	@Override
	public SurveyStageInterface getInterface(final Callback1<MealOperation> onComplete,
			Callback1<Function1<Pair<FoodEntry, Meal>, Pair<FoodEntry, Meal>>> updateIntermediateState) {
		final TemplateFood food = (TemplateFood) meal.foods.get(foodIndex);
		final ComponentDef component = food.data.template.get(componentIndex);

		final FlowPanel content = new FlowPanel();

		PromptUtil.addBackLink(content);

		component.headerConstructor.accept(new Option.SideEffectVisitor<Function0<Widget>>() {
			@Override
			public void visitSome(Function0<Widget> item) {
				Widget header = item.apply();
				ShepherdTour.makeShepherdTarget(header);
				content.add(header);
			}

			@Override
			public void visitNone() {
			}
		});

		SafeHtml promptText;

		if (isFirst)
			promptText = SafeHtmlUtils.fromSafeConstant(component.primaryInstancePrompt);
		else
			promptText = SafeHtmlUtils.fromSafeConstant(component.secondaryInstancePrompt);

		final Panel promptPanel = WidgetFactory.createPromptPanel(promptText, WidgetFactory.createHelpButton(new ClickHandler() {
			@Override
			public void onClick(ClickEvent arg0) {
				String promptType = CompoundFoodPrompt.class.getSimpleName() + "." + food.data.template_id;
				GoogleAnalytics.trackHelpButtonClicked(promptType);
				ShepherdTour.startTour(tour.plusAll(foodBrowser.getShepherdTourSteps()), promptType);
			}
		}));

		content.add(promptPanel);
		ShepherdTour.makeShepherdTarget(promptPanel);
		promptPanel.getElement().setId("intake24-compound-food-prompt");

		String skipLabel;

		if (isFirst)
			skipLabel = component.primarySkipButtonLabel;
		else
			skipLabel = component.secondarySkipButtonLabel;

		Option<SkipFoodHandler> skipHandler = allowSkip ? Option.some(new SkipFoodHandler(skipLabel, new Callback() {
			@Override
			public void call() {
				onComplete.call(MealOperation.updateTemplateFood(foodIndex, new Function1<TemplateFood, TemplateFood>() {
					@Override
					public TemplateFood apply(TemplateFood argument) {
						return argument.markComponentComplete(componentIndex);
					}
				}));
			}
		})) : Option.<SkipFoodHandler> none();

		foodBrowser = new FoodBrowser(new Callback1<FoodData>() {
			@Override
			public void call(final FoodData result) {
				onComplete.call(MealOperation.update(new Function1<Meal, Meal>() {
					@Override
					public Meal apply(Meal argument) {
						EncodedFood componentFood = new EncodedFood(result, FoodLink.newLinked(food.link.id), "compound food template");
						return argument.plusFood(componentFood).updateFood(foodIndex, food.addComponent(componentIndex, componentFood.link.id));
					}
				}));
			}
		}, new Callback1<String>() {
			@Override
			public void call(String code) {
				throw new RuntimeException("Special foods are not allowed as compound food ingredients");
			}
		}, new Callback() {
			@Override
			public void call() {
				onComplete.call(MealOperation.update(new Function1<Meal, Meal>() {
					@Override
					public Meal apply(Meal argument) {
						MissingFood missingFood = new MissingFood(FoodLink.newLinked(food.link.id), component.name, false).withFlag(MissingFood.NOT_HOME_RECIPE_FLAG);

						return argument.plusFood(missingFood).updateFood(foodIndex, food.addComponent(componentIndex, missingFood.link.id));
					}
				}));
			}
		}, skipHandler, false, Option.<Pair<String, String>> none());

		foodBrowser.browse(component.categoryCode, component.dataSetLabel, component.foodsLabel, component.categoriesLabel);

		content.add(foodBrowser);

		return new SurveyStageInterface.Aligned(content, HasHorizontalAlignment.ALIGN_LEFT, HasVerticalAlignment.ALIGN_TOP,
				SurveyStageInterface.DEFAULT_OPTIONS);
	}

	@Override
	public String toString() {
		return "Compound food prompt";
	}
}