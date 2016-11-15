/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.services;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("helpRequest")
public interface HelpService extends RemoteService {
	boolean requestCall(String name, String number);

	void reportUncaughtException(String strongName, List<String> classNames, List<String> messages, List<StackTraceElement[]> stackTraces, String surveyState);
}
