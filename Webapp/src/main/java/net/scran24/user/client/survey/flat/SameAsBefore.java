package net.scran24.user.client.survey.flat;

import org.pcollections.client.PVector;

import net.scran24.user.shared.EncodedFood;
import net.scran24.user.shared.FoodEntry;

public class SameAsBefore {
	public final EncodedFood mainFood;
	public final PVector<FoodEntry> linkedFoods;

	public SameAsBefore(EncodedFood mainFood, PVector<FoodEntry> linkedFoods) {
		this.mainFood = mainFood;
		this.linkedFoods = linkedFoods;
	}
}
