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

package net.scran24.common.client;

import net.scran24.datastore.shared.UserInfo;

import org.workcraft.gwt.shared.client.Callback1;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

public class LoginForm extends Composite {
  final private static LoginServiceAsync loginService = LoginServiceAsync.Util.getInstance();
  final private static CommonMessages messages = CommonMessages.Util.getInstance();

  final private TextBox userText;
  final private PasswordTextBox passText;
  final private Callback1<UserInfo> onSuccess;
  final private FlowPanel errorMessage;
  private Button loginButton;

  private void doLogin(final String supportEmail) {
    loginService.login(EmbeddedData.surveyId(), userText.getText(), passText.getText(), new AsyncCallback<UserInfo>() {
      @Override
      public void onSuccess(UserInfo result) {
        onSuccess.call(result);
      }

      @Override
      public void onFailure(Throwable caught) {
        errorMessage.clear();
        if (caught instanceof CredentialsException)
          errorMessage.add(new HTMLPanel(SafeHtmlUtils.fromSafeConstant(messages.loginForm_passwordNotRecognised(supportEmail))));
        else
          errorMessage.add(new HTMLPanel(SafeHtmlUtils.fromSafeConstant(messages.loginForm_serviceException(supportEmail))));
        loginButton.setEnabled(true);
      }
    });

  }

  private void initLogin() {
    loginButton.setEnabled(false);

    errorMessage.clear();
    errorMessage.add(new LoadingWidget());

    loginService.getSurveySupportEmail(EmbeddedData.surveyId(), new AsyncCallback<String>() {
      @Override
      public void onFailure(Throwable caught) {
        // Use fallback address
        doLogin("support@intake24.co.uk");
      }

      @Override
      public void onSuccess(final String supportEmail) {
        doLogin(supportEmail);
      }
    });
  }

  public LoginForm(final Callback1<UserInfo> onSuccess, SafeHtml prompt) {
    this.onSuccess = onSuccess;
    Grid g = new Grid(2, 2);

    g.setCellPadding(5);
    Label userLabel = new Label(messages.loginForm_userNameLabel());
    Label passLabel = new Label(messages.loginForm_passwordLabel());

    this.userText = new TextBox();
    this.passText = new PasswordTextBox();

    g.setWidget(0, 0, userLabel);
    g.setWidget(1, 0, passLabel);
    g.setWidget(0, 1, userText);
    g.setWidget(1, 1, passText);

    VerticalPanel contents = new VerticalPanel();
    contents.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);

    FlowPanel linkPanel = new FlowPanel();

    linkPanel.add(WidgetFactory.createTutorialVideoLink());

    HTMLPanel pp = new HTMLPanel(prompt);
    contents.add(pp);
    HTMLPanel divider = new HTMLPanel(messages.loginForm_logInSeparator());
    divider.getElement().addClassName("intake24-login-form-divider");
    contents.add(divider);
    contents.add(linkPanel);
    contents.add(g);

    errorMessage = new FlowPanel();
    contents.add(errorMessage);

    loginButton = WidgetFactory.createButton(messages.loginForm_logInButtonLabel(), new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        initLogin();
      }
    });

    loginButton.getElement().setId("intake24-login-button");

    passText.addKeyPressHandler(new KeyPressHandler() {
      @Override
      public void onKeyPress(KeyPressEvent event) {
        if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ENTER)
          initLogin();
      }
    });

    userText.addKeyPressHandler(new KeyPressHandler() {
      @Override
      public void onKeyPress(KeyPressEvent event) {
        if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ENTER)
          initLogin();
      }
    });

    contents.add(WidgetFactory.createButtonsPanel(loginButton));
    contents.addStyleName("intake24-login-form");

    initWidget(contents);
  }

  public static void showPopup(final Callback1<UserInfo> onSuccess) {
    final OverlayDiv div = new OverlayDiv();

    LoginForm dialog = new LoginForm(new Callback1<UserInfo>() {
      @Override
      public void call(UserInfo info) {
        CurrentUser.setUserInfo(info);
        div.setVisible(false);
        onSuccess.call(info);
      }
    }, SafeHtmlUtils.fromSafeConstant(messages.loginForm_sessionExpired()));

    dialog.addStyleName("intake24-login-popup");

    div.setContents(dialog);
    div.setVisible(true);
  }

}