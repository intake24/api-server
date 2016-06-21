/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package org.workcraft.gwt.shared.client;

import java.util.NoSuchElementException;

import com.google.gwt.user.client.rpc.IsSerializable;

public abstract class Either<L, R> {
	public static interface Visitor<L, R, T> {
		public T visitRight(R value);
		public T visitLeft (L value);
	}
	
	public interface SideEffectVisitor<L, R> {
		public void visitRight(R value);
		public void visitLeft (L value);
	}

	public static class Right<L, R> extends Either <L, R> implements IsSerializable {
		public R value;
		
		@Deprecated
		public Right() { }
		
		public Right (R value) {
			this.value = value;
		}
		
		@Override
		public <T> T accept(org.workcraft.gwt.shared.client.Either.Visitor<L, R, T> visitor) {
			return visitor.visitRight(value);
		}
		
		@Override
		public String toString() {
			return "Right (" + value.toString() + ")";			
		}

		@Override
		public void accept(org.workcraft.gwt.shared.client.Either.SideEffectVisitor<L, R> visitor) {
			visitor.visitRight(value);
			
		}
	}
	
	public static class Left<L, R> extends Either <L, R> implements IsSerializable {
		public L value;
		
		@Deprecated
		public Left() { }
		
		public Left (L value) {
			this.value = value;
		}
		
		@Override
		public <T> T accept(org.workcraft.gwt.shared.client.Either.Visitor<L, R, T> visitor) {
			return visitor.visitLeft(value);
		}
		
		@Override
		public String toString() {
			return "Left (" + value.toString() + ")";			
		}

		@Override
		public void accept(org.workcraft.gwt.shared.client.Either.SideEffectVisitor<L, R> visitor) {
			visitor.visitLeft(value);
		}				
	}
	
	public boolean isLeft() {
		return accept (new Visitor<L, R, Boolean>() {
			@Override
			public Boolean visitRight(R value) {
				return false;
			}

			@Override
			public Boolean visitLeft(L value) {
				return true;
			}
		});
	}
	
	public boolean isRight() {
		return !isLeft();
	}
	
	public L getLeftOrDie() {
		return accept (new Visitor<L, R, L>() {
			@Override
			public L visitRight(R value) {
				throw new NoSuchElementException("getLeft on a Right"); 
			}

			@Override
			public L visitLeft(L value) {
				return value;
			}
		});		
	}
	
	public R getRightOrDie() {
		return accept (new Visitor<L, R, R>() {
			@Override
			public R visitRight(R value) {
				return value; 
			}

			@Override
			public R visitLeft(L value) {
				throw new NoSuchElementException("getRight on a Left");
			}
		});		
	}
	
	
	public abstract <T> T accept (Visitor<L, R, T> visitor);
	public abstract void accept (SideEffectVisitor<L, R> visitor);
	
	
	public boolean equalsTo (final Either<L, R> other) {
		return accept (new Either.Visitor<L, R, Boolean>() {
			@Override
			public Boolean visitRight(final R value1) {
				return other.accept(new Either.Visitor<L, R, Boolean>(){
					@Override
					public Boolean visitRight(R value2) {
						return value1.equals(value2);
					}

					@Override
					public Boolean visitLeft(L value) {
						return false;
					}
					
				});
			}

			@Override
			public Boolean visitLeft(final L value1) {
				return other.accept(new Either.Visitor<L, R, Boolean>(){
					@Override
					public Boolean visitRight(R value2) {
						return false;
					}

					@Override
					public Boolean visitLeft(L value2) {
						return value1.equals(value2);
					}
				});
			}
		}); 		
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (obj.getClass() != this.getClass())
			return false;
		return equalsTo((Either<L,R>)obj);	
	}
}