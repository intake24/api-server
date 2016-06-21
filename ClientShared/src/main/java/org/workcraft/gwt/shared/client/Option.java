/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package org.workcraft.gwt.shared.client;

import java.util.NoSuchElementException;

import com.google.gwt.user.client.rpc.IsSerializable;

public abstract class Option<T> implements IsSerializable {
	public interface Visitor<T, R> {
		public R visitSome(T item);

		public R visitNone();
	}
	
	public interface SideEffectVisitor<T> {
		public void visitSome(T item);
		public void visitNone();
	}


	public static class Some<T> extends Option<T> implements IsSerializable{
		public T item;
		
		@Deprecated
		public Some() { }

		public Some(T item) {
			this.item = item;
		}

		@Override
		public <R> R accept(Visitor<T, R> visitor) {
			return visitor.visitSome(item);
		}

		@Override
		public void accept(SideEffectVisitor<T> visitor) {
			visitor.visitSome(item);
		}
		
		@Override
		public String toString() {
			return "Some(" + item.toString() + ")";
		}
	}

	public static class None<T> extends Option<T> implements IsSerializable {
		@Override
		public <R> R accept(Visitor<T, R> visitor) {
			return visitor.visitNone();
		}

		@Override
		public void accept(SideEffectVisitor<T> visitor) {
			visitor.visitNone();			
		}
		
		@Override
		public String toString() {
			return "None";
		}
	}

	public abstract <R> R accept(Visitor<T, R> visitor);
	
	public abstract void accept(SideEffectVisitor<T> visitor);

	public static <T> Option<T> none() {
		return new Option.None<T>();
	}

	public static <T> Option<T> some(T item) {
		return new Option.Some<T>(item);
	}
	
	public static <T> Option<T> fromNullable(T item) {
		if (item == null)
			return none();
		else
			return some(item);
	}

	public boolean isEmpty() {
		return accept(new Visitor<T, Boolean>() {
			@Override
			public Boolean visitSome(T item) {
				return false;
			}

			@Override
			public Boolean visitNone() {
				return true;
			}
		});
	}
	
	public T getOrElse(final T def) {
		return accept(new Visitor<T, T>() {
			@Override
			public T visitSome(T item) {
				return item;
			}

			@Override
			public T visitNone() {
				return def;
			}
		});
	}
	
	public T getOrDie(final String message) {
		return accept(new Visitor<T, T>() {
			@Override
			public T visitSome(T item) {
				return item;
			}

			@Override
			public T visitNone() {
				throw new NoSuchElementException(message);
			}
		});
	}
	
	public T getOrDie() {
		return getOrDie("getOrDie from a None");
	}
	
	public <T2> Option<T2> map (final Function1<T, T2> func) {
		return this.accept(new Visitor<T, Option<T2>>() {

			@Override
			public Option<T2> visitSome(T item) {
				return new Option.Some<T2>(func.apply(item));
			}

			@Override
			public Option<T2> visitNone() {
				return new Option.None<T2>();
			}
		});		
	}
	
	public boolean equalTo (final Option<T> other) {
		return accept (new Option.Visitor<T, Boolean>() {
			@Override
			public Boolean visitSome(final T item1) {
				return other.accept(new Option.Visitor<T, Boolean>() {
					@Override
					public Boolean visitSome(T item2) {
						return item1.equals(item2);
					}

					@Override
					public Boolean visitNone() {
						return false;
					}
				});
			}

			@Override
			public Boolean visitNone() {
				return other.accept(new Option.Visitor<T, Boolean>() {
					@Override
					public Boolean visitSome(T item) {
						return false;
					}

					@Override
					public Boolean visitNone() {
						return true;
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
		return equalTo((Option<T>)obj);	
	}	
}
