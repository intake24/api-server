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

package net.scran24.user.client.survey.portionsize.experimental;

import net.scran24.user.client.survey.flat.SimplePrompt;
import net.scran24.user.client.survey.prompts.messages.PromptMessages;
import net.scran24.user.shared.FoodData;

import org.pcollections.client.PMap;
import org.pcollections.client.PVector;
import org.pcollections.client.TreePVector;
import org.workcraft.gwt.shared.client.Function1;
import org.workcraft.gwt.shared.client.Option;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;

import static net.scran24.user.client.survey.flat.PromptUtil.withBackLink;
import static net.scran24.user.client.survey.portionsize.experimental.PortionSizeScriptUtil.done;
import static net.scran24.user.client.survey.portionsize.experimental.PortionSizeScriptUtil.standardUnitChoicePrompt;

public class MilkInHotDrinkPortionSizeScript implements PortionSizeScript {
	public static final String name = "milk-in-a-hot-drink";
	
	private final static PromptMessages messages = GWT.create(PromptMessages.class);
		
	public static final PVector<StandardUnitDef> amounts = 
			TreePVector.<StandardUnitDef>empty()
			.plus(new StandardUnitDef(messages.milkInHotDrink_amountLittle(), false, 0.10))
			.plus(new StandardUnitDef(messages.milkInHotDrink_amountAverage(), false, 0.16))
			.plus(new StandardUnitDef(messages.milkInHotDrink_amountLot(), false, 0.24));
	
	@Override
	public Option<SimplePrompt<UpdateFunc>> nextPrompt(PMap<String, String> data, final FoodData foodData) {
		if (!data.containsKey("milkPartIndex"))
			return Option.some(withBackLink(standardUnitChoicePrompt(SafeHtmlUtils.fromSafeConstant(messages.milkInHotDrink_promptText(foodData.description().toLowerCase(), "tea or coffee")), messages.milkInHotDrink_confirmButtonLabel(), amounts, new Function1<StandardUnitDef, String>(){
				@Override
				public String apply(StandardUnitDef argument) {
					return argument.name;
				}
			}, "milkPartIndex"))); 			
		else
		  return done();
	}
}
