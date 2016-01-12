/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.admin.client;

import java.util.Date;

import net.scran24.common.client.EmbeddedData;
import net.scran24.common.client.WidgetFactory;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.datepicker.client.DatePicker;

public class DownloadMissingFoods extends Composite {
	final DatePicker dateFrom;
	final DatePicker dateTo;

	public DownloadMissingFoods() {
		final FlowPanel contents = new FlowPanel();

		HorizontalPanel hpanel = new HorizontalPanel();

		hpanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_TOP);
		hpanel.setSpacing(10);

		dateFrom = new DatePicker();
		dateTo = new DatePicker();

		hpanel.add(new Label("From:"));
		hpanel.add(dateFrom);
		hpanel.add(new Label("To:"));
		hpanel.add(dateTo);

		final long day = 24 * 60 * 60 * 1000;
		
		final Button downloadMissingFoods = WidgetFactory.createButton("Download missing foods report", new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				long timeFrom = dateFrom.getValue().getTime();
				long timeTo = dateTo.getValue().getTime() + day;

				String url = GWT.getModuleBaseURL() + "../admin/downloadMissingFoods?timeFrom=" + Long.toString(timeFrom) + "&timeTo="
						+ Long.toString(timeTo);
				Window.open(url, "_blank", "status=0,toolbar=0,menubar=0,location=0");
			}
		});
		
		downloadMissingFoods.getElement().addClassName("scran24-admin-button");
	
		downloadMissingFoods.setEnabled(false);

		dateFrom.addValueChangeHandler(new ValueChangeHandler<Date>() {
			@Override
			public void onValueChange(ValueChangeEvent<Date> event) {
				if (event.getValue() != null && dateTo.getValue() != null) {
					downloadMissingFoods.setEnabled(false);
				}

			}
		});

		dateTo.addValueChangeHandler(new ValueChangeHandler<Date>() {
			@Override
			public void onValueChange(ValueChangeEvent<Date> event) {
				if (event.getValue() != null && dateFrom.getValue() != null) {
					downloadMissingFoods.setEnabled(true);
				}
			}
		});
		
		contents.add(hpanel);
		contents.add(WidgetFactory.createButtonsPanel(downloadMissingFoods));

		initWidget(contents);
	}
}
