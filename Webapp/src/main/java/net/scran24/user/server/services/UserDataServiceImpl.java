/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.server.services;

import java.util.Map;

import javax.servlet.ServletException;

import net.scran24.common.server.auth.ScranUserId;
import net.scran24.datastore.DataStore;
import net.scran24.datastore.DataStoreException;
import net.scran24.user.client.services.UserDataService;

import org.apache.shiro.SecurityUtils;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public class UserDataServiceImpl extends RemoteServiceServlet implements UserDataService {
  private static final long serialVersionUID = -5525469181691523598L;

  private DataStore dataStore;

  @Override
  public void init() throws ServletException {
    dataStore = (DataStore) this.getServletContext()
      .getAttribute("scran24.datastore");
  }

  public void submit(Map<String, String> data) {
    ScranUserId user_id = (ScranUserId) SecurityUtils.getSubject()
      .getPrincipal();

    try {
      dataStore.setUserData(user_id.survey, user_id.username, data);
    } catch (DataStoreException e) {
      throw new RuntimeException(e);
    }
  }
}