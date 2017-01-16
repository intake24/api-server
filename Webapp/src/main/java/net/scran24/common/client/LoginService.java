/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.common.client;

import net.scran24.datastore.shared.UserInfo;
import org.workcraft.gwt.shared.client.Option;
import com.google.gwt.user.client.rpc.RemoteService;

public interface LoginService extends RemoteService {
  UserInfo login(String surveyId, String username, String password) throws LoginServiceException;

  String getSurveySupportEmail(String surveyId);

  Option<UserInfo> getUserInfo();
}
