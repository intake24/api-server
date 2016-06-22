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

package net.scran24.user.client.survey.flat.rules;

import net.scran24.common.client.CurrentUser;
import net.scran24.user.client.survey.CompoundFoodTemplateManager;
import net.scran24.user.client.survey.flat.Prompt;
import net.scran24.user.client.survey.flat.PromptRule;
import net.scran24.user.client.survey.flat.SameAsBefore;
import net.scran24.user.client.survey.flat.SelectionMode;
import net.scran24.user.client.survey.flat.StateManagerUtil;
import net.scran24.user.client.survey.portionsize.experimental.PortionSizeScriptManager;
import net.scran24.user.client.survey.prompts.MealOperation;
import net.scran24.user.client.survey.prompts.SameAsBeforePrompt;
import net.scran24.user.shared.EncodedFood;
import net.scran24.user.shared.FoodEntry;
import net.scran24.user.shared.Meal;
import net.scran24.user.shared.SpecialData;
import net.scran24.user.shared.WithPriority;

import org.pcollections.client.PSet;
import org.workcraft.gwt.shared.client.Option;
import org.workcraft.gwt.shared.client.Pair;

public class ShowSameAsBeforePrompt implements PromptRule<Pair<FoodEntry, Meal>, MealOperation> {
	private final PortionSizeScriptManager scriptManager;
	private final CompoundFoodTemplateManager templateManager;
	private final String scheme_id;
	private final String version_id;
		
	public ShowSameAsBeforePrompt(String scheme_id, String version_id, PortionSizeScriptManager scriptManager, CompoundFoodTemplateManager templateManager) {
		this.scheme_id = scheme_id;
		this.version_id = version_id;
		this.scriptManager = scriptManager;
		this.templateManager = templateManager;
	}

	@Override
	public Option<Prompt<Pair<FoodEntry, Meal>, MealOperation>> apply(final Pair<FoodEntry, Meal> pair, SelectionMode selectionType, PSet<String> surveyFlags) {
		if (pair.left.isEncoded()) {
			EncodedFood f = pair.left.asEncoded();
			if (!f.data.sameAsBeforeOption || f.notSameAsBefore() || f.isInCategory(SpecialData.FOOD_CODE_MILK_IN_HOT_DRINK) || f.isPortionSizeComplete() || f.link.isLinked())
				return Option.none();
			else {
				Option<SameAsBefore> sameAsBefore = StateManagerUtil.getSameAsBefore(CurrentUser.getUserInfo().userName, f.data.code, scheme_id, version_id, scriptManager, templateManager);
				return sameAsBefore.accept(new Option.Visitor<SameAsBefore, Option<Prompt<Pair<FoodEntry, Meal>, MealOperation>>>() {
					@Override
					public Option<Prompt<Pair<FoodEntry, Meal>, MealOperation>> visitSome(SameAsBefore item) {
						return Option.<Prompt<Pair<FoodEntry, Meal>, MealOperation>>some(new SameAsBeforePrompt(pair, pair.right.foodIndex(pair.left), item));
					}

					@Override
					public Option<Prompt<Pair<FoodEntry, Meal>, MealOperation>> visitNone() {
						return Option.none();
					}
				});
			}
		} else
			return Option.none();
	}

	public static WithPriority<PromptRule<Pair<FoodEntry, Meal>, MealOperation>> withPriority(int priority, String scheme_id, String version_id, PortionSizeScriptManager scriptManager, CompoundFoodTemplateManager templateManager) {
		return new WithPriority<PromptRule<Pair<FoodEntry, Meal>, MealOperation>>(new ShowSameAsBeforePrompt(scheme_id, version_id, scriptManager, templateManager), priority);
	}
}