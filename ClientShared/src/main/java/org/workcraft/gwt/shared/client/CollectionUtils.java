/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package org.workcraft.gwt.shared.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.pcollections.client.HashTreePMap;
import org.pcollections.client.HashTreePSet;
import org.pcollections.client.PMap;
import org.pcollections.client.PSet;
import org.pcollections.client.PVector;
import org.pcollections.client.TreePVector;

public class CollectionUtils {
	public static class WithIndex<T> {
		public final T value;
		public final int index;

		public WithIndex(T value, int index) {
			this.value = value;
			this.index = index;
		}
	}

	public static <T> Function1<T, T> identity() {
		return new Function1<T, T>() {
			@Override
			public T apply(T argument) {
				return argument;
			}
		};
	}

	public static <T> PVector<Integer> indices(PVector<T> v) {
		PVector<Integer> res = TreePVector.<Integer> empty();

		for (int i = 0; i < v.size(); i++)
			res = res.plus(i);

		return res;
	}

	public static <T> boolean exists(Collection<? extends T> collection, Function1<T, Boolean> predicate) {
		for (T elem : collection)
			if (predicate.apply(elem))
				return true;
		return false;
	}

	public static <T> boolean forall(Collection<? extends T> collection, final Function1<T, Boolean> predicate) {
		return !exists(collection, new Function1<T, Boolean>() {
			@Override
			public Boolean apply(T argument) {
				return !predicate.apply(argument);
			}
		});
	}

	public static <T> int indexFrom(List<? extends T> collection, final Function1<T, Boolean> predicate, int startIndex) {
		for (int idx = startIndex; idx < collection.size(); idx++)
			if (predicate.apply(collection.get(idx)))
				return idx;
		
		return -1;
	}

	public static <T> int indexOf(List<? extends T> collection, final Function1<T, Boolean> predicate) {
		return indexFrom(collection, predicate, 0);
	}

	public static <T> int lastIndexOf(List<? extends T> collection, final Function1<T, Boolean> predicate) {
		int lastSeenAt = indexOf(collection, predicate);
		
		if (lastSeenAt == -1)
			return -1;
		
		while (true) {
			int nextIndex = indexFrom(collection, predicate, lastSeenAt + 1);
			if (nextIndex == -1)
				return lastSeenAt;
			lastSeenAt = nextIndex; 
		}
	}

	public static <T> PVector<T> sort(PVector<T> vec, Comparator<T> cmp) {
		ArrayList<T> tmp = new ArrayList<T>();
		for (T t : vec)
			tmp.add(t);
		Collections.sort(tmp, cmp);
		return TreePVector.<T> empty().plusAll(tmp);
	}

	public static <T1, T2> PVector<T2> map(PVector<T1> vec, Function1<T1, T2> func) {
		PVector<T2> acc = TreePVector.<T2> empty();
		for (T1 elem : vec)
			acc = acc.plus(func.apply(elem));
		return acc;
	}
	
	public static <T1, T2> PSet<T2> map(PSet<T1> set, Function1<T1, T2> func) {
		PSet<T2> acc = HashTreePSet.<T2> empty();
		for (T1 elem : set)
			acc = acc.plus(func.apply(elem));
		return acc;
	}

	public static <T1, T2> T2 foldl(PVector<T1> vec, T2 initial, Function2<T2, T1, T2> func) {
		T2 acc = initial;
		for (T1 elem : vec)
			acc = func.apply(acc, elem);
		return acc;
	}

	public static <T> PVector<WithIndex<T>> zipWithIndex(PVector<T> vec) {
		PVector<WithIndex<T>> acc = TreePVector.<WithIndex<T>> empty();
		for (int i = 0; i < vec.size(); i++)
			acc = acc.plus(new WithIndex<T>(vec.get(i), i));
		return acc;
	}

	public static <T> PVector<T> filter(PVector<T> vec, Function1<T, Boolean> predicate) {
		PVector<T> acc = TreePVector.<T> empty();
		for (T elem : vec)
			if (predicate.apply(elem))
				acc = acc.plus(elem);
		return acc;
	}

	public static <T> PSet<T> filter(PSet<T> vec, Function1<T, Boolean> predicate) {
		PSet<T> acc = HashTreePSet.<T> empty();
		for (T elem : vec)
			if (predicate.apply(elem))
				acc = acc.plus(elem);
		return acc;
	}

	public static <T> PVector<T> flatten(PVector<PVector<T>> vec) {
		PVector<T> acc = TreePVector.<T> empty();
		for (int i = 0; i < vec.size(); i++) {
			PVector<T> subVec = vec.get(i);
			for (int j = 0; j < subVec.size(); j++)
				acc = acc.plus(subVec.get(j));
		}
		return acc;
	}

	public static <T1, T2> PVector<T2> flatMap(PVector<T1> vec, Function1<T1, PVector<T2>> f) {
		return flatten(map(vec, f));
	}

	public static <T> Option<T> flattenOption(Option<Option<T>> opt) {
		return opt.accept(new Option.Visitor<Option<T>, Option<T>>() {
			@Override
			public Option<T> visitSome(Option<T> item) {
				return item;
			}

			@Override
			public Option<T> visitNone() {
				return Option.none();
			}
		});
	}

	public static <T> PVector<T> flattenOption(PVector<Option<T>> vec) {
		return map(filter(vec, new Function1<Option<T>, Boolean>() {
			@Override
			public Boolean apply(Option<T> arg) {
				return !arg.isEmpty();
			}
		}), new Function1<Option<T>, T>() {
			@Override
			public T apply(Option<T> argument) {
				return argument.getOrDie();
			}
		});
	}
	
	public static <A, B> Option<B> bind (Option<A> opt, Function1<A, Option<B>> f) {
		return flattenOption(opt.map(f));		
	}

	public static <T> PVector<T> removeAll(PVector<T> vec, PSet<Integer> indices) {
		PVector<T> result = vec;

		for (Integer i : indices)
			result = result.minus(i);

		return result;
	}

	public static <K, T1, T2> PMap<K, T2> mapValues(PMap<K, T1> map, Function1<T1, T2> f) {
		PMap<K, T2> res = HashTreePMap.<K, T2> empty();

		for (K key : map.keySet())
			res = res.plus(key, f.apply(map.get(key)));

		return res;
	}
	
	public static <T> Option<T> chooseFirst(PVector<Option<T>> options) {
		for (Option<T> opt: options)
			if (!opt.isEmpty())
				return opt;
		return Option.<T>none();
	}
}