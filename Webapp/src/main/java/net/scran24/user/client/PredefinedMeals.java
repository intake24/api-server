/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client;

import net.scran24.user.client.survey.prompts.messages.PromptMessages;
import net.scran24.user.shared.Meal;

import org.pcollections.client.PVector;
import org.pcollections.client.TreePVector;

import com.google.gwt.core.client.GWT;

public class PredefinedMeals {
	private static PromptMessages messages =  GWT.create(PromptMessages.class);
		
	public static final PVector<Meal> startingMeals = TreePVector.<Meal> empty()
	.plus(Meal.empty(messages.predefMeal_Breakfast()))
	.plus(Meal.empty(messages.predefMeal_EarlySnack()))
	.plus(Meal.empty(messages.predefMeal_Lunch()))
	.plus(Meal.empty(messages.predefMeal_MidDaySnack()))
	.plus(Meal.empty(messages.predefMeal_EveningMeal()))
	.plus(Meal.empty(messages.predefMeal_LateSnack()));
	
	public static final String[] mealNameChoice = { 
			messages.predefMeal_EarlySnack(),
			messages.predefMeal_Breakfast(),
			messages.predefMeal_Snack(),
			messages.predefMeal_Lunch(),
			messages.predefMeal_MidDaySnack(),
			messages.predefMeal_Dinner(),
			messages.predefMeal_EveningMeal(),
			messages.predefMeal_LateSnack() 
			};
}
