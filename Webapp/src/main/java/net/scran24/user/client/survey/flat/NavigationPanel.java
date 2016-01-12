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

import net.scran24.common.client.UnorderedList;
import net.scran24.common.client.WidgetFactory;
import net.scran24.user.client.survey.SurveyMessages;
import net.scran24.user.shared.Meal;

import org.workcraft.gwt.shared.client.Callback;
import org.workcraft.gwt.shared.client.Callback1;
import org.workcraft.gwt.shared.client.CollectionUtils.WithIndex;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.logical.shared.AttachEvent.Handler;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;

public class NavigationPanel extends Composite {
	private static final SurveyMessages messages = SurveyMessages.Util.getInstance(); 
	
	private final FlowPanel mealsPanel;
	private Callback requestAddMeal;
	private Callback1<Selection> requestSelection;

	private native void stateChangedJS() /*-{
		if (typeof $wnd.intake24_mealsPanelChanged == 'function')
			$wnd.intake24_mealsPanelChanged();
	}-*/;

	private native void addMealClickedJS() /*-{
		if (typeof $wnd.intake24_addMealClicked == 'function')
			$wnd.intake24_addMealClicked();
	}-*/;

	public void stateChanged(final Survey state) {
		mealsPanel.clear();

		Button addMealButton = WidgetFactory.createButton(messages.addMealLabel(), new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				addMealClickedJS();
				requestAddMeal.call();
			}
		});

		addMealButton.getElement().setId("intake24-add-meal-button");

		UnorderedList<MealPanel> mealList = new UnorderedList<MealPanel>();
		mealList.addStyleName("intake24-meal-list");

		for (WithIndex<Meal> m : state.mealsSortedByTime) {
			MealPanel p = new MealPanel(m.value, m.index, state.selectedElement, new Callback1<Selection>() {
				@Override
				public void call(Selection arg1) {
					requestSelection.call(arg1);
				}
			});
			mealList.addItem(p);
		}

		FlowPanel headerContainer = new FlowPanel();
		headerContainer.addStyleName("intake24-meals-panel-header-container");

		FlowPanel headerButton = new FlowPanel();
		headerButton.addStyleName("intake24-meals-panel-header-button");

		HTMLPanel header = new HTMLPanel(SafeHtmlUtils.fromSafeConstant(messages.navPanelHeader()));
		header.addStyleName("intake24-meals-panel-header");

		headerContainer.add(headerButton);
		headerContainer.add(header);

		mealsPanel.add(headerContainer);
		mealsPanel.add(mealList);
		mealsPanel.add(addMealButton);

		stateChangedJS();
	}

	public void setCallbacks(Callback1<Selection> requestSelection, Callback requestAddMeal) {
		this.requestSelection = requestSelection;
		this.requestAddMeal = requestAddMeal;
	}

	public NavigationPanel(Survey initialState) {
		mealsPanel = new FlowPanel();
		mealsPanel.getElement().setId("intake24-meals-panel");
		initWidget(mealsPanel);

		addAttachHandler(new Handler() {
			@Override
			public void onAttachOrDetach(AttachEvent event) {
				if (event.isAttached())
					stateChangedJS();
			}
		});

		stateChanged(initialState);
	}
}