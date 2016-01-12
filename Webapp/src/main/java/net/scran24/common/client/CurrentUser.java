/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.common.client;

import net.scran24.datastore.shared.UserInfo;

public class CurrentUser {
	public static UserInfo userInfo;

	public static void setUserInfo(UserInfo info) {
		userInfo = info;
		setUserNameVar(info.userName);
	}

	//@Deprecated
	public static UserInfo getUserInfo() {
		return userInfo;
	}

	public static native void setUserNameVar(String username) /*-{
		$wnd.scran24_username = username;
	}-*/;
}
