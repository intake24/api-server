/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.shared;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.scran24.datastore.shared.Time;

public class PromptDrivenSurveyData {
	public Time wakeupTime = null;
	public Time breakfastTime = null;
	public Time lunchTime = null;
	public Time eveningMealTime = null;

	public Map<MealType, List<String>> foodsAsTyped = new HashMap<MealType, List<String>>();
	public Map<MealType, List<EncodedFood>> encodedFoods = new HashMap<MealType, List<EncodedFood>>();

	public PromptDrivenSurveyData() {
		for (MealType t : MealType.values()) {
			foodsAsTyped.put(t, new ArrayList<String>());
			encodedFoods.put(t, new ArrayList<EncodedFood>());
		}
	}
}
