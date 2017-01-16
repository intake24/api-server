/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.admin.client;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.scran24.admin.client.services.SurveyManagementServiceAsync;
import net.scran24.common.client.LoadingPanel;
import net.scran24.common.client.UserInfoUpload;

import org.workcraft.gwt.shared.client.Callback1;
import org.workcraft.gwt.shared.client.Option;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;

public class SurveyManagement extends Composite {
  final private SurveyManagementServiceAsync service = SurveyManagementServiceAsync.Util.getInstance();

  private FlowPanel surveyControls;

  private void setControlsFor(String id) {
    surveyControls.clear();

    final HTMLPanel idLabel = new HTMLPanel("<h2>" + SafeHtmlUtils.htmlEscape(id) + "</h2>");
    final HTMLPanel staffUploadLabel = new HTMLPanel("<h3>Upload staff accounts</h3>");
    final FlowPanel messageDiv = new FlowPanel();
    messageDiv.getElement().addClassName("scran24-admin-survey-id-message");

    List<String> permissions = Arrays.asList(new String[] {
      "downloadData:" + id,
      "readSurveys:" + id,
      "uploadUserInfo:" + id,
      "surveyControl:" + id
    });

    final UserInfoUpload staffUpload = new UserInfoUpload(id, "staff", permissions, new Callback1<Option<String>>() {
      @Override
      public void call(Option<String> res) {
        res.accept(new Option.SideEffectVisitor<String>() {
          @Override
          public void visitSome(String error) {
            messageDiv.clear();
            messageDiv.getElement().getStyle().setColor("#d00");
            messageDiv.add(new HTMLPanel(SafeHtmlUtils.fromString(error)));
          }

          @Override
          public void visitNone() {
            messageDiv.clear();
            messageDiv.getElement().getStyle().setColor("#0d0");
            messageDiv.add(new HTMLPanel(SafeHtmlUtils.fromString("Staff accounts uploaded successfully")));
          }
        });
      }
    });

    surveyControls.add(idLabel);
    surveyControls.add(staffUploadLabel);
    surveyControls.add(messageDiv);
    surveyControls.add(staffUpload);

  }

  public SurveyManagement() {
    final FlowPanel contents = new FlowPanel();

    contents.add(new LoadingPanel("Loading surveys..."));

    service.listSurveys(new AsyncCallback<List<String>>() {
      @Override
      public void onFailure(Throwable arg0) {
        contents.clear();
        contents.add(new Label("Server error - please check server logs"));
      }

      @Override
      public void onSuccess(List<String> surveys) {
        contents.clear();

        if (surveys.isEmpty()) {
          contents.add(new HTMLPanel("<h2>No surveys to manage.</h2>"));
        } else {

          Collections.sort(surveys);

          FlowPanel surveySelector = new FlowPanel();

          final ListBox surveyIdList = new ListBox(false);

          for (String id : surveys)
            surveyIdList.addItem(id);

          surveySelector.add(new Label("Select survey: "));
          surveySelector.add(surveyIdList);

          contents.add(surveySelector);

          surveyControls = new FlowPanel();

          contents.add(surveyControls);

          surveyIdList.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent arg0) {
              setControlsFor(surveyIdList.getItemText(surveyIdList.getSelectedIndex()));
            }
          });

          surveyIdList.setSelectedIndex(0);
          setControlsFor(surveyIdList.getItemText(surveyIdList.getSelectedIndex()));
        }
      }
    });

    initWidget(contents);
  }
}