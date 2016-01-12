/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package org.workcraft.gwt;


import org.junit.Test;
import org.workcraft.gwt.imagemap.shared.Point;
import org.workcraft.gwt.imagemap.shared.Polygon;

import static org.junit.Assert.assertEquals;

public class PolygonTest {
	
	@Test(expected = IllegalArgumentException.class)
	public void testTooShort() {
		Point points[] = new Point[] { new Point(0,0) };
		@SuppressWarnings("unused")
		Polygon p = new Polygon(points);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testMergedTooShort() {
		Point points[] = new Point[] { new Point(0,0), new Point(0,0), new Point(0,0), new Point(0,0), new Point(0,0) };
		
		@SuppressWarnings("unused")
		Polygon p = new Polygon(points);
	}
	
	@Test
	public void preprocessTest() {
		Point points[] = new Point[] { new Point(0,0), new Point(0,0), new Point(1,1), new Point(1,1), new Point(2,2) };
		Point points2[] = Polygon.preprocess(points);
		
		assertEquals (4, points2.length);
		assertEquals (new Point(0, 0), points2[0]);
		assertEquals (new Point(1, 1), points2[1]);
		assertEquals (new Point(2, 2), points2[2]);
		assertEquals (new Point(0, 0), points2[3]);		
	}
	
	@Test
	public void pointInsideTest1() {
		Point points[] = new Point[] { new Point(0,0), new Point(1, 0), new Point(1, 1), new Point(0, 1) };				
		Polygon p = new Polygon(points);

		assertEquals(true, p.isInside(new Point(0.5, 0.5)));
		assertEquals(false, p.isInside(new Point(2.0, 0.0)));
		assertEquals(false, p.isInside(new Point(0.0, 2.0)));
	}
	
	@Test
	public void pointInsideTest2() {
		Point points[] = new Point[] { new Point(0,0), new Point(0.5, 0.5), new Point(1,0), new Point (1, 1), new Point (0,1) };
		Polygon p = new Polygon(points);
		
		assertEquals(true, p.isInside(new Point(0.75, 0.5)));
		assertEquals(false, p.isInside(new Point(0.5, 0.49)));
		assertEquals(false, p.isInside(new Point(1.01, 0.5)));
	}
}
