/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.common.client.widgets;

import net.scran24.datastore.shared.Time;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;

public class TimePicker extends Composite {
	public final Counter hourCounter;
	public final Counter minuteCounter;
	/* final private AmPmPicker ampm;

	public Time getTime() {
		int ampmHours;
		
		if (ampm.isAmSelected()) {
			if (hourCounter.value == 12)
				ampmHours = 0;
			else
				ampmHours = hourCounter.value;
		} else {
			if (hourCounter.value == 12)
				ampmHours = 12;
			else
				ampmHours = hourCounter.value + 12;
		}
		
		return new Time (ampmHours, minuteCounter.value);		
	}*/

	public TimePicker (Time initialTime) {
		/*boolean isAm = false;
		
		if (initialTime.hours < 12)
			isAm = true;
		
		int ampmHours;
		
		if (isAm) {
			ampmHours = initialTime.hours;
		} else {
			ampmHours = initialTime.hours - 12;
		}
		
		if (ampmHours == 0)
			ampmHours = 12;*/

		
		hourCounter = new Counter(0, 23, 1, initialTime.hours, "00");
		minuteCounter = new Counter (0, 45, 15, initialTime.minutes, "00");
		// ampm = new AmPmPicker(initialTime.am);

		HorizontalPanel panel = new HorizontalPanel();
		panel.setSpacing(5);
		panel.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
		panel.add(hourCounter);
		Label label = new Label(":");
		panel.add(label);
		panel.add(minuteCounter);
		
		//panel.add(ampm);

		initWidget(panel);
		
		addStyleName("time-picker");
	}

	public Time getTime() {
		return new Time (hourCounter.value, minuteCounter.value);
	}
}