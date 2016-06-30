/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey.prompts;

import static org.workcraft.gwt.shared.client.CollectionUtils.map;
import net.scran24.common.client.WidgetFactory;
import net.scran24.user.client.ShepherdTour;
import net.scran24.user.client.survey.SurveyStageInterface;
import net.scran24.user.client.survey.flat.Prompt;
import net.scran24.user.client.survey.flat.PromptUtil;
import net.scran24.user.client.survey.flat.SameAsBefore;
import net.scran24.user.client.survey.portionsize.experimental.MilkInHotDrinkPortionSizeScript;
import net.scran24.user.client.survey.portionsize.experimental.PortionSize;
import net.scran24.user.client.survey.prompts.messages.HelpMessages;
import net.scran24.user.client.survey.prompts.messages.PromptMessages;
import net.scran24.user.shared.EncodedFood;
import net.scran24.user.shared.FoodEntry;
import net.scran24.user.shared.FoodLink;
import net.scran24.user.shared.Meal;
import net.scran24.user.shared.SpecialData;

import org.pcollections.client.PVector;
import org.pcollections.client.TreePVector;
import org.workcraft.gwt.shared.client.Callback1;
import org.workcraft.gwt.shared.client.Function1;
import org.workcraft.gwt.shared.client.Pair;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Panel;

public class SameAsBeforePrompt implements Prompt<Pair<FoodEntry, Meal>, MealOperation> {
	private static final PromptMessages messages = PromptMessages.Util.getInstance();
	private static final HelpMessages helpMessages = HelpMessages.Util.getInstance();

	private final static PVector<ShepherdTour.Step> tour = TreePVector
			.<ShepherdTour.Step> empty()
			.plus(new ShepherdTour.Step("protionSize", "#intake24-sab-portion-size", helpMessages.sameAsBefore_portionSizeTitle(), helpMessages.sameAsBefore_portionSizeDescription()))
			.plus(new ShepherdTour.Step("leftovers", "#intake24-sab-leftovers", helpMessages.sameAsBefore_leftoversTitle(), helpMessages.sameAsBefore_leftoversDescription()))
			.plus(new ShepherdTour.Step("assocFoods", "#intake24-sab-assoc-foods", helpMessages.sameAsBefore_assocFoodsTitle(), helpMessages.sameAsBefore_assocFoodsDescription()))
			.plus(new ShepherdTour.Step("yesButton", "#intake24-sab-yes-button", helpMessages.sameAsBefore_yesButtonTitle(), helpMessages.sameAsBefore_yesButtonDescription()))
			.plus(new ShepherdTour.Step("noButton", "#intake24-sab-no-button", helpMessages.sameAsBefore_noButtonTitle(), helpMessages.sameAsBefore_noButtonDescription(), "top right", "bottom right"));
	
	private final EncodedFood food;
	private final SameAsBefore asBefore;
	private final int foodIndex;
	
	public SameAsBeforePrompt(final Pair<FoodEntry, Meal> pair, final int foodIndex, SameAsBefore asBefore) {
		this.foodIndex = foodIndex;
		this.food = pair.left.asEncoded();
		this.asBefore = asBefore;
	}

	@Override
	public String toString() {
		return "Same as before prompt";
	}

	@Override
	public SurveyStageInterface getInterface(final Callback1<MealOperation> onComplete,
			Callback1<Function1<Pair<FoodEntry, Meal>, Pair<FoodEntry, Meal>>> updateIntermediateState) {
		
		final FlowPanel content = new FlowPanel();
		PromptUtil.addBackLink(content);
		final Panel promptPanel = WidgetFactory.createPromptPanel(SafeHtmlUtils.fromSafeConstant(messages.sameAsBefore_promptText(SafeHtmlUtils.htmlEscape(food.description().toLowerCase()))), ShepherdTour.createTourButton(tour, SameAsBeforePrompt.class.getSimpleName()));
		content.add(promptPanel);
		
		final EncodedFood mainFoodAsBefore = asBefore.mainFood; 
		final PVector<FoodEntry> assocFoodsAsBefore = asBefore.linkedFoods;
		
		final double leftoversWeight = mainFoodAsBefore.completedPortionSize().leftoversWeight();
		final double servingWeight = mainFoodAsBefore.completedPortionSize().servingWeight();
		
		final int leftoversPercent = (int) (leftoversWeight * 100.0 / servingWeight);
		final int leftoversPercentRounded = (leftoversPercent + 4) / 5 * 5; 
		
		final String portionSize = messages.sameAsBefore_servingSize(Integer.toString((int)servingWeight) + (mainFoodAsBefore.isDrink() ? " ml" : " g"));
		final String leftovers = (leftoversWeight < 0.01) ? (mainFoodAsBefore.isDrink() ? messages.sameAsBefore_noLeftoversDrink() : messages.sameAsBefore_noLeftoversFood()) : messages.sameAsBefore_leftovers(leftoversPercentRounded + "%");
		
		HTMLPanel portionSizePanel = new HTMLPanel (portionSize);
		portionSizePanel.getElement().setId("intake24-sab-portion-size");
		content.add(portionSizePanel);
		HTMLPanel leftoversPanel = new HTMLPanel (leftovers);
		leftoversPanel.getElement().setId("intake24-sab-leftovers");
		content.add(leftoversPanel);
		
		String assocFoodsHTML = messages.sameAsBefore_hadItWith();
	
		if (!assocFoodsAsBefore.isEmpty())
			assocFoodsHTML += "<ul>";
		
		for (FoodEntry f: assocFoodsAsBefore) {
			EncodedFood assocFood = f.asEncoded();
			
			String assocFoodDescription;
			
			if (assocFood.isInCategory(SpecialData.FOOD_CODE_MILK_IN_HOT_DRINK))
				assocFoodDescription = SafeHtmlUtils.htmlEscape(assocFood.description()) + " (" + SafeHtmlUtils.htmlEscape(MilkInHotDrinkPortionSizeScript.amounts.get(Integer.parseInt(assocFood.completedPortionSize().data.get("milkPartIndex"))).name) +")";
			else
				assocFoodDescription = SafeHtmlUtils.htmlEscape(assocFood.description()) + " (" + Integer.toString((int)assocFood.completedPortionSize().servingWeight()) + (assocFood.isDrink() ? " ml" : " g") + ")";
			
			assocFoodsHTML += "<li>" + assocFoodDescription + "</li>";
		}
		
		HTMLPanel assocFoodsPanel;

		if (!assocFoodsAsBefore.isEmpty()) {
			assocFoodsHTML += "</ul>";
			assocFoodsPanel = new HTMLPanel(assocFoodsHTML);
			
		} else {
			assocFoodsPanel = new HTMLPanel(messages.sameAsBefore_noAddedFoods());
		}
		
		assocFoodsPanel.getElement().setId("intake24-sab-assoc-foods");
		content.add(assocFoodsPanel);
		
		Button yes = WidgetFactory.createButton(messages.sameAsBefore_confirmButtonLabel(), new ClickHandler() {
			
			@Override
			public void onClick(ClickEvent event) {
				onComplete.call(MealOperation.update(new Function1<Meal, Meal>() {
					@Override
					public Meal apply(final Meal meal) {
						
						PVector<FoodEntry> updatedFoods = 
								meal.foods.with(foodIndex, food.withPortionSize(PortionSize.complete(mainFoodAsBefore.completedPortionSize())).disableAllPrompts())
								.plusAll(map(assocFoodsAsBefore, new Function1<FoodEntry, FoodEntry>() {
									@Override
									public FoodEntry apply(FoodEntry assocFood) {
										return assocFood.relink(FoodLink.newLinked(food.link.id));
									}
								}));
						
						return meal.withFoods(updatedFoods);
					}
				}));				
			}
		});
		
		yes.getElement().setId("intake24-sab-yes-button");
		
		Button no = WidgetFactory.createButton(messages.sameAsBefore_rejectButtonLabel(), new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				onComplete.call(MealOperation.updateEncodedFood(foodIndex, new Function1<EncodedFood, EncodedFood>() {
					
					@Override
					public EncodedFood apply(EncodedFood argument) {
						return argument.markNotSameAsBefore();
					}
				}));
				
			}
		});
		
		no.getElement().setId("intake24-sab-no-button");
			
		content.add(WidgetFactory.createButtonsPanel(yes, no));
		
		ShepherdTour.makeShepherdTarget(promptPanel, portionSizePanel, leftoversPanel, assocFoodsPanel, yes, no);
		
		return new SurveyStageInterface.Aligned(content, HasHorizontalAlignment.ALIGN_LEFT, HasVerticalAlignment.ALIGN_TOP,
				SurveyStageInterface.DEFAULT_OPTIONS);
	}
}