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
*/

package net.scran24.user.server.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import uk.ac.ncl.openlab.intake24.PortionSizeMethodParameter;
import net.scran24.user.shared.CategoryHeader;
import net.scran24.user.shared.FoodData;
import net.scran24.user.shared.FoodHeader;
import net.scran24.user.shared.FoodPrompt;
import net.scran24.user.shared.lookup.PortionSizeMethod;

import org.workcraft.gwt.shared.client.Option;

import scala.collection.Iterator;
import scala.collection.Seq;

public class ScalaConversions {
	
	public static <T> Option<T> toJavaOption(scala.Option<T> scalaOption) {
		if (scalaOption.isDefined())
			return Option.some(scalaOption.get());
		else
			return Option.none();		
	}	
	
	public static FoodHeader toJavaFoodHeader(uk.ac.ncl.openlab.intake24.UserFoodHeader header) {
		return new FoodHeader(header.code(), header.localDescription());
	}
	
	public static List<FoodHeader> toJavaFoodHeaders(Seq<uk.ac.ncl.openlab.intake24.UserFoodHeader> headers) {
		Iterator<uk.ac.ncl.openlab.intake24.UserFoodHeader> iter = headers.iterator();
		
		List<FoodHeader> result = new ArrayList<FoodHeader>();

		while (iter.hasNext()) {			
			uk.ac.ncl.openlab.intake24.UserFoodHeader header = iter.next();
			result.add(toJavaFoodHeader(header));
		}

		return result;		
	}
	
	public static CategoryHeader toJavaCategoryHeader(uk.ac.ncl.openlab.intake24.UserCategoryHeader header) {
		return new CategoryHeader(header.code(), header.localDescription());
	}
	
	public static List<CategoryHeader> toJavaCategoryHeaders(Seq<uk.ac.ncl.openlab.intake24.UserCategoryHeader> headers) {
		Iterator<uk.ac.ncl.openlab.intake24.UserCategoryHeader> iter = headers.iterator();
		
		List<CategoryHeader> result = new ArrayList<CategoryHeader>();

		while (iter.hasNext()) {			
			uk.ac.ncl.openlab.intake24.UserCategoryHeader header = iter.next();
			result.add(toJavaCategoryHeader(header));
		}

		return result;		
	}
	
	public static List<PortionSizeMethod> toJavaPortionSizeMethods(Seq<uk.ac.ncl.openlab.intake24.PortionSizeMethod> methods, String imageUrlBase) {
		Iterator<uk.ac.ncl.openlab.intake24.PortionSizeMethod> iter = methods.iterator();

		ArrayList<PortionSizeMethod> result = new ArrayList<PortionSizeMethod>();

		while (iter.hasNext()) {
			uk.ac.ncl.openlab.intake24.PortionSizeMethod next = iter.next();
			Iterator<PortionSizeMethodParameter> paramIter = next.parameters().iterator();

			HashMap<String, String> params = new HashMap<String, String>();

			while (paramIter.hasNext()) {
				PortionSizeMethodParameter param = paramIter.next();
				params.put(param.name(), param.value());
			}

			result.add(new PortionSizeMethod(next.method(), next.description(), imageUrlBase + "/" + next.imageUrl(), next.useForRecipes(), params));

		}

		return result;
	}
	
	public static FoodPrompt toJavaPrompt(uk.ac.ncl.openlab.intake24.AssociatedFood prompt) {
		return new FoodPrompt(prompt.category(), true, prompt.promptText(), prompt.linkAsMain(), prompt.genericName());
	}
	
	public static List<FoodPrompt> toJavaPrompts(Seq<uk.ac.ncl.openlab.intake24.AssociatedFood> prompts) {
		Iterator<uk.ac.ncl.openlab.intake24.AssociatedFood> iter = prompts.iterator();
		
		List<FoodPrompt> result = new ArrayList<FoodPrompt>();
		
		while (iter.hasNext()) {
			result.add(toJavaPrompt(iter.next()));
		}
		
		return result;		
	}
	
	public static List<String> toJavaList(Seq<String> seq) {
		Iterator<String> iter = seq.iterator();

		ArrayList<String> result = new ArrayList<String>();
		
		while (iter.hasNext()) {
			result.add(iter.next());
		}
		
		return result;
	}

	public static FoodData buildJavaFoodData(uk.ac.ncl.openlab.intake24.UserFoodData data, double calPer100g, Seq<uk.ac.ncl.openlab.intake24.AssociatedFood> prompts, Seq<String> brands, Seq<uk.ac.ncl.openlab.intake24.CategoryHeader> allSuperCategories, String imageUrlBase) {
		
		ArrayList<String> categoryCodes = new ArrayList<String>();
		
		Iterator<uk.ac.ncl.openlab.intake24.CategoryHeader> i = allSuperCategories.iterator();
		
		while (i.hasNext())
			categoryCodes.add(i.next().code());
						
		return new FoodData(data.code(), data.readyMealOption(), data.sameAsBeforeOption(), calPer100g, data.localDescription(), toJavaPortionSizeMethods(data.portionSize(), imageUrlBase),
				toJavaPrompts(prompts), toJavaList(brands), categoryCodes);
	}
}
