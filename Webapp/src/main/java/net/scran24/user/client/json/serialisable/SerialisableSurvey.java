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

package net.scran24.user.client.json.serialisable;

import static org.workcraft.gwt.shared.client.CollectionUtils.map;

import java.util.List;
import java.util.Map;
import java.util.Set;

import net.scran24.user.client.json.serialisable.SerialisableSelection.SerialisableEmptySelection;
import net.scran24.user.client.json.serialisable.SerialisableSelection.SerialisableSelectedFood;
import net.scran24.user.client.json.serialisable.SerialisableSelection.SerialisableSelectedMeal;
import net.scran24.user.client.survey.CompoundFoodTemplateManager;
import net.scran24.user.client.survey.flat.Selection;
import net.scran24.user.client.survey.flat.Selection.EmptySelection;
import net.scran24.user.client.survey.flat.Selection.SelectedFood;
import net.scran24.user.client.survey.flat.Selection.SelectedMeal;
import net.scran24.user.client.survey.flat.Survey;
import net.scran24.user.client.survey.portionsize.experimental.PortionSizeScriptManager;
import net.scran24.user.shared.Meal;

import org.pcollections.client.HashTreePMap;
import org.pcollections.client.HashTreePSet;
import org.pcollections.client.PMap;
import org.pcollections.client.PSet;
import org.pcollections.client.PVector;
import org.pcollections.client.TreePVector;
import org.workcraft.gwt.shared.client.Function1;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SerialisableSurvey {
	
	@JsonProperty
	public long startTime;
	@JsonProperty
	public final PVector<SerialisableMeal> meals;
	@JsonProperty
	public final SerialisableSelection selectedElement;
	@JsonProperty
	public final PSet<String> flags;
	@JsonProperty
	public final PMap<String, String> customData;
	@JsonProperty
	public final String scheme_id;
	@JsonProperty
	public final String version_id;
	

	@JsonCreator
	public SerialisableSurvey(@JsonProperty("meals") List<SerialisableMeal> meals, @JsonProperty("selectedElement") SerialisableSelection selectedElement,
			@JsonProperty("startTime") long startTime, @JsonProperty("flags") Set<String> flags,
			@JsonProperty("customData") Map<String, String> customData, @JsonProperty("scheme_id") String scheme_id, 
			@JsonProperty("version_id") String version_id) {
		this.startTime = startTime;
		this.meals = TreePVector.from(meals);
		this.selectedElement = selectedElement;
		this.flags = HashTreePSet.from(flags);
		this.customData = HashTreePMap.from(customData);
		this.scheme_id = scheme_id;
		this.version_id = version_id;
	}

	public SerialisableSurvey(Survey survey, String scheme_id, String version_id) {
		this.startTime = survey.startTime;
		this.scheme_id = scheme_id;
		this.version_id = version_id;
		this.meals = map(survey.meals, new Function1<Meal, SerialisableMeal>() {
			@Override
			public SerialisableMeal apply(Meal argument) {
				return new SerialisableMeal(argument);
			}			
		});
		
		this.selectedElement = survey.selectedElement.accept(new Selection.Visitor<SerialisableSelection>() {
			@Override
			public SerialisableSelection visitMeal(SelectedMeal meal) {
				return new SerialisableSelection.SerialisableSelectedMeal(meal);
			}

			@Override
			public SerialisableSelection visitFood(SelectedFood food) {
				return new SerialisableSelection.SerialisableSelectedFood(food);
			}

			@Override
			public SerialisableSelection visitNothing(EmptySelection selection) {
				return new SerialisableSelection.SerialisableEmptySelection(selection);
			}
		});
		
		this.flags = survey.flags;
		this.customData = survey.customData;		
	}
	
	public Survey toSurvey(final PortionSizeScriptManager scriptManager, final CompoundFoodTemplateManager templateManager) {
		
		PVector<Meal> surveyMeals = map(meals, new Function1<SerialisableMeal, Meal>() {
			@Override
			public Meal apply(SerialisableMeal argument) {
				return argument.toMeal(scriptManager, templateManager);
			}			
		});
		
		Selection surveySelectedElement = selectedElement.accept(new SerialisableSelection.Visitor<Selection>() {
			@Override
			public Selection visitMeal(SerialisableSelectedMeal meal) {
				return meal.toSelectedMeal();
			}

			@Override
			public Selection visitFood(SerialisableSelectedFood food) {
				return food.toSelectedFood();
			}

			@Override
			public Selection visitNothing(SerialisableEmptySelection selection) {
				return selection.toEmptySelection();
			}
		});
		
		return new Survey(surveyMeals, surveySelectedElement, startTime, flags, customData); 
	}
}
