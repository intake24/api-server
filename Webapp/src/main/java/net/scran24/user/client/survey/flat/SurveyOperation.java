/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey.flat;

import net.scran24.user.shared.FoodEntry;

import org.pcollections.client.PVector;
import org.workcraft.gwt.shared.client.Function1;

public abstract class SurveyOperation {
	public static interface Visitor<R> {
		R visitNoChange();
		R visitAddMeal(int selectedIndex);
		R visitEditFoodsRequest(int mealIndex, boolean addDrink);
		R visitEditTimeRequest(int mealIndex);
		R visitDeleteMealRequest(int mealIndex, boolean showConfirmation);
		R visitUpdate(Update update);
		R visitSplitFood(int mealIndex, int foodIndex, PVector<FoodEntry> splitInto);
	}
	
	public static interface SideEffectVisitor {
		void visitNoChange();
		void visitAddMeal(int selectedIndex);
		void visitEditFoodsRequest(int mealIndex, boolean addDrink);
		void visitEditTimeRequest(int mealIndex);
		void visitUpdate(Update update);
		void visitDeleteMealRequest(int mealIndex, boolean showConfirmation);
		void visitSplitFood(int mealIndex, int foodIndex, PVector<FoodEntry> splitInto);
	}
	
	public static class NoChange extends SurveyOperation {
		@Override
		public <R> R accept(Visitor<R> visitor) {
			return visitor.visitNoChange();
		}

		@Override
		public void accept(SideEffectVisitor visitor) {
			visitor.visitNoChange();			
		}
	}
	
	public static class EditFoodsRequest extends SurveyOperation {
		public final int mealIndex;
		public final boolean addDrink;

		public EditFoodsRequest(int mealIndex, boolean addDrink) {
			this.mealIndex = mealIndex;
			this.addDrink = addDrink;
		}

		@Override
		public <R> R accept(Visitor<R> visitor) {
			return visitor.visitEditFoodsRequest(mealIndex, addDrink);			
		}

		@Override
		public void accept(SideEffectVisitor visitor) {
			visitor.visitEditFoodsRequest(mealIndex, addDrink);		
		}
	}
	
	public static class EditMealTimeRequest extends SurveyOperation {
		private final int mealIndex;
		public EditMealTimeRequest (final int mealIndex) {
			this.mealIndex = mealIndex;
		}
		@Override
		public <R> R accept(Visitor<R> visitor) {
			return visitor.visitEditTimeRequest(mealIndex);
		}
		@Override
		public void accept(SideEffectVisitor visitor) {
			visitor.visitEditTimeRequest(mealIndex);			
		}
	}
	
	public static class Update extends SurveyOperation {
		public final Function1<Survey, Survey> update;
		public final boolean makeHistoryEntry;
		
		public Update(Function1<Survey, Survey> update) {
			this(update, true);
		}
		
		public Update(Function1<Survey, Survey> update, boolean makeHistoryEntry) {
			this.update = update;
			this.makeHistoryEntry = makeHistoryEntry;
		}

		@Override
		public <R> R accept(Visitor<R> visitor) {
			return visitor.visitUpdate(this);
		}

		@Override
		public void accept(SideEffectVisitor visitor) {
			visitor.visitUpdate(this);
		}
	}
	

	public static class DeleteMealRequest extends SurveyOperation {
		private final int mealIndex;
		private final boolean showConfirmation;

		public DeleteMealRequest(final int mealIndex, boolean showConfirmation) {
			this.mealIndex = mealIndex;
			this.showConfirmation = showConfirmation;
		}
		 
		@Override
		public <R> R accept(Visitor<R> visitor) {
			return visitor.visitDeleteMealRequest(mealIndex, showConfirmation);
		}

		@Override
		public void accept(SideEffectVisitor visitor) {
			visitor.visitDeleteMealRequest(mealIndex, showConfirmation);
		}
	}
	
	public static class SplitFood extends SurveyOperation {
		public final int mealIndex;
		public final int foodIndex;
		public final PVector<FoodEntry> splitInto;

		public SplitFood(int mealIndex, int foodIndex, PVector<FoodEntry> splitInto) {
			this.mealIndex = mealIndex;
			this.foodIndex = foodIndex;
			this.splitInto = splitInto;
		}

		@Override
		public <R> R accept(Visitor<R> visitor) {
			return visitor.visitSplitFood(mealIndex, foodIndex, splitInto);
		}

		@Override
		public void accept(SideEffectVisitor visitor) {
			visitor.visitSplitFood(mealIndex, foodIndex, splitInto);
		}
	}
	
	public static class AddMealRequest extends SurveyOperation {
		public final int selectedIndex;
		
		public AddMealRequest(int selectedIndex) {
			this.selectedIndex = selectedIndex;
		}

		@Override
		public <R> R accept(Visitor<R> visitor) {
			return visitor.visitAddMeal(selectedIndex);
		}

		@Override
		public void accept(SideEffectVisitor visitor) {
			visitor.visitAddMeal(selectedIndex);
		}
	}
	
	public abstract <R> R accept (Visitor<R> visitor);
	
	public abstract void accept (SideEffectVisitor visitor);
	
	public static final SurveyOperation noChange = new NoChange();
	
	public static SurveyOperation addMealRequest(int selectedIndex) {
		return new AddMealRequest(selectedIndex);
	}
		
	public static SurveyOperation editMealTimeRequest(int mealIndex) {
		return new EditMealTimeRequest(mealIndex);
	}
	
	public static SurveyOperation deleteMealRequest(int mealIndex, boolean showConfirmation) {
		return new DeleteMealRequest(mealIndex, showConfirmation);
	}
	
	public static SurveyOperation update(Function1<Survey, Survey> update) {
		return new Update(update, true);
	}
	
	public static SurveyOperation update(Function1<Survey, Survey> update, boolean makeHistoryEntry) {
		return new Update(update, makeHistoryEntry);
	}
}