/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.test;

import static org.junit.Assert.assertEquals;
import net.scran24.datastore.shared.Time;

import org.junit.Test;

public class TestTime {
	@Test
	public void testMinusHours() {
		Time t = new Time(10, 0);
		
		Time t1 = t.minusHours(8);
		Time t2 = t.minusHours(10);
		Time t3 = t.minusHours(0);
		Time t4 = t.minusHours(16);
		
		assertEquals (t1.minutes, 0);
		assertEquals (t2.minutes, 0);
		assertEquals (t3.minutes, 0);
		assertEquals (t4.minutes, 0);
		
		assertEquals (2, t1.hours);
		assertEquals (0, t2.hours);
		assertEquals (10, t3.hours);
		assertEquals (18, t4.hours);
	}
	
	@Test 
	public void testMinutesBefore() {
		Time t = new Time(10, 0);
		
		Time t1 = new Time(4, 0);
		Time t2 = new Time(10, 30);
		Time t3 = new Time (15, 15);
		
		assertEquals(0, t.minutesBefore(t));
		assertEquals(1080, t.minutesBefore(t1));
		assertEquals(30, t.minutesBefore(t2));
		assertEquals(315, t.minutesBefore(t3));
	}
	
	@Test 
	public void testMinutesAfter() {
		Time t = new Time(10, 0);
		
		Time t1 = new Time(4, 0);
		Time t2 = new Time(10, 30);
		Time t3 = new Time (15, 15);
		
		assertEquals(0, t.minutesAfter(t));
		assertEquals(360, t.minutesAfter(t1));
		assertEquals(1440 - 30, t.minutesAfter(t2));
		assertEquals(1125, t.minutesAfter(t3));
	}
}