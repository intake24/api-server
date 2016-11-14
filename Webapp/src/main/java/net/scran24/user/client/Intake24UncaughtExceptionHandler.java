package net.scran24.user.client;

import java.util.Arrays;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.GWT.UncaughtExceptionHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;

import net.scran24.common.client.CurrentUser;
import net.scran24.user.client.json.ErrorReport;
import net.scran24.user.client.json.ErrorReportCodec;
import net.scran24.user.client.services.HelpServiceAsync;
import net.scran24.user.client.survey.flat.StateManagerUtil;

import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;

public class Intake24UncaughtExceptionHandler implements UncaughtExceptionHandler {

  private final static HelpServiceAsync helpService = HelpServiceAsync.Util.getInstance();
  private final static ErrorReportCodec reportCodec = GWT.create(ErrorReportCodec.class);

  @Override
  public void onUncaughtException(final Throwable e) {
    String encodedSurveyState = "{}";

    if (CurrentUser.userInfo != null) {
      encodedSurveyState = StateManagerUtil.getLatestStateSerialised(CurrentUser.userInfo.userName)
        .getOrElse("{}");
    };
    
    String userName = "";
    String surveyId = "";
    
    if (CurrentUser.userInfo != null) {
      userName = CurrentUser.userInfo.userName;
      surveyId = CurrentUser.userInfo.surveyId;
    }

    JSONValue surveyStateJson = JSONParser.parseStrict(encodedSurveyState);

    ErrorReport report = new ErrorReport(userName, surveyId, e.getClass().getName(), e.getMessage(), Arrays.asList(e.getStackTrace()));
    
    JSONValue reportJson = reportCodec.encode(report);
    
    reportJson.isObject().put("surveyState", surveyStateJson);

    helpService.reportUncaughtException(e.getStackTrace(), new AsyncCallback<Void>() {

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
