package net.scran24.user.client;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.GWT.UncaughtExceptionHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;

import net.scran24.common.client.CurrentUser;
import net.scran24.user.client.services.HelpServiceAsync;
import net.scran24.user.client.survey.flat.StateManagerUtil;

public class Intake24UncaughtExceptionHandler implements UncaughtExceptionHandler {

  private final static HelpServiceAsync helpService = HelpServiceAsync.Util.getInstance();

  @Override
  public void onUncaughtException(final Throwable e) {
    String encodedSurveyState = "{}";

    if (CurrentUser.userInfo != null) {
      encodedSurveyState = StateManagerUtil.getLatestStateSerialised(CurrentUser.userInfo.userName)
        .getOrElse("{}");
    }

    Throwable cur = e;

    List<String> classNames = new ArrayList<String>();
    List<String> messages = new ArrayList<String>();
    List<StackTraceElement[]> stackTraces = new ArrayList<StackTraceElement[]>();

    while (cur != null) {
      classNames.add(cur.getClass()
        .getName());
      messages.add(cur.getMessage());
      stackTraces.add(cur.getStackTrace());
      cur = cur.getCause();
    }

    helpService.reportUncaughtException(GWT.getPermutationStrongName(), classNames, messages, stackTraces,
        encodedSurveyState, new AsyncCallback<Void>() {

          @Override
          public void onSuccess(Void result) {
            GWT.log("Reported uncaught exception to the server:");
            GWT.log("Uncaught exception", e);
          }

          @Override
          public void onFailure(Throwable caught) {
            GWT.log("Failed to reported uncaught exception to the server:");
            GWT.log("Uncaught exception", e);
          }
        });
  }
}
