/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.staff.client;

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

public class DownloadData extends Composite {
	final DatePicker dateFrom;
	final DatePicker dateTo;

	public DownloadData() {
		final FlowPanel contents = new FlowPanel();
		final FlowPanel surveyDump = new FlowPanel();

		HorizontalPanel hpanel = new HorizontalPanel();

		hpanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_TOP);
		hpanel.setSpacing(10);

		dateFrom = new DatePicker();
		dateTo = new DatePicker();

		hpanel.add(new Label("From:"));
		hpanel.add(dateFrom);
		hpanel.add(new Label("To:"));
		hpanel.add(dateTo);

		// final Button showSurveys = new Button("Show surveys");

		// showSurveys.setWidth("150px");

		final long day = 24 * 60 * 60 * 1000;

	

		final Button downloadSurveys = WidgetFactory.createButton("Download survey data", new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				long timeFrom = dateFrom.getValue().getTime();
				long timeTo = dateTo.getValue().getTime() + day;

				String url = GWT.getModuleBaseURL() + "../staff/downloadData?timeFrom=" + Long.toString(timeFrom) + "&timeTo="
						+ Long.toString(timeTo) + "&surveyId=" + EmbeddedData.surveyId();
				Window.open(url, "_blank", "status=0,toolbar=0,menubar=0,location=0");
			}
		});
		
		downloadSurveys.getElement().addClassName("scran24-admin-button");

		final Button downloadActivity = WidgetFactory.createButton("Download activity report", new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				long timeFrom = dateFrom.getValue().getTime();
				long timeTo = dateTo.getValue().getTime() + day;

				String url = GWT.getModuleBaseURL() + "../staff/downloadActivity?timeFrom=" + Long.toString(timeFrom) + "&timeTo="
						+ Long.toString(timeTo) + "&surveyId=" + EmbeddedData.surveyId();
				Window.open(url, "_blank", "status=0,toolbar=0,menubar=0,location=0");
			}
		});
		
		downloadActivity.getElement().addClassName("scran24-admin-button");

		// showSurveys.setEnabled(false);
		downloadSurveys.setEnabled(false);
		downloadActivity.setEnabled(false);

		dateFrom.addValueChangeHandler(new ValueChangeHandler<Date>() {
			@Override
			public void onValueChange(ValueChangeEvent<Date> event) {
				if (event.getValue() != null && dateTo.getValue() != null) {
					// showSurveys.setEnabled(true);
					downloadSurveys.setEnabled(false);
					downloadActivity.setEnabled(false);
				}

			}
		});

		dateTo.addValueChangeHandler(new ValueChangeHandler<Date>() {
			@Override
			public void onValueChange(ValueChangeEvent<Date> event) {
				if (event.getValue() != null && dateFrom.getValue() != null) {
					// showSurveys.setEnabled(true);
					downloadSurveys.setEnabled(true);
					downloadActivity.setEnabled(true);
				}
			}
		});

		// buttons.add(showSurveys);
		// buttons.add(download);

		/*FlowPanel p1 = new FlowPanel();
		p1.add(downloadSurveys);
		
		FlowPanel p2 = new FlowPanel();
		p2.add(downloadActivity);*/
		
		contents.add(hpanel);
		contents.add(WidgetFactory.createButtonsPanel(downloadSurveys, downloadActivity));
		contents.add(surveyDump);

		initWidget(contents);
	}
}
