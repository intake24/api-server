/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.services;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

public interface HelpServiceAsync {
	void requestCall(String name, String phoneNumber, AsyncCallback<Boolean> callback);
	
	void reportUncaughtException(String strongName, List<String> classNames, List<String> messages, List<StackTraceElement[]> stackTraces, String surveyState, AsyncCallback<Void> callback);

	public static final class Util {
		private static HelpServiceAsync instance;

		public static final HelpServiceAsync getInstance() {
			if (instance == null)
				instance = (HelpServiceAsync) GWT.create(HelpService.class);
			
			return instance;
		}

		private Util() {
		}
	}
}
