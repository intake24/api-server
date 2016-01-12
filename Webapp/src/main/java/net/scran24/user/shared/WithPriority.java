/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.shared;

import java.util.Comparator;

public class WithPriority<T> {
	public final int priority;
	public final T value;

	public WithPriority(T value, int priority) {
		this.priority = priority;
		this.value = value;
	}
	
	public static <T> Comparator<WithPriority<T>> comparator() {
		return new Comparator<WithPriority<T>>() {
			@Override
			public int compare(WithPriority<T> o1, WithPriority<T> o2) {
				// Highest priority first
				return o2.priority - o1.priority;
			}
		};
	}
	
	@Override
	public String toString() {
		return value.toString() + "(" + Integer.toString(priority) + ")";
	}
}
