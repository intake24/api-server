/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package org.workcraft.gwt.shared.client;

import com.google.gwt.user.client.rpc.IsSerializable;

public class Pair<L, R> implements IsSerializable {
	public L left;
	public R right;
	
	@Deprecated
	public Pair() {}

	public Pair(L left, R right) {
		this.left = left;
		this.right = right;
	}

	public static <L, R> Pair<L, R> create(L left, R right) {
		return new Pair<L, R>(left, right);
	}
}
