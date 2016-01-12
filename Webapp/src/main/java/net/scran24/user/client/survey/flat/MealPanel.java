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

package net.scran24.user.client.survey.flat;

import static org.workcraft.gwt.shared.client.CollectionUtils.filter;
import static org.workcraft.gwt.shared.client.CollectionUtils.zipWithIndex;
import net.scran24.common.client.UnorderedList;
import net.scran24.datastore.shared.Time;
import net.scran24.user.client.survey.SurveyMessages;
import net.scran24.user.client.survey.flat.Selection.EmptySelection;
import net.scran24.user.client.survey.flat.Selection.SelectedFood;
import net.scran24.user.client.survey.flat.Selection.SelectedMeal;
import net.scran24.user.client.survey.flat.Selection.Visitor;
import net.scran24.user.shared.FoodEntry;
import net.scran24.user.shared.Meal;
import net.scran24.user.shared.UUID;

import org.pcollections.client.PVector;
import org.workcraft.gwt.shared.client.Callback1;
import org.workcraft.gwt.shared.client.CollectionUtils.WithIndex;
import org.workcraft.gwt.shared.client.Function1;
import org.workcraft.gwt.shared.client.Option;
import org.workcraft.gwt.shared.client.Option.SideEffectVisitor;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;

public class MealPanel extends Composite {
	
	private final static SurveyMessages messages = SurveyMessages.Util.getInstance();
	
	private final int mealIndex;
	private final Selection selection;
	private final Callback1<Selection> requestSelectionChange;

	private void addFoodRow(UnorderedList<FlowPanel> list, final WithIndex<FoodEntry> f) {
		String desc = f.value.description();

		FlowPanel foodRow = new FlowPanel();
		foodRow.addStyleName("intake24-food");
		
		Label foodLabel = new Label(desc);
		foodLabel.addStyleName("intake24-food-name");
		if (!f.value.link.linkedTo.isEmpty())
			foodLabel.addStyleName("intake24-linked-food-name");

		foodLabel.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				requestSelectionChange.call(new Selection.SelectedFood(mealIndex, f.index, SelectionType.MANUAL_SELECTION));
			}
		});

		FlowPanel encodingComplete = new FlowPanel();
		FlowPanel portionSizeComplete = new FlowPanel();

		if (!f.value.isTemplate()) {
			foodRow.add(portionSizeComplete);			
			foodRow.add(encodingComplete);
		}
		
		foodRow.add(foodLabel);

		if (!f.value.isRaw()) {
			encodingComplete.addStyleName("intake24-checkbox-checked-icon");
			encodingComplete.setTitle(messages.navPanelMatchedTooltip());
		} else {
			encodingComplete.addStyleName("intake24-checkbox-unchecked-icon");
			encodingComplete.setTitle(messages.navPanelNotMatchedTooltip());
		}

		if (f.value.isPortionSizeComplete()) {
			portionSizeComplete.addStyleName("intake24-checkbox-checked-icon");
			portionSizeComplete.setTitle(messages.navPanelPortionSizeComplete());
		} else {
			portionSizeComplete.addStyleName("intake24-checkbox-unchecked-icon");
			portionSizeComplete.setTitle(messages.navPanelPortionSizeIncomplete());
		}

		boolean thisFoodSelected = selection.accept(new Visitor<Boolean>() {
			@Override
			public Boolean visitMeal(SelectedMeal meal) {
				return false;
			}

			@Override
			public Boolean visitFood(SelectedFood food) {
				return (food.mealIndex == mealIndex) && (food.foodIndex == f.index);
			}

			@Override
			public Boolean visitNothing(EmptySelection selection) {
				return false;
			}
		});

		if (thisFoodSelected)
			foodRow.addStyleName("intake24-selected-food");
		
		list.addItem(foodRow);
	}

	public MealPanel(final Meal meal, final int mealIndex, final Selection selection, final Callback1<Selection> requestSelectionChange) {
		this.mealIndex = mealIndex;
		this.selection = selection;
		this.requestSelectionChange = requestSelectionChange;
		
		FlowPanel contents = new FlowPanel();
		initWidget(contents);

		boolean thisMealSelected = selection.accept(new Visitor<Boolean>() {
			@Override
			public Boolean visitMeal(SelectedMeal meal) {
				return meal.mealIndex == mealIndex;
			}

			@Override
			public Boolean visitFood(SelectedFood food) {
				return false;
			}

			@Override
			public Boolean visitNothing(EmptySelection selection) {
				return false;
			}
		});

		final FlowPanel mealRow = new FlowPanel();
			
		mealRow.addStyleName("intake24-meal");
		
		if (thisMealSelected)
			mealRow.addStyleName("intake24-selected-meal");
			
		final Label label = new Label(meal.name);
		label.addStyleName("intake24-meal-name");
		
		label.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				requestSelectionChange.call(new Selection.SelectedMeal(mealIndex, SelectionType.MANUAL_SELECTION));
			}
		});
		
		meal.time.accept(new SideEffectVisitor<Time>() {
			@Override
			public void visitSome(Time item) {
				Label timeLabel = new Label(meal.time.getOrDie().toString());
				timeLabel.addStyleName("intake24-meal-time");
				mealRow.add(timeLabel);
			}

			@Override
			public void visitNone() {
				final FlowPanel questionIcon = new FlowPanel();
				questionIcon.addStyleName("intake24-meal-question-icon");
				questionIcon.setTitle(messages.navPanelSuggestedTooltip());
				mealRow.add(questionIcon);
			}
		});
		
		mealRow.add(label);
		
		contents.add(mealRow);

		if (!meal.foods.isEmpty()) {
			UnorderedList<FlowPanel> foodList = new UnorderedList<FlowPanel>();
			foodList.addStyleName("intake24-food-list");
			
			PVector<WithIndex<FoodEntry>> withIndex = zipWithIndex(meal.foods);

			PVector<WithIndex<FoodEntry>> topLevel = filter(withIndex, new Function1<WithIndex<FoodEntry>, Boolean>() {
				@Override
				public Boolean apply(WithIndex<FoodEntry> argument) {
					return argument.value.link.linkedTo.isEmpty();
				}
			});

			for (final WithIndex<FoodEntry> f : topLevel) {
				addFoodRow(foodList, f);

				PVector<WithIndex<FoodEntry>> linkedToThis = filter(withIndex, new Function1<WithIndex<FoodEntry>, Boolean>() {
					@Override
					public Boolean apply(WithIndex<FoodEntry> argument) {
						return argument.value.link.linkedTo.accept(new Option.Visitor<UUID, Boolean>() {
							@Override
							public Boolean visitSome(UUID item) {
								return item.equals(f.value.link.id);
							}

							@Override
							public Boolean visitNone() {
								return false;
							}
						});
					}
				});

				for (final WithIndex<FoodEntry> linked : linkedToThis) {
					addFoodRow(foodList, linked);
				}
			}
			contents.add(foodList);
		}
	}
}
