/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey.foodlist;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class LookupFoodList extends VerticalPanel {
	private final List<LookupFoodListItem> items = new ArrayList<LookupFoodListItem>();
	private final Widget placeholder;

	public LookupFoodList(final List<String> foods, final Widget placeholder) {
		this.placeholder = placeholder;

		setSpacing(0);
		setBorderWidth(0);
		setVerticalAlignment(ALIGN_MIDDLE);

		addStyleName("scran24-food-list");

		if (foods.isEmpty())
			add(this.placeholder);
		else
			for (String food : foods) {
				LookupFoodListItem item = new LookupFoodListItem(food);
				items.add(item);
				
				add(item);
			}
	}
}