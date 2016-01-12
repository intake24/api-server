/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package org.workcraft.gwt.imagemap.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

public class Point implements IsSerializable {
	public double x;
	public double y;
	
	public Point() { }
	
	public Point(double x, double y) {
		this.x = x;
		this.y = y;
	};
	
	public boolean equals (Point other) {
		return (Math.abs(x - other.x) + Math.abs (y - other.y)) < 1E-6;		
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Point other = (Point) obj;
		return equals (other);
	}
	
	@Override
	public String toString() {
		return "(" + x + ", " + y + ")";
	}
}
