/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.shared;

public class MealTypeName {
	public static String nameForType(MealType type) {
		switch (type) {
		case BeforeBreakfast:
			return "Before-breakfast snacks";
		case Breakfast:
			return "Breakfast";
		case MidMorning:
			return "Mid-morning snacks";
		case Lunch:
			return "Lunch";
		case Afternoon:
			return "Afternoon snacks";
		case Evening:
			return "Evening meal";
		case LateEvening:
			return "Late evening snacks";
		default:
			throw new RuntimeException("Unknown type");
		}
	}
}
