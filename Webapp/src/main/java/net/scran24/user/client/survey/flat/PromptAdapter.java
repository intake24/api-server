/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey.flat;

import net.scran24.user.client.survey.SurveyStageInterface;
import net.scran24.user.client.survey.prompts.MealOperation;
import net.scran24.user.client.survey.prompts.MealOperation.DeleteRequest;
import net.scran24.user.client.survey.prompts.MealOperation.Visitor;
import net.scran24.user.shared.FoodEntry;
import net.scran24.user.shared.Meal;

import org.pcollections.client.PVector;
import org.workcraft.gwt.shared.client.Callback1;
import org.workcraft.gwt.shared.client.Function1;
import org.workcraft.gwt.shared.client.Pair;

public abstract class PromptAdapter<T, Op> implements Prompt<Survey, SurveyOperation> {
	private final Prompt<T, Op> adaptee;

	public abstract SurveyOperation apply(Op operation);

	public abstract Function1<Survey, Survey> updateIntermediate(Function1<T, T> update);

	public PromptAdapter(final Prompt<T, Op> adaptee) {
		this.adaptee = adaptee;
	}

	@Override
	public SurveyStageInterface getInterface(final Callback1<SurveyOperation> onComplete,
			final Callback1<Function1<Survey, Survey>> updateIntermediateState) {
		return adaptee.getInterface(new Callback1<Op>() {
			@Override
			public void call(Op arg1) {
				onComplete.call(apply(arg1));
			}
		}, new Callback1<Function1<T, T>>() {
			@Override
			public void call(Function1<T, T> arg1) {
				updateIntermediateState.call(updateIntermediate(arg1));
			}
		});
	}

	public static class ForMeal extends PromptAdapter<Meal, MealOperation> {
		public final int mealIndex;

		public ForMeal(final int mealIndex, Prompt<Meal, MealOperation> prompt) {
			super(prompt);
			this.mealIndex = mealIndex;
		}

		@Override
		public SurveyOperation apply(MealOperation operation) {
			return operation.accept(new Visitor<SurveyOperation>() {
				@Override
				public SurveyOperation visitNoChange() {
					return SurveyOperation.noChange;
				}

				@Override
				public SurveyOperation visitDeleteRequest(DeleteRequest request) {
					return SurveyOperation.deleteMealRequest(mealIndex, request.showConfirmation);
				}

				@Override
				public SurveyOperation visitEditFoodsRequest(boolean addDrink) {
					return new SurveyOperation.EditFoodsRequest(mealIndex, addDrink);
				}

				@Override
				public SurveyOperation visitEditTimeRequest() {
					return SurveyOperation.editMealTimeRequest(mealIndex);
				}

				@Override
				public SurveyOperation visitUpdate(final Function1<Meal, Meal> update) {
					return SurveyOperation.update(new Function1<Survey, Survey>() {
						@Override
						public Survey apply(Survey argument) {
							return argument.updateMeal(mealIndex, update.apply(argument.meals.get(mealIndex)));
						}
					}, true);
				}
			});
		}

		@Override
		public Function1<Survey, Survey> updateIntermediate(final Function1<Meal, Meal> update) {
			return new Function1<Survey, Survey>() {
				@Override
				public Survey apply(Survey argument) {
					return argument.updateMeal(mealIndex, update.apply(argument.meals.get(mealIndex)));
				}
			};
		}
	}

	public static class ForFoodExtended extends PromptAdapter<Pair<FoodEntry, Meal>, MealOperation> {
		public final int mealIndex;

		public ForFoodExtended(final int mealIndex, Prompt<Pair<FoodEntry, Meal>, MealOperation> prompt) {
			super(prompt);
			this.mealIndex = mealIndex;
		}

		@Override
		public SurveyOperation apply(MealOperation operation) {
			return operation.accept(new Visitor<SurveyOperation>() {
				@Override
				public SurveyOperation visitNoChange() {
					return SurveyOperation.noChange;
				}

				@Override
				public SurveyOperation visitDeleteRequest(DeleteRequest request) {
					return SurveyOperation.deleteMealRequest(mealIndex, request.showConfirmation);
				}

				@Override
				public SurveyOperation visitEditFoodsRequest(boolean addDrink) {
					return new SurveyOperation.EditFoodsRequest(mealIndex, addDrink);
				}

				@Override
				public SurveyOperation visitEditTimeRequest() {
					return SurveyOperation.editMealTimeRequest(mealIndex);
				}

				@Override
				public SurveyOperation visitUpdate(final Function1<Meal, Meal> update) {
					return SurveyOperation.update(new Function1<Survey, Survey>() {
						@Override
						public Survey apply(Survey argument) {
							return argument.updateMeal(mealIndex, update.apply(argument.meals.get(mealIndex)));
						}
					}, true);
				}
			});
		}

		@Override
		public Function1<Survey, Survey> updateIntermediate(final Function1<Pair<FoodEntry, Meal>, Pair<FoodEntry, Meal>> update) {
			return new Function1<Survey, Survey>() {
				@Override
				public Survey apply(Survey argument) {
					return argument.updateMeal(mealIndex, update.apply(Pair.<FoodEntry, Meal>create(null, argument.meals.get(mealIndex))).right);
				}
			};
		}
	}

	public static class ForFood extends PromptAdapter<FoodEntry, FoodOperation> {
		public final int mealIndex;
		private final int foodIndex;

		public ForFood(final int mealIndex, final int foodIndex, Prompt<FoodEntry, FoodOperation> prompt) {
			super(prompt);
			this.mealIndex = mealIndex;
			this.foodIndex = foodIndex;
		}

		@Override
		public SurveyOperation apply(FoodOperation operation) {
			return operation.accept(new FoodOperation.Visitor<SurveyOperation>() {
				@Override
				public SurveyOperation visitNoChange() {
					return SurveyOperation.noChange;
				}

				@Override
				public SurveyOperation visitDeleteRequest() {
					return SurveyOperation.update(new Function1<Survey, Survey>() {
						@Override
						public Survey apply(Survey argument) {
							return argument.updateMeal(mealIndex, argument.meals.get(mealIndex).minusFood(foodIndex))
									.withSelection(new Selection.SelectedMeal(mealIndex, SelectionMode.AUTO_SELECTION));
						}
					}, true);
				}

				@Override
				public SurveyOperation visitEditFoodsRequest() {
					return new SurveyOperation.EditFoodsRequest(mealIndex, false);
				}

				@Override
				public SurveyOperation visitUpdate(final FoodOperation.Update update) {
					return SurveyOperation.update(new Function1<Survey, Survey>() {
						@Override
						public Survey apply(Survey argument) {
							return argument.updateMeal(mealIndex,
									argument.meals.get(mealIndex).updateFood(foodIndex, update.update.apply(argument.meals.get(mealIndex).foods.get(foodIndex))));
						}
					}, update.makeHistoryEntry);
				}

				@Override
				public SurveyOperation visitSplit(PVector<FoodEntry> into) {
					return new SurveyOperation.SplitFood(mealIndex, foodIndex, into);
				}
			});
		}

		@Override
		public Function1<Survey, Survey> updateIntermediate(final Function1<FoodEntry, FoodEntry> update) {
			return new Function1<Survey, Survey>() {
				@Override
				public Survey apply(Survey argument) {
					return argument.updateMeal(mealIndex,
							argument.meals.get(mealIndex).updateFood(foodIndex, update.apply(argument.meals.get(mealIndex).foods.get(foodIndex))));
				}
			};
		}
	}
}