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

package net.scran24.user.client.survey;

import net.scran24.user.client.survey.prompts.messages.PromptMessages;
import net.scran24.user.shared.TemplateFoodData;
import net.scran24.user.shared.TemplateFoodData.ComponentDef;
import net.scran24.user.shared.TemplateFoodData.ComponentOccurence;
import net.scran24.user.shared.TemplateFoodData.ComponentType;

import org.pcollections.client.TreePVector;
import org.workcraft.gwt.shared.client.Function0;
import org.workcraft.gwt.shared.client.Option;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;

public class FoodTemplates {
	
	final private static PromptMessages messages = PromptMessages.Util.getInstance();
	
	public static final Option<Function0<Widget>> noHeader = Option.<Function0<Widget>>none();
		
	public static Option<Function0<Widget>> simpleHeader(final String styleName) {
		return Option.<Function0<Widget>>some(new Function0<Widget>() {
			@Override
			public Widget apply() {
				FlowPanel result = new FlowPanel();
				result.setStyleName(styleName);
				return result;
			}
		});
	}
	
	public static Option<Function0<Widget>> imageHeader(final String imageUrl) {
		return Option.<Function0<Widget>>some(new Function0<Widget>() {
			@Override
			public Widget apply() {
				Image img = new Image(imageUrl);
				img.addStyleName("intake24-compound-food-img");
				return img;
			}
		});
	}
	
	public static final TemplateFoodData sandwich = new TemplateFoodData("sandwich", TreePVector.<ComponentDef> empty()
			.plus(new ComponentDef(
					messages.compFood_sandwich_bread(), 
					messages.compFood_sandwich_bread_primary(), messages.compFood_sandwich_bread_secondary(), 
					messages.compFood_sandwich_bread_primary_negative(), messages.compFood_sandwich_bread_secondary_negative(), 
					messages.compFood_sandwich_bread_foods_label(), messages.compFood_sandwich_bread_categories_label(), messages.compFood_sandwich_bread_dataset_label(), 
					"SW01", imageHeader("../../images/sandwich-wizard/icons_2.png"), ComponentOccurence.Single, ComponentType.Required))
			.plus(new ComponentDef(
					messages.compFood_sandwich_spread(), 
					messages.compFood_sandwich_spread_primary(), messages.compFood_sandwich_spread_secondary(), 
					messages.compFood_sandwich_spread_primary_negative(), messages.compFood_sandwich_spread_secondary_negative(), 
					messages.compFood_sandwich_spread_foods_label(), messages.compFood_sandwich_spread_categories_label(), messages.compFood_sandwich_spread_dataset_label(), 
					"SW02", imageHeader("../../images/sandwich-wizard/icons_3.png"), ComponentOccurence.Single, ComponentType.Optional))
			.plus(new ComponentDef(
					messages.compFood_sandwich_meat_or_fish(), 
					messages.compFood_sandwich_meat_or_fish_primary(), messages.compFood_sandwich_meat_or_fish_secondary(), 
					messages.compFood_sandwich_meat_or_fish_primary_negative(), messages.compFood_sandwich_meat_or_fish_secondary_negative(), 
					messages.compFood_sandwich_meat_or_fish_foods_label(), messages.compFood_sandwich_meat_or_fish_categories_label(), messages.compFood_sandwich_meat_or_fish_dataset_label(), 
                   "SW03", imageHeader("../../images/sandwich-wizard/icons_4.png"), ComponentOccurence.Single, ComponentType.Optional))
			.plus(new ComponentDef(
					messages.compFood_sandwich_cheese_or_dairy(), 
					messages.compFood_sandwich_cheese_or_dairy_primary(), messages.compFood_sandwich_cheese_or_dairy_secondary(), 
					messages.compFood_sandwich_cheese_or_dairy_primary_negative(), messages.compFood_sandwich_cheese_or_dairy_secondary_negative(), 
					messages.compFood_sandwich_cheese_or_dairy_foods_label(), messages.compFood_sandwich_cheese_or_dairy_categories_label(), messages.compFood_sandwich_cheese_or_dairy_dataset_label(), 
					"SW04", imageHeader("../../images/sandwich-wizard/icons_5.png"), ComponentOccurence.Single, ComponentType.Optional))
             .plus(new ComponentDef(
					messages.compFood_sandwich_extra_filling(), 
					messages.compFood_sandwich_extra_filling_primary(), messages.compFood_sandwich_extra_filling_secondary(), 
					messages.compFood_sandwich_extra_filling_primary_negative(), messages.compFood_sandwich_extra_filling_secondary_negative(), 
					messages.compFood_sandwich_extra_filling_foods_label(), messages.compFood_sandwich_extra_filling_categories_label(), messages.compFood_sandwich_extra_filling_dataset_label(), 
					"SW05", imageHeader("../../images/sandwich-wizard/icons_6.png"), ComponentOccurence.Multiple, ComponentType.Optional))
			 .plus(new ComponentDef(
					messages.compFood_sandwich_sauce_or_dressing(), 
					messages.compFood_sandwich_sauce_or_dressing_primary(), messages.compFood_sandwich_sauce_or_dressing_secondary(), 
					messages.compFood_sandwich_sauce_or_dressing_primary_negative(), messages.compFood_sandwich_sauce_or_dressing_secondary_negative(), 
					messages.compFood_sandwich_sauce_or_dressing_foods_label(), messages.compFood_sandwich_sauce_or_dressing_categories_label(), messages.compFood_sandwich_sauce_or_dressing_dataset_label(),
					"SW06", imageHeader("../../images/sandwich-wizard/icons_7.png"), ComponentOccurence.Multiple, ComponentType.Optional)));

	public static final TemplateFoodData salad = new TemplateFoodData("salad", TreePVector.<ComponentDef> empty()
			.plus(new ComponentDef(
					messages.compFood_salad_ingredient(), 
					messages.compFood_salad_ingredient_primary(), messages.compFood_salad_ingredient_secondary(), 
					messages.compFood_salad_ingredient_primary_negative(), messages.compFood_salad_ingredient_secondary_negative(), 
					messages.compFood_salad_ingredient_foods_label(), messages.compFood_salad_ingredient_categories_label(), messages.compFood_salad_ingredient_dataset_label(),
					"SLW1", simpleHeader("scran24-salad-wizard-stage1"), ComponentOccurence.Multiple, ComponentType.Required))
			 .plus(new ComponentDef(
					messages.compFood_salad_sauce_or_dressing(), 
					messages.compFood_salad_sauce_or_dressing_primary(), messages.compFood_salad_sauce_or_dressing_secondary(), 
					messages.compFood_salad_sauce_or_dressing_primary_negative(), messages.compFood_salad_sauce_or_dressing_secondary_negative(), 
					messages.compFood_salad_sauce_or_dressing_foods_label(), messages.compFood_salad_sauce_or_dressing_categories_label(), messages.compFood_salad_sauce_or_dressing_dataset_label(),
					"SLW2", simpleHeader("scran24-salad-wizard-stage2"), ComponentOccurence.Single, ComponentType.Optional)));
	
}
