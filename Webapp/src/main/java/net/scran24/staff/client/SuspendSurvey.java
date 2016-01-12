/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.staff.client;

import net.scran24.common.client.WidgetFactory;
import net.scran24.datastore.shared.SurveyParameters;
import net.scran24.datastore.shared.SurveyState;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Window.Location;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.TextBox;

public class SuspendSurvey extends Composite {
	public SuspendSurvey(final SurveyParameters parameters, final SurveyControlServiceAsync surveyControl) {
		FlowPanel contents = new FlowPanel();
		
		HTMLPanel label = new HTMLPanel(SafeHtmlUtils.fromSafeConstant("<h3>Reason for suspension:</h3>"));
		final TextBox reason = new TextBox();
		Button suspend = WidgetFactory.createButton ("Suspend", new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				if (reason.getText().isEmpty()) {
					Window.alert ("Please give a reason for suspension.");					
				} else {
					surveyControl.setParameters(parameters.withSuspensionReason(reason.getText()).withState(SurveyState.SUSPENDED), new AsyncCallback<Void>() {
						@Override
						public void onSuccess(Void result) {
							Location.reload();							
						}
						
						@Override
						public void onFailure(Throwable caught) {
							Window.alert ("Server error: " + caught.getMessage());							
						}
					});					
				}
			}
		});
		
		suspend.getElement().addClassName("scran24-admin-button");
		
		contents.add(label);
		contents.add(reason);
		contents.add(suspend);
		
		initWidget(contents);
	}
}