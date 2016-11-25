/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.common.client;

import java.util.List;

import net.scran24.datastore.shared.UserInfo;

import org.workcraft.gwt.shared.client.Callback1;
import org.workcraft.gwt.shared.client.Option;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteEvent;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteHandler;
import com.google.gwt.user.client.ui.FormPanel.SubmitEvent;
import com.google.gwt.user.client.ui.FormPanel.SubmitHandler;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Hidden;
import com.google.gwt.user.client.ui.VerticalPanel;

public class UserInfoUpload extends Composite {

	public UserInfoUpload(final String surveyId, final String role, final List<String> permissions, final Callback1<Option<String>> onUploadComplete) {
		final FormPanel form = new FormPanel();
		form.setAction(GWT.getModuleBaseURL() + "../staff/uploadUserInfo?surveyId="+surveyId);

		form.setEncoding(FormPanel.ENCODING_MULTIPART);
		form.setMethod(FormPanel.METHOD_POST);

		VerticalPanel panel = new VerticalPanel();
		form.setWidget(panel);
		
		final Hidden roleField = new Hidden("role", role);
		panel.add(roleField);
		
		for (String perm: permissions) {
			final Hidden permField = new Hidden("permission", perm);
			panel.add(permField);
		}
		
		final FileUpload upload = new FileUpload();
		upload.setName("file");
		panel.add(upload);

		Button uploadButton = WidgetFactory.createButton("Upload", new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				form.submit();
			}
		});
		
		uploadButton.getElement().addClassName("scran24-admin-button");
		
		panel.add(uploadButton);

		form.addSubmitHandler(new SubmitHandler() {
			@Override
			public void onSubmit(SubmitEvent event) {
				if (upload.getFilename().isEmpty()) {
					Window.alert("Please choose a .csv file containing user information to upload");
					event.cancel();
				}
			}
		});

		form.addSubmitCompleteHandler(new SubmitCompleteHandler() {
			@Override
			public void onSubmitComplete(SubmitCompleteEvent event) {
				// This is not a very robust way of detecting the status code,
				// but unfortunately there does not seem to be a better way of
				// doing this that supports IE8
				// https://code.google.com/p/google-web-toolkit/issues/detail?id=7365

				String result = event.getResults();

				if (result.equals("OK")) {
					onUploadComplete.call(Option.<String>none());
				}
				else if (result.startsWith("ERR:")) {
					onUploadComplete.call(Option.some("There was a problem uploading user information: " + result.substring(4)));
				} else if (result.contains("401")) {
					LoginForm.showPopup(new Callback1<UserInfo>() {
						@Override
						public void call(UserInfo info) {
							form.submit();
						}
					});
				} else if (result.contains("403")) {
					// User is not authorised, e.g. someone has logged on as
					// admin, opened the user upload tab and timed out, then someone
					// else logged on as someone who does not have the right to
					// upload users. In this case, let them know and refresh the
					// page to either show the UI that corresponds to their set of
					// permissions or redirect them to another page.
					onUploadComplete.call(Option.some("You are not authorised to upload user information."));
				} else {
					onUploadComplete.call(Option.some("There was an problem uploading user information: " + result));
				}
			}
		});

		initWidget(form);
	}
}