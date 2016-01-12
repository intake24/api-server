/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.common.client;

public class GoogleAnalytics {

	public native static void trackHelpButtonClicked(String promptType) /*-{
		if ("ga" in $wnd) {
			$wnd.ga("send", "event", "help", "Help button clicked", promptType);
		}
	}-*/;

	public native static void trackHelpCallbackRequested(String promptType) /*-{
		if ("ga" in $wnd) {
			$wnd.ga("send", "event", "help", "Call back requested", promptType);
		}
	}-*/;

	public native static void trackHelpCallbackAccepted() /*-{
		if ("ga" in $wnd) {
			$wnd.ga("send", "event", "help", "Call back request accepted");
		}
	}-*/;

	public native static void trackHelpCallbackRejected() /*-{
		if ("ga" in $wnd) {
			$wnd.ga("send", "event", "help", "Call back request rejected");
		}
	}-*/;

	public native static void trackMissingFoodHomeRecipe() /*-{
		if ("ga" in $wnd) {
			$wnd.ga("send", "event", "missingFood", "Home recipe option clicked");
		}
	}-*/;

	public native static void trackMissingFoodNotHomeRecipe() /*-{
		if ("ga" in $wnd) {
			$wnd.ga("send", "event", "missingFood", "Not a home recipe option clicked");
		}
	}-*/;
	
	public native static void trackMissingFoodReported() /*-{
		if ("ga" in $wnd) {
			$wnd.ga("send", "event", "missingFood", "Report missing food clicked");
		}		
	}-*/;
}
