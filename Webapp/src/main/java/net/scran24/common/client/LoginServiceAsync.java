/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.common.client;

import net.scran24.datastore.shared.UserInfo;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;

public interface LoginServiceAsync {
  /**
   * Requests the details of the currently logged-in user. The user must be
   * logged in before calling this.
   */
  void getUserInfo(AsyncCallback<org.workcraft.gwt.shared.client.Option<UserInfo>> callback);

  /**
   * Logs a user in.
   * 
   * @param surveyId
   *          id of the survey, or special id "admin" for administration users
   */
  void login(String surveyId, String username, String password, AsyncCallback<UserInfo> callback);

  public static final class Util {
    private static LoginServiceAsync instance;

    public static final LoginServiceAsync getInstance() {
      if (instance == null) {
        instance = (LoginServiceAsync) GWT.create(LoginService.class);
        ServiceDefTarget target = (ServiceDefTarget) instance;
        target.setServiceEntryPoint(GWT.getModuleBaseURL() + "../common/login");
      }
      return instance;
    }

    private Util() {
    }
  }

  void getSurveySupportEmail(String surveyId, AsyncCallback<String> callback);
}