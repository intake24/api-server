/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey;

import java.util.ArrayList;
import java.util.List;

import net.scran24.user.shared.EncodedFood;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.CellList;
import com.google.gwt.user.cellview.client.CellList.Style;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SelectionChangeEvent.Handler;
import com.google.gwt.view.client.SingleSelectionModel;

public class FoodLookupList extends FlowPanel {
	public class EncodedFoodCell extends AbstractCell<EncodedFood> {
		@Override
		public void render(com.google.gwt.cell.client.Cell.Context context,
				EncodedFood value, SafeHtmlBuilder sb) {
			sb.appendHtmlConstant("<div class=\"scran24-food-lookup-cell\">");
			sb.appendEscaped(value.description());
			sb.appendHtmlConstant("</div>");
		}
	}
	
	interface R extends CellList.Resources {
		@Override
		@Source({"net/scran24/user/client/survey/foodLookupList.css"})
		public Style cellListStyle();
	}
	
	interface SelectionHandler {
		void selectionChanged (EncodedFood selection);
	}
	
	public FoodLookupList(List<EncodedFood> choices, final SelectionHandler handler) {
		EncodedFoodCell cell = new EncodedFoodCell(); 
		
		List<EncodedFood> data = new ArrayList<EncodedFood>();
		
		CellList<EncodedFood> list = new CellList<EncodedFood>(cell, GWT.<R>create(R.class));
		list.setRowData(data);
		final SingleSelectionModel<EncodedFood> selectionModel = new SingleSelectionModel<EncodedFood>();
		selectionModel.addSelectionChangeHandler(new Handler() {
			@Override
			public void onSelectionChange(SelectionChangeEvent event) {
				handler.selectionChanged(selectionModel.getSelectedObject());				
			}
		});
		list.setSelectionModel(selectionModel);
		
		add(list);
	}
}
