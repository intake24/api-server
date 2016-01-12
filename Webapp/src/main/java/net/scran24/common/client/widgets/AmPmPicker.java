/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.common.client.widgets;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

public class AmPmPicker extends Composite {
	private boolean amSelected = true;
	
	final Label amLabel = new Label("AM");
	final Label pmLabel = new Label("PM");
	
	public boolean isAmSelected() {
		return amSelected;
	}
	
	private void selectAm () {
		amLabel.removeStyleName("ampmDeselected");
		amLabel.addStyleName("ampmSelected");
		
		pmLabel.addStyleName("ampmDeselected");
		pmLabel.removeStyleName("ampmSelected");
	}
	
	private void selectPm() {
		
		pmLabel.removeStyleName("ampmDeselected");
		pmLabel.addStyleName("ampmSelected");
		
		amLabel.addStyleName("ampmDeselected");
		amLabel.removeStyleName("ampmSelected");
	}
	
	public AmPmPicker (boolean startWithAm) {
		VerticalPanel panel = new VerticalPanel();
		
		amLabel.addStyleName("ampmLabel");
		pmLabel.addStyleName("ampmLabel");
		
		if (startWithAm) 
			selectAm();
		else
			selectPm();
		
		amLabel.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				amSelected = true;
				selectAm();			
			}
		});
		
		pmLabel.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				amSelected = false;
				selectPm();
			}
		});
		
		panel.add (amLabel);
		panel.add (pmLabel);
		
		initWidget(panel);
	}
}