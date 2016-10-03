package net.scran24.user.client;

import java.util.ArrayList;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.GWT.UncaughtExceptionHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;

import net.scran24.common.client.CurrentUser;
import net.scran24.user.client.services.HelpServiceAsync;

public class Intake24UncaughtExceptionHandler implements UncaughtExceptionHandler {

	private final static HelpServiceAsync helpService = HelpServiceAsync.Util.getInstance();

	@Override
	public void onUncaughtException(final Throwable e) {
		ArrayList<String> stackTrace = new ArrayList<String>();

		for (StackTraceElement ste : e.getStackTrace()) {
			stackTrace.add(ste.toString());
		}

		helpService.reportUncaughtException(CurrentUser.userInfo == null ? "undefined" : CurrentUser.userInfo.surveyId,
				CurrentUser.userInfo == null ? "undefined" : CurrentUser.userInfo.userName, e.getClass().getName(), e.getMessage(), stackTrace,
				new AsyncCallback<Void>() {

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
