/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client;

import org.workcraft.gwt.shared.client.Callback;

import net.scran24.common.client.CommonMessages;
import net.scran24.common.client.CurrentUser;
import net.scran24.common.client.GoogleAnalytics;
import net.scran24.common.client.LoadingWidget;
import net.scran24.common.client.OverlayDiv;
import net.scran24.common.client.WidgetFactory;
import net.scran24.user.client.services.HelpServiceAsync;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

public class CallbackRequestForm extends Composite {
  private final static CommonMessages messages = CommonMessages.Util.getInstance();
  private final static HelpServiceAsync helpService = HelpServiceAsync.Util.getInstance();

  final private TextBox nameTextBox;
  final private TextBox phoneNumberTextBox;

  final private FlowPanel errorMessage;
  final private Button requestCallbackButton;
  final private Button hideFormButton;

  private void doRequest() {
    requestCallbackButton.setEnabled(false);

    errorMessage.clear();

    if (nameTextBox.getText().isEmpty() || phoneNumberTextBox.getText().isEmpty()) {
      errorMessage.add(new HTMLPanel(SafeHtmlUtils.fromSafeConstant(messages.callbackRequestForm_fieldsEmpty())));
      errorMessage.getElement().addClassName("intake24-login-error-message");
      requestCallbackButton.setEnabled(true);
      return;
    }

    if (CurrentUser.userInfo.surveyId.equals("demo")) {
      errorMessage.add(new HTMLPanel(SafeHtmlUtils.fromSafeConstant(messages.callbackRequestForm_disabledForDemo("support@intake24.co.uk"))));
      errorMessage.getElement().addClassName("intake24-login-error-message");
      requestCallbackButton.setEnabled(true);
      return;
    }

    errorMessage.add(new LoadingWidget());

    helpService.requestCall(nameTextBox.getText(), phoneNumberTextBox.getText(), new AsyncCallback<Boolean>() {

      @Override
      public void onFailure(Throwable caught) {
        errorMessage.clear();
        errorMessage.add(new HTMLPanel(SafeHtmlUtils.fromSafeConstant(messages.serverError())));
        errorMessage.getElement().addClassName("intake24-login-error-message");
        requestCallbackButton.setEnabled(true);
      }

      @Override
      public void onSuccess(Boolean result) {
        if (result) {
          errorMessage.clear();
          errorMessage.getElement().addClassName("intake24-login-success-message");
          errorMessage.add(new HTMLPanel(SafeHtmlUtils.fromSafeConstant(messages.callbackRequestForm_success())));

          GoogleAnalytics.trackHelpCallbackAccepted();
        } else {
          errorMessage.clear();
          errorMessage.getElement().addClassName("intake24-login-error-message");
          errorMessage.add(new HTMLPanel(SafeHtmlUtils.fromSafeConstant(messages.callbackRequestForm_tooSoon())));

          GoogleAnalytics.trackHelpCallbackRejected();
        }
      }

    });
  }

  public CallbackRequestForm(final Callback onComplete) {
    Grid g = new Grid(2, 2);

    g.setCellPadding(5);
    Label nameLabel = new Label(messages.callbackRequestForm_nameLabel());
    Label phoneNumberLabel = new Label(messages.callbackRequestForm_phoneNumberLabel());

    this.nameTextBox = new TextBox();
    this.phoneNumberTextBox = new TextBox();

    g.setWidget(0, 0, nameLabel);
    g.setWidget(1, 0, phoneNumberLabel);
    g.setWidget(0, 1, nameTextBox);
    g.setWidget(1, 1, phoneNumberTextBox);

    VerticalPanel p = new VerticalPanel();
    p.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);

    FlowPanel videoLinkDiv = new FlowPanel();
    videoLinkDiv.add(WidgetFactory.createTutorialVideoLink());

    p.add(new HTMLPanel(messages.callbackRequestForm_watchWalkthrough()));
    p.add(videoLinkDiv);

    HTMLPanel pp = new HTMLPanel(SafeHtmlUtils.fromSafeConstant(messages.callbackRequestForm_promptText()));
    pp.getElement().addClassName("intake24-login-prompt-text");
    p.add(pp);
    p.add(g);

    errorMessage = new FlowPanel();
    p.add(errorMessage);

    requestCallbackButton = WidgetFactory.createButton(messages.callbackRequestForm_requestCallbackButtonLabel(), new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        doRequest();
      }
    });

    hideFormButton = WidgetFactory.createButton(messages.callbackRequestForm_hideButtonLabel(), new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        onComplete.call();
      }
    });

    requestCallbackButton.getElement().setId("intake24-login-button");

    nameTextBox.addKeyPressHandler(new KeyPressHandler() {
      @Override
      public void onKeyPress(KeyPressEvent event) {
        if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ENTER)
          doRequest();
      }
    });

    phoneNumberTextBox.addKeyPressHandler(new KeyPressHandler() {
      @Override
      public void onKeyPress(KeyPressEvent event) {
        if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ENTER)
          doRequest();
      }
    });

    p.add(WidgetFactory.createButtonsPanel(requestCallbackButton, hideFormButton));
    p.addStyleName("intake24-login-form");

    initWidget(p);
  }

  public static void showPopup() {
    final OverlayDiv div = new OverlayDiv();

    CallbackRequestForm dialog = new CallbackRequestForm(new Callback() {

      @Override
      public void call() {
        div.setVisible(false);
      }
    });

    dialog.addStyleName("intake24-login-popup");

    div.setContents(dialog);
    div.setVisible(true);
  }

}