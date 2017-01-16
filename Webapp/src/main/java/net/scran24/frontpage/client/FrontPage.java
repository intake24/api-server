/*
This file is part of Intake24.

Copyright 2015, 2016, 2017 Newcastle University.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

This file is based on Intake24 v1.0.

© Crown copyright, 2012, 2013, 2014

Licensed under the Open Government Licence 3.0: 

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.frontpage.client;

import net.scran24.common.client.CommonMessages;
import net.scran24.common.client.EmbeddedData;
import net.scran24.common.client.LoginForm;
import net.scran24.common.client.LoginServiceAsync;
import net.scran24.common.client.WidgetFactory;
import net.scran24.datastore.shared.UserInfo;
import net.scran24.datastore.shared.UserRecord;
import net.scran24.frontpage.client.services.GenUserServiceAsync;

import org.workcraft.gwt.shared.client.Callback1;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.Window.Location;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;

public class FrontPage implements EntryPoint {
  private GenUserServiceAsync genUserService = GenUserServiceAsync.Util.getInstance();
  private LoginServiceAsync loginService = LoginServiceAsync.Util.getInstance();
  private CommonMessages commonMessages = CommonMessages.Util.getInstance();

  private native void initComplete() /*-{
		if (typeof $wnd.intake24_initComplete == 'function')
			$wnd.intake24_initComplete();
  }-*/;

  private void replaceHeader() {
    // A hack to support old generated login HTML files

    RootPanel headerPanel = RootPanel.get("login-header");
    headerPanel.getElement().removeAllChildren();
    headerPanel.add(new HTMLPanel("h1", commonMessages.loginForm_welcome()));
  }

  public void onModuleLoad() {
    if (Location.getParameter("genUser") != null) {
      genUserService.autoCreateUser(EmbeddedData.surveyId(), new AsyncCallback<UserRecord>() {
        @Override
        public void onSuccess(final UserRecord userRecord) {
          loginService.login(EmbeddedData.surveyId(), userRecord.username, userRecord.password, new AsyncCallback<UserInfo>() {
            @Override
            public void onSuccess(UserInfo arg0) {
              RootPanel.get("loading").getElement().removeFromParent();

              final String url = Location.createUrlBuilder().removeParameter("genUser").buildString();

              FlowPanel userInfo = new FlowPanel();
              userInfo.addStyleName("intake24-user-info-panel");

              userInfo.add(new HTMLPanel(SafeHtmlUtils.fromSafeConstant(commonMessages.genUserWelcome())));
              userInfo.add(new HTMLPanel(SafeHtmlUtils.fromSafeConstant(commonMessages.genUserSaveInfo())));

              String surveyUrl = Location.getProtocol() + "//" + Location.getHost() + Location.getPath().replace("/login/", "/");

              Grid userInfoTable = new Grid(2, 2);
              userInfoTable.addStyleName("intake24-user-info-table");

              userInfoTable.setWidget(0, 0, new Label(commonMessages.loginForm_userNameLabel()));
              userInfoTable.setWidget(0, 1, new Label(userRecord.username));
              userInfoTable.setWidget(1, 0, new Label(commonMessages.loginForm_passwordLabel()));
              userInfoTable.setWidget(1, 1, new Label(userRecord.password));

              userInfo.add(userInfoTable);

              userInfo.add(new HTMLPanel(SafeHtmlUtils.fromSafeConstant(commonMessages.genUserSurveyLink())));

              FlowPanel urlDiv = new FlowPanel();
              urlDiv.add(new Anchor(surveyUrl, surveyUrl));
              userInfo.add(urlDiv);

              userInfo.add(new HTMLPanel(SafeHtmlUtils.fromSafeConstant(commonMessages.genUserOneSitting())));

              Button cont = WidgetFactory.createGreenButton(commonMessages.genUserContinue(), new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                  Location.replace(url);
                }
              });

              userInfo.add(WidgetFactory.createButtonsPanel(cont));

              replaceHeader();

              RootPanel.get("loginForm").add(userInfo);
            }

            @Override
            public void onFailure(Throwable arg0) {
              RootPanel.get("loading").getElement().removeFromParent();

              FlowPanel errorPanel = new FlowPanel();
              errorPanel.addStyleName("intake24-error-panel");

              errorPanel.add(new HTMLPanel(SafeHtmlUtils.fromSafeConstant(commonMessages.serverError())));

              RootPanel.get("loginForm").add(errorPanel);
            }
          });
        }

        @Override
        public void onFailure(Throwable arg0) {
          RootPanel.get("loading").getElement().removeFromParent();

          FlowPanel errorPanel = new FlowPanel();
          errorPanel.addStyleName("intake24-error-panel");

          errorPanel.add(new HTMLPanel(SafeHtmlUtils.fromSafeConstant(commonMessages.serverError())));

          RootPanel.get("loginForm").add(errorPanel);
        }
      });
    } else {
      RootPanel.get("loading").getElement().removeFromParent();

      LoginForm login = new LoginForm(new Callback1<UserInfo>() {
        @Override
        public void call(UserInfo info) {
          Location.reload();
        }
      }, SafeHtmlUtils.fromSafeConstant(commonMessages.loginForm_logInToContinue()));

      replaceHeader();
      RootPanel.get("loginForm").add(login);
    }

    initComplete();
  }
}