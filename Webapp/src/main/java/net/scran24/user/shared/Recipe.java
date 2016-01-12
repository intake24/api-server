/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.shared;

import org.pcollections.client.PVector;

public class Recipe {	
	public final static String SERVINGS_NUMBER_KEY = "servings-number";
	public final static String IS_SAVED_FLAG = "saved";
	
	public final TemplateFood mainFood;
	public final PVector<FoodEntry> ingredients;
	
	public Recipe(TemplateFood mainFood, PVector<FoodEntry> ingredients) {
		this.mainFood = mainFood;
		this.ingredients = ingredients;
	}		
}
