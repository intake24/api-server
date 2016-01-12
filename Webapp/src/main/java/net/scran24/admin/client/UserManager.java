/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.admin.client;

import net.scran24.common.client.LoginForm;
import net.scran24.common.client.WidgetFactory;
import net.scran24.datastore.shared.UserInfo;

import org.workcraft.gwt.shared.client.Callback1;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Window.Location;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteEvent;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteHandler;
import com.google.gwt.user.client.ui.FormPanel.SubmitEvent;
import com.google.gwt.user.client.ui.FormPanel.SubmitHandler;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.VerticalPanel;

public class UserManager extends Composite {

	public UserManager() {
		FlowPanel contents = new FlowPanel();

		final FormPanel form = new FormPanel();
		form.setAction(GWT.getModuleBaseURL() + "uploadUserInfo");

		form.setEncoding(FormPanel.ENCODING_MULTIPART);
		form.setMethod(FormPanel.METHOD_POST);

		VerticalPanel panel = new VerticalPanel();
		form.setWidget(panel);

		final FileUpload upload = new FileUpload();
		upload.setName("file");
		panel.add(upload);

		RadioButton append = new RadioButton("mode", "Append to existing user list");
		append.setFormValue("append");

		final RadioButton replace = new RadioButton("mode", "Replace existing user list");
		replace.setFormValue("replace");

		replace.setValue(true);

		panel.add(append);
		panel.add(replace);

		panel.add(WidgetFactory.createButton("Upload", new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				form.submit();
			}
		}));

		form.addSubmitHandler(new SubmitHandler() {
			@Override
			public void onSubmit(SubmitEvent event) {
				if (upload.getFilename().isEmpty()) {
					Window.alert("Please choose a .csv file containing user information to upload");
					event.cancel();
				} else if (replace.getValue())
					if (!Window
							.confirm("Doing this will delete all user information from the database and replace it with the list you are submitting. Proceed?"))
						event.cancel();
			}
		});

		form.addSubmitCompleteHandler(new SubmitCompleteHandler() {
			@Override
			public void onSubmitComplete(SubmitCompleteEvent event) {
				// This is not a very robust way of detecting the status code,
				// but unfortunately there does not seem to be a better way of doing
				// this
				// that supports IE8 -- Ivan.
				// https://code.google.com/p/google-web-toolkit/issues/detail?id=7365

				String result = event.getResults();

				if (result.equals("OK"))
					Window.alert("User information uploaded.");
				else if (result.startsWith("ERR:")) {
					Window.alert("There was a problem uploading the user information: " + result.substring(4));
				} else if (result.contains("401")) {
					LoginForm.showPopup(new Callback1<UserInfo>() {
						@Override
						public void call(UserInfo info) {
							form.submit();
						}
					});
				} else if (result.contains("403")) {
					// User is not authorised, e.g. someone has logged on as admin,
					// opened the user upload tab and timed out, then someone else
					// logged on as someone who does not have the right to
					// upload users. In this case, let them know and refresh the page
					// to either show the UI that corresponds to their set of permissions
					// or redirect them to another page.

					Window.alert("You are not authorised to upload user information.");
					Location.reload();
				}
			}
		});
		contents.add(new HTMLPanel("<h2>Staff user accounts</h2>"));
		contents.add(new Button("Add"));

		contents.add(new HTMLPanel("<h2>Respondent user accounts</h2>"));
		contents.add(form);

		initWidget(contents);
	}
}