/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey.flat;

import net.scran24.user.shared.CompoundFood;
import net.scran24.user.shared.TemplateFood;
import net.scran24.user.shared.EncodedFood;
import net.scran24.user.shared.FoodEntry;
import net.scran24.user.shared.MissingFood;
import net.scran24.user.shared.RawFood;

import org.pcollections.client.PVector;
import org.workcraft.gwt.shared.client.Function1;

public abstract class FoodOperation {
	public static interface Visitor<R> {
		R visitNoChange();

		R visitDeleteRequest();

		R visitEditFoodsRequest();

		R visitUpdate(Update update);

		R visitSplit(PVector<FoodEntry> into);
	}

	public static class NoChange extends FoodOperation {
		@Override
		public <R> R accept(Visitor<R> visitor) {
			return visitor.visitNoChange();
		}
	}

	public static class DeleteRequest extends FoodOperation {
		@Override
		public <R> R accept(Visitor<R> visitor) {
			return visitor.visitDeleteRequest();
		}
	}

	public static class EditFoodsRequest extends FoodOperation {
		@Override
		public <R> R accept(Visitor<R> visitor) {
			return visitor.visitEditFoodsRequest();
		}
	}

	public static class Update extends FoodOperation {
		public final Function1<FoodEntry, FoodEntry> update;
		public final boolean makeHistoryEntry;

		public Update(Function1<FoodEntry, FoodEntry> update) {
			this(update, true);
		}

		public Update(Function1<FoodEntry, FoodEntry> update, boolean makeHistoryEntry) {
			this.update = update;
			this.makeHistoryEntry = makeHistoryEntry;
		}

		@Override
		public <R> R accept(Visitor<R> visitor) {
			return visitor.visitUpdate(this);
		}
	}

	public static class Split extends FoodOperation {
		private final PVector<FoodEntry> into;

		public Split(PVector<FoodEntry> into) {
			this.into = into;
		}

		@Override
		public <R> R accept(Visitor<R> visitor) {
			return visitor.visitSplit(into);
		}

	}

	public abstract <R> R accept(Visitor<R> visitor);

	public boolean retainFocus() {
		return false;
	}

	public static final FoodOperation noChange = new NoChange();

	public static FoodOperation replaceWith(final FoodEntry with) {
		return new Update(new Function1<FoodEntry, FoodEntry>() {
			@Override
			public FoodEntry apply(FoodEntry argument) {
				return with;
			}
		});
	}

	public static final FoodOperation deleteRequest = new DeleteRequest();
	public static final FoodOperation editFoodsRequest = new EditFoodsRequest();

	public static FoodOperation setCustomData(final String key, final String value) {
		return update(new Function1<FoodEntry, FoodEntry>() {
			@Override
			public FoodEntry apply(FoodEntry argument) {
				return argument.withCustomDataField(key, value);
			}
		});
	}
	
	public static FoodOperation update(Function1<FoodEntry, FoodEntry> update) {
		return new Update(update, true);
	}

	public static FoodOperation update(Function1<FoodEntry, FoodEntry> update, boolean makeHistoryEntry) {
		return new Update(update, makeHistoryEntry);
	}

	public static FoodOperation updateEncoded(final Function1<EncodedFood, EncodedFood> updateFunc) {
		return updateEncoded(updateFunc, true);
	}

	public static FoodOperation updateEncoded(final Function1<EncodedFood, EncodedFood> updateFunc, boolean makeHistoryEntry) {
		return update(new Function1<FoodEntry, FoodEntry>() {
			@Override
			public FoodEntry apply(FoodEntry argument) {
				return argument.accept(new FoodEntry.Visitor<FoodEntry>() {
					@Override
					public FoodEntry visitRaw(RawFood food) {
						throw new IllegalStateException("Cannot apply this operation to raw food");
					}

					@Override
					public FoodEntry visitEncoded(EncodedFood food) {
						return updateFunc.apply(food);
					}

					@Override
					public FoodEntry visitTemplate(TemplateFood food) {
						throw new IllegalStateException("Cannot apply this operation to template food");
					}

					@Override
					public FoodEntry visitMissing(MissingFood food) {
						throw new IllegalStateException("Cannot apply this operation to missing ood");
					}

					@Override
					public FoodEntry visitCompound(CompoundFood food) {
						throw new IllegalStateException("Cannot apply this operation to compound food");
					}
				});
			}
		}, makeHistoryEntry);
	}

	public static FoodOperation updateRaw(final Function1<RawFood, RawFood> updateFunc) {
		return update(new Function1<FoodEntry, FoodEntry>() {
			@Override
			public FoodEntry apply(FoodEntry argument) {
				return argument.accept(new FoodEntry.Visitor<FoodEntry>() {
					@Override
					public FoodEntry visitRaw(RawFood food) {
						return updateFunc.apply(food);
					}

					@Override
					public FoodEntry visitEncoded(EncodedFood food) {
						throw new IllegalStateException("Cannot apply this operation to encoded food");
					}

					@Override
					public FoodEntry visitTemplate(TemplateFood food) {
						throw new IllegalStateException("Cannot apply this operation to template food");
					}

					@Override
					public FoodEntry visitMissing(MissingFood food) {
						throw new IllegalStateException("Cannot apply this operation to missing food");						
					}

					@Override
					public FoodEntry visitCompound(CompoundFood food) {
						throw new IllegalStateException("Cannot apply this operation to compound food");					
					}
				});
			}
		});
	}

}