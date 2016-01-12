/*
This file is part of Intake24.

Copyright 2015, 2016 Newcastle University.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

This file is based on Intake24 v1.0.

© Crown copyright, 2012, 2013, 2014

Licensed under the Open Government Licence 3.0: 

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.datastore.shared;

import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.rpc.IsSerializable;

public class Time implements IsSerializable {
	public int hours;
	public int minutes;

	@Deprecated
	public Time() {
	}

	public Time(int hours, int minutes) {
		checkBounds(hours, minutes);
		this.hours = hours;
		this.minutes = minutes;
	}

	public Time minusHours(int hours) {
		int result = (this.hours - hours) % 24;
		if (result < 0)
			return new Time(24 + result, minutes);
		else
			return new Time(result, minutes);
	}

	private void checkBounds(int hours, int minutes) {
		if (hours < 0 || hours > 23)
			throw new RuntimeException("Invalid value: hours = " + hours);
		if (minutes < 0 || minutes > 59)
			throw new RuntimeException("Invalid value: minutes = " + minutes);

	}

	private int toMinutes() {
		return hours * 60 + minutes;
	}

	private int minutesWrapAround(int minutes) {
		if (minutes < 0)
			return 1440 + minutes;
		else
			return minutes;
	}

	public boolean isBefore(Time t) {
		if (t.hours == hours)
			return minutes < t.minutes;
		else
			return hours < t.hours;
	}

	public boolean isAfter(Time t) {
		if (t.hours == hours)
			return minutes > t.minutes;
		else
			return hours > t.hours;
	}

	public int minutesAfter(Time t) {
		int t1 = toMinutes();
		int t2 = t.toMinutes();

		return minutesWrapAround(t1 - t2);
	}

	public int minutesBefore(Time t) {
		int t1 = toMinutes();
		int t2 = t.toMinutes();

		return minutesWrapAround(t2 - t1);
	}

	public String toString() {
		NumberFormat nf = NumberFormat.getFormat("00");
		return nf.format(hours) + ":" + nf.format(minutes);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + hours;
		result = prime * result + minutes;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Time other = (Time) obj;
		if (hours != other.hours)
			return false;
		if (minutes != other.minutes)
			return false;
		return true;
	}		
}
