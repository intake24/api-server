/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey.prompts;

import static org.workcraft.gwt.shared.client.CollectionUtils.filter;
import static org.workcraft.gwt.shared.client.CollectionUtils.zipWithIndex;

import java.util.HashMap;
import java.util.Map;

import net.scran24.common.client.WidgetFactory;
import net.scran24.user.client.ShepherdTour;
import net.scran24.user.client.survey.SurveyStageInterface;
import net.scran24.user.client.survey.flat.Prompt;
import net.scran24.user.client.survey.prompts.messages.HelpMessages;
import net.scran24.user.client.survey.prompts.messages.PromptMessages;
import net.scran24.user.shared.CompoundFood;
import net.scran24.user.shared.TemplateFood;
import net.scran24.user.shared.EncodedFood;
import net.scran24.user.shared.FoodEntry;
import net.scran24.user.shared.Meal;
import net.scran24.user.shared.MissingFood;
import net.scran24.user.shared.RawFood;

import org.pcollections.client.PVector;
import org.pcollections.client.TreePVector;
import org.workcraft.gwt.shared.client.Callback1;
import org.workcraft.gwt.shared.client.CollectionUtils.WithIndex;
import org.workcraft.gwt.shared.client.Function1;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;

public class ReadyMealsPrompt implements Prompt<Meal, MealOperation> {	
	private final static PromptMessages messages = PromptMessages.Util.getInstance();
	private final static HelpMessages helpMessages = HelpMessages.Util.getInstance();
	
	private final static PVector<ShepherdTour.Step> tour = TreePVector
			.<ShepherdTour.Step> empty()
			.plus(new ShepherdTour.Step("brandNameList", "#intake24-ready-meals-list", helpMessages.readyMeals_listTitle(), helpMessages.readyMeals_listDescription()))
			.plus(new ShepherdTour.Step("cont", "#intake24-ready-meals-finished-button", helpMessages.readyMeals_finishedButtonTitle(), helpMessages.readyMeals_finishedButtonDescription()));
	
	
	private final Meal meal;

	public ReadyMealsPrompt (Meal meal) {
		this.meal = meal;
	}
 
	@Override
	public SurveyStageInterface getInterface(final Callback1<MealOperation> onComplete, 
			final Callback1<Function1<Meal, Meal>> onIntermediateStateChange) {
		
		FlowPanel content = new FlowPanel();
		
		FlowPanel promptPanel = WidgetFactory.createPromptPanel(SafeHtmlUtils.fromSafeConstant(messages.readyMeals_promptText(SafeHtmlUtils.htmlEscape(meal.name.toLowerCase()))), ShepherdTour.createTourButton(tour, ReadyMealsPrompt.class.getSimpleName()));
		ShepherdTour.makeShepherdTarget(promptPanel);
		
		content.add(promptPanel);
		
		PVector<WithIndex<FoodEntry>> potentialReadyMeals = filter(zipWithIndex(meal.foods), new Function1<WithIndex<FoodEntry>, Boolean>() {
			@Override
			public Boolean apply(WithIndex<FoodEntry> argument) {
				return argument.value.accept(new FoodEntry.Visitor<Boolean>() {

					@Override
					public Boolean visitRaw(RawFood food) {
						return false;						
					}

					@Override
					public Boolean visitEncoded(EncodedFood food) {
						return !food.isDrink() && !food.link.isLinked() && food.data.askIfReadyMeal;
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
		
		final Map<CheckBox, Integer> checkBoxToIndex = new HashMap<CheckBox, Integer>();
		
		FlowPanel checkboxesDiv = new FlowPanel();
		checkboxesDiv.addStyleName("scran24-ready-meals-checkboxes-block");
		checkboxesDiv.getElement().setId("intake24-ready-meals-list");
		
		for (WithIndex<FoodEntry> f : potentialReadyMeals) {
			FlowPanel rowDiv = new FlowPanel();
			
			CheckBox readyMealCheck = new CheckBox(SafeHtmlUtils.htmlEscape(f.value.description()));
			readyMealCheck.addStyleName("scran24-ready-meals-checkbox");
			
			checkBoxToIndex.put (readyMealCheck, f.index);
			
			rowDiv.add(readyMealCheck);
			
			checkboxesDiv.add(rowDiv);
		}
		
		content.add(checkboxesDiv);
		
		Button finishedButton = WidgetFactory.createButton(messages.editMeal_finishButtonLabel(), new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				onComplete.call(MealOperation.update(new Function1<Meal, Meal>(){
					@Override
					public Meal apply(Meal argument) {
						Meal result = argument;
						
						for (CheckBox check: checkBoxToIndex.keySet()) {
							int index = checkBoxToIndex.get(check);
							result = (check.getValue()) ? result.updateFood(index, result.foods.get(index).markReadyMeal()) : result;
						}
						
						return result.markReadyMealsComplete();												
					}
				}));
			}
		});
		
		finishedButton.getElement().setId("intake24-ready-meals-finished-button");
		
		ShepherdTour.makeShepherdTarget(checkboxesDiv, finishedButton);
		
		content.add(WidgetFactory.createButtonsPanel(finishedButton));
		
		return new SurveyStageInterface.Aligned(content, HasHorizontalAlignment.ALIGN_LEFT, HasVerticalAlignment.ALIGN_TOP, SurveyStageInterface.DEFAULT_OPTIONS);
	}
	
	@Override
	public String toString() {
		return "Mark ready meals";
	}
}