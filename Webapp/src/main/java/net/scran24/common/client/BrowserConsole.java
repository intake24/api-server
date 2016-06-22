/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.common.client;

public class BrowserConsole {
	public static native void log(String msg) /*-{
		$wnd.console.log(msg);
	}-*/;

	public static native void error(String msg) /*-{
		$wnd.console.error(msg);
	}-*/;

	public static native void warn(String msg) /*-{
		$wnd.console.warn(msg);
	}-*/;
}
