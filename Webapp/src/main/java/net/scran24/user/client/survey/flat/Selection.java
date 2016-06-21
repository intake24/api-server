/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
 */

package net.scran24.user.client.survey.flat;

public abstract class Selection {
	public interface Visitor<R> {
		public R visitMeal(SelectedMeal meal);

		public R visitFood(SelectedFood food);

		public R visitNothing(EmptySelection selection);
	}

	public static class SelectedFood extends Selection {
		public final int mealIndex;
		public final int foodIndex;

		public SelectedFood(int mealIndex, int foodIndex, SelectionMode selectionType) {
			super(selectionType);
			this.mealIndex = mealIndex;
			this.foodIndex = foodIndex;
		}

		@Override
		public <R> R accept(Visitor<R> visitor) {
			return visitor.visitFood(this);
		}
	}

	public static class SelectedMeal extends Selection {
		public final int mealIndex;

		public SelectedMeal(int mealIndex, SelectionMode selectionType) {
			super(selectionType);
			this.mealIndex = mealIndex;
		}

		@Override
		public <R> R accept(Visitor<R> visitor) {
			return visitor.visitMeal(this);
		}
	}

	public static class EmptySelection extends Selection {

		public EmptySelection(final SelectionMode selectionType) {
			super(selectionType);
		}

		@Override
		public <R> R accept(Visitor<R> visitor) {
			return visitor.visitNothing(this);
		}
	}

	public final SelectionMode selectionMode;

	public abstract <R> R accept(Visitor<R> visitor);

	public Selection(final SelectionMode type) {
		this.selectionMode = type;
	}

	@Override
	public String toString() {
		return this.accept(new Visitor<String>() {
			@Override
			public String visitMeal(SelectedMeal meal) {
				return "Meal(" + meal.mealIndex + ")";
			}

			@Override
			public String visitFood(SelectedFood food) {
				return "Food(" + food.mealIndex + ", " + food.foodIndex + ")";
			}

			@Override
			public String visitNothing(EmptySelection selection) {
				return "Empty";
			}
		}) + ((this.selectionMode == SelectionMode.AUTO_SELECTION) ? " auto" : " manual");
	}

	public boolean equals(final Selection other) {
		return this.accept(new Selection.Visitor<Boolean>() {
			@Override
			public Boolean visitMeal(final SelectedMeal thisMeal) {
				return other.accept(new Visitor<Boolean>() {
					@Override
					public Boolean visitMeal(SelectedMeal otherMeal) {
						return thisMeal.mealIndex == otherMeal.mealIndex;
					}

					@Override
					public Boolean visitFood(SelectedFood food) {
						return false;
					}

					@Override
					public Boolean visitNothing(EmptySelection selection) {
						return false;
					}
				});
			}

			@Override
			public Boolean visitFood(final SelectedFood thisFood) {
				return other.accept(new Visitor<Boolean>() {
					@Override
					public Boolean visitMeal(SelectedMeal otherMeal) {
						return false;
					}

					@Override
					public Boolean visitFood(SelectedFood food) {
						return (thisFood.mealIndex == food.mealIndex) && (thisFood.foodIndex == food.foodIndex);
					}

					@Override
					public Boolean visitNothing(EmptySelection selection) {
						return false;
					}
				});
			}

			@Override
			public Boolean visitNothing(EmptySelection selection) {
				return other.accept(new Visitor<Boolean>() {
					@Override
					public Boolean visitMeal(SelectedMeal otherMeal) {
						return false;
					}

					@Override
					public Boolean visitFood(SelectedFood food) {
						return false;
					}

					@Override
					public Boolean visitNothing(EmptySelection selection) {
						return true;
					}
				});
			}
		});
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Selection other = (Selection) obj;
		return this.equals(other);
	}

}