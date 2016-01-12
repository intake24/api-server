/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey.prompts;

import org.workcraft.gwt.shared.client.Function1;

import net.scran24.user.shared.TemplateFood;
import net.scran24.user.shared.EncodedFood;
import net.scran24.user.shared.FoodEntry;
import net.scran24.user.shared.Meal;

public abstract class MealOperation {
	public static interface Visitor<R> {
		R visitNoChange();
		R visitDeleteRequest(DeleteRequest request);
		R visitEditFoodsRequest(boolean addDrink);
		R visitEditTimeRequest();
		R visitUpdate(Function1<Meal, Meal> update);
	}

	public static class NoChange extends MealOperation {
		@Override
		public <R> R accept(Visitor<R> visitor) {
			return visitor.visitNoChange();
		}
	}

	public static class DeleteRequest extends MealOperation {
		public final boolean showConfirmation;
		
		public DeleteRequest(boolean showConfirmation) {
			this.showConfirmation = showConfirmation;
		}
		
		@Override
		public <R> R accept(Visitor<R> visitor) {
			return visitor.visitDeleteRequest(this);
		}
	}

	public static class EditFoodsRequest extends MealOperation {
		/* start editing a drink automatically 
		 * for DrinkReminderPrompt
		 * */
		final public boolean addDrink;
		
		public EditFoodsRequest(boolean addDrink) {
			this.addDrink = addDrink;
		}

		@Override
		public <R> R accept(Visitor<R> visitor) {
			return visitor.visitEditFoodsRequest(addDrink);
		}
	}

	public static class EditTimeRequest extends MealOperation {
		@Override
		public <R> R accept(Visitor<R> visitor) {
			return visitor.visitEditTimeRequest();
		}
	}

	public static class Update extends MealOperation {
		private final Function1<Meal, Meal> update;

		public Update(Function1<Meal, Meal> apply) {
			this.update = apply;
		}

		@Override
		public <R> R accept(Visitor<R> visitor) {
			return visitor.visitUpdate(update);
		}
	}
	

	public abstract <R> R accept(Visitor<R> visitor);

	public static final MealOperation noChange = new NoChange();
	
	public static final MealOperation editTimeRequest = new EditTimeRequest();
	
	public static MealOperation editFoodsRequest(boolean addDrink) {
		return new EditFoodsRequest(addDrink);
	}
	
	public static MealOperation deleteRequest(boolean showConfirmation) {
		return new DeleteRequest(showConfirmation);
	}

	public static MealOperation update(Function1<Meal, Meal> update) {
		return new Update(update);
	}
	
	public static MealOperation replaceFood(final int foodIndex, final FoodEntry newEntry) {
		return new Update(new Function1<Meal, Meal>() {
			@Override
			public Meal apply(Meal meal) {
				return meal.updateFood(foodIndex, newEntry);
			}
		});						
	}
	
	public static MealOperation updateEncodedFood(final int foodIndex, final Function1<EncodedFood, EncodedFood> update) {
		return new Update(new Function1<Meal, Meal>() {
			@Override
			public Meal apply(Meal meal) {
				return meal.updateFood(foodIndex, update.apply((EncodedFood)meal.foods.get(foodIndex)));
			}
		});				
	}
	
	public static MealOperation updateFood(final int foodIndex, final Function1<FoodEntry, FoodEntry> update) {
		return new Update(new Function1<Meal, Meal>() {
			@Override
			public Meal apply(Meal meal) {
				return meal.updateFood(foodIndex, update.apply(meal.foods.get(foodIndex)));
			}
		});		
	}
	
	public static MealOperation updateTemplateFood(final int foodIndex, final Function1<TemplateFood, TemplateFood> update) {
		return new Update(new Function1<Meal, Meal>() {
			@Override
			public Meal apply(Meal meal) {
				return meal.updateFood(foodIndex, update.apply((TemplateFood)meal.foods.get(foodIndex)));
			}
		});				
	}
	
	public static MealOperation setCustomDataField(final String key, final String value) {
		return new Update(new Function1<Meal, Meal>() {
			@Override
			public Meal apply(Meal argument) {
				return argument.withCustomDataField(key, value);
			}
		});
	}
}