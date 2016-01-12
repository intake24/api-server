/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey.foodlist;

import net.scran24.user.shared.EncodedFood;

import com.google.gwt.user.client.ui.HTMLPanel;

public class LookupFoodListItem extends HTMLPanel {
	private final String description;
	private EncodedFood resolved = null;

	public LookupFoodListItem(final String description) {
		super(description);
		this.description = description;
		
		addStyleName ("scran24-food-list-item");
		addStyleName ("scran24-food-list-text");
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setResolved (EncodedFood resolved) {
		this.resolved = resolved;

		// update UI
	}
	
	public EncodedFood getResolved() {
		return resolved;
	}
	
	public void enableHighlight() {
		addStyleName ("scran24-lookup-food-list-item-highlight");						
	}
	
	public void disableHighlight() {
		removeStyleName ("scran24-lookup-food-list-item-highlight");		
	}
}
