/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.surveyscheme;

import static org.workcraft.gwt.shared.client.CollectionUtils.map;
import static org.workcraft.gwt.shared.client.CollectionUtils.zipWithIndex;
import net.scran24.common.client.WidgetFactory;
import net.scran24.user.client.survey.SurveyStageInterface;
import net.scran24.user.client.survey.flat.Prompt;
import net.scran24.user.client.survey.prompts.MealOperation;
import net.scran24.user.shared.FoodEntry;
import net.scran24.user.shared.Meal;

import org.pcollections.client.PVector;
import org.workcraft.gwt.shared.client.Callback1;
import org.workcraft.gwt.shared.client.CollectionUtils.WithIndex;
import org.workcraft.gwt.shared.client.Function1;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;

public class FoodSourcesPrompt implements Prompt<Meal, MealOperation> {
	private final Meal meal;
	private final PVector<String> sourceOptions;
	private final String mainSourceOption;

	public FoodSourcesPrompt(Meal meal, PVector<String> sourceOptions, String mainSourceOption) {
		this.meal = meal;
		this.sourceOptions = sourceOptions;
		this.mainSourceOption = mainSourceOption;
	}
	
	private ListBox createSourceChoice(String existingChoice) {
		ListBox result = new ListBox(false);
		
		if (!sourceOptions.contains(mainSourceOption))
			result.addItem(mainSourceOption);
			
		for (String s : sourceOptions)
			result.addItem(s);
		
		if (existingChoice != null)
			result.setSelectedIndex(sourceOptions.indexOf(existingChoice));
		else
			result.setSelectedIndex(sourceOptions.indexOf(mainSourceOption));
		
		return result;
	}

	@Override
	public SurveyStageInterface getInterface(final Callback1<MealOperation> onComplete, final Callback1<Function1<Meal, Meal>> onIntermediateStateChange) {

		FlowPanel content = new FlowPanel();
		
		/* PVector<WithIndex<FoodEntry>> foodsToShow = filter(zipWithIndex(meal.foods), new Function1<WithIndex<FoodEntry>, Boolean>() {
			@Override
			public Boolean apply(WithIndex<FoodEntry> argument) {
				return !argument.value.customData.containsKey("foodSource");
			}
		}); */

		content.add(WidgetFactory.createPromptPanel(SafeHtmlUtils.fromSafeConstant("<p>If some of the food items that you had for your <strong>"
				+ meal.safeName() + "</strong> came from a place other than " + SafeHtmlUtils.htmlEscape(mainSourceOption.toLowerCase())
				+ ", please indicate where did you get those.</p>")));

		Grid foodSourceChoice = new Grid(meal.foods.size() + 1, 2);
		foodSourceChoice.setCellPadding(5);
		foodSourceChoice.setWidget(0, 0, new Label("Food"));
		foodSourceChoice.setWidget(0, 1, new Label("Source"));
		
		final ListBox[] sourceChoices = new ListBox[meal.foods.size()];
		
		for (int i = 0; i < meal.foods.size(); i++) {
			sourceChoices[i] = createSourceChoice(meal.foods.get(i).customData.get("foodSource"));
			
			foodSourceChoice.setWidget(i+1, 0, new Label(SafeHtmlUtils.htmlEscape(meal.foods.get(i).description())));
			foodSourceChoice.setWidget(i+1, 1, sourceChoices[i]);
		}
		
		content.add(foodSourceChoice);
		
		Button finished = WidgetFactory.createButton("Continue", new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				onComplete.call(MealOperation.update(new Function1<Meal, Meal>() {
					@Override
					public Meal apply(Meal meal) {
						return meal.withFoods(map(zipWithIndex(meal.foods), new Function1<WithIndex<FoodEntry>, FoodEntry>() {
							@Override
							public FoodEntry apply(WithIndex<FoodEntry> argument) {
								return argument.value.withCustomDataField("foodSource", sourceChoices[argument.index].getValue(sourceChoices[argument.index].getSelectedIndex()));
							}
						}));																		
					}
				}));
			}
		});

		content.add(WidgetFactory.createButtonsPanel(finished));

		return new SurveyStageInterface.Aligned(content, HasHorizontalAlignment.ALIGN_LEFT, HasVerticalAlignment.ALIGN_TOP,
				SurveyStageInterface.DEFAULT_OPTIONS);
	}

	@Override
	public String toString() {
		return "Mark ready meals";
	}
}