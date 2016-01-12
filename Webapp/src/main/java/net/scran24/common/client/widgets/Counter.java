/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.common.client.widgets;

import net.scran24.common.client.WidgetFactory;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

public class Counter extends Composite {
	public int value;
	
	private final Label box = new Label();
	private final String format;
	
	public void update() {
		NumberFormat nf = NumberFormat.getFormat(format);
		box.setText(nf.format(value));
	}
	
	public int getValue() {
		return value;
	}
	
	public Counter (final int min, final int max, final int increment, final int initial, String format) {
		value = initial;
		this.format = format;
		
		
		VerticalPanel panel = new VerticalPanel ();
		
		panel.setStyleName("counterPanel");
		
		panel.setHorizontalAlignment(VerticalPanel.ALIGN_CENTER);
		Button inc = WidgetFactory.createButton("▲", new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				value += increment;
				if (value > max) value = min;
				update();
			}
		});
		
		inc.setStyleName("counterIncButton");
		
		Button dec = WidgetFactory.createButton ("▼", new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				value -= increment;
				if (value < min) value = max;
				update();
			}
		});
		
		dec.setStyleName("counterDecButton");
		
		box.setStyleName("counterTextBox");
		
		panel.add(inc);
		panel.add(box);
		panel.add(dec);
		
		update();
		
		initWidget(panel);
	}
}
