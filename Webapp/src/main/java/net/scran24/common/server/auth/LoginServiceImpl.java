/*
This file is part of Intake24.

Copyright 2015, 2016 Newcastle University.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

This file is based on Intake24 v1.0.

© Crown copyright, 2012, 2013, 2014

Licensed under the Open Government Licence 3.0: 

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.common.server.auth;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;

import net.scran24.common.client.CredentialsException;
import net.scran24.common.client.LoginService;
import net.scran24.common.client.LoginServiceException;
import net.scran24.common.client.UnexpectedErrorException;
import net.scran24.datastore.DataStore;
import net.scran24.datastore.DataStoreException;
import net.scran24.datastore.shared.UserInfo;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.workcraft.gwt.shared.client.Option;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.google.inject.Injector;

public class LoginServiceImpl extends RemoteServiceServlet implements LoginService {
  private static final long serialVersionUID = 858441628260257598L;

  private DataStore dataStore;

  private final Logger log = LoggerFactory.getLogger(LoginServiceImpl.class);

  private UserInfo mkUserInfo(Subject subject) throws DataStoreException {
    ScranUserId userId = (ScranUserId) subject.getPrincipal();

    if (userId.survey.equals("admin"))
      // Not too elegant, but there is no reasonable survey state that can be
      // substituted
      return new UserInfo(userId.username, userId.survey, null, dataStore.getUserData(userId.survey, userId.username));
    else
      return new UserInfo(userId.username, userId.survey, dataStore.getSurveyParameters(userId.survey),
          dataStore.getUserData(userId.survey, userId.username));
  }

  @Override
  public void init() throws ServletException {
    Injector injector = (Injector) getServletContext().getAttribute("intake24.injector");
    dataStore = injector.getInstance(DataStore.class);
  }

  private String hidePassword(String password) {
    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < password.length(); i++) {
      if (i == 0 || i == (password.length() - 1))
        sb.append(password.charAt(i));
      else
        sb.append("*");
    }

    return sb.toString();
  }

  private String removeWhitespace(String s) {
    return s.replaceAll("\\s+", "");
  }

  private UserInfo loginWithOneOf(List<ScranAuthToken> tokens) throws LoginServiceException {
    String user_id = tokens.get(0).survey + "/" + tokens.get(0).username;

    for (ScranAuthToken token : tokens) {
      try {
        Subject subject = SecurityUtils.getSubject();

        subject.login(token);

        log.info("User " + token.getUsername() + " logged in");

        return mkUserInfo(subject);
      } catch (DataStoreAuthenticationException e) {
        log.error("User " + token.getUsername() + " log in attempt failed due to data store error", e);
        e.printStackTrace();
        throw new UnexpectedErrorException();
      } catch (DataStoreException e) {
        log.error("User " + token.getUsername() + " log in attempt failed due to data store error", e);
        e.printStackTrace();
        throw new UnexpectedErrorException();
      } catch (AuthenticationException e) {
        continue;
      }
    }

    log.info(
        "User " + user_id + " log in attempt failed due to invalid credentials (supplied password: " + hidePassword(tokens.get(0).password) + ")");
    throw new CredentialsException();
  }

  public String capitaliseFirstLetter(String s) {
    if (s.isEmpty())
      return s;

    return Character.toUpperCase(s.charAt(0)) + s.substring(1);
  }

  public static String invertCase(String s) {
    char[] chars = s.toCharArray();
    for (int i = 0; i < chars.length; i++) {
      char c = chars[i];
      if (Character.isUpperCase(c)) {
        chars[i] = Character.toLowerCase(c);
      } else if (Character.isLowerCase(c)) {
        chars[i] = Character.toUpperCase(c);
      } else {
        chars[i] = c;
      }
    }
    return new String(chars);
  }

  public List<ScranAuthToken> generateAcceptedPasswordVariations(String realm, String username, String password) {
    List<ScranAuthToken> result = new ArrayList<ScranAuthToken>();

    String filteredUsername = removeWhitespace(username);
    String filteredPassword = removeWhitespace(password);

    result.add(new ScranAuthToken(realm, filteredUsername, filteredPassword));
    result.add(new ScranAuthToken(realm, filteredUsername, invertCase(filteredPassword)));
    result.add(new ScranAuthToken(realm, filteredUsername, capitaliseFirstLetter(filteredPassword)));

    return result;
  }

  @Override
  public UserInfo login(String realm, String username, String password) throws LoginServiceException {
    return loginWithOneOf(generateAcceptedPasswordVariations(realm, username, password));
  }

  @Override
  public Option<UserInfo> getUserInfo() {
    Subject subject = SecurityUtils.getSubject();

    if (subject.isAuthenticated())
      try {
        return Option.some(mkUserInfo(subject));
      } catch (DataStoreException e) {
        throw new RuntimeException(e);
      }
    else
      return Option.none();
  }

  @Override
  public String getSurveySupportEmail(String surveyId) {
    try {
      return dataStore.getSurveySupportEmail(surveyId);
    } catch (DataStoreException e) {
      throw new RuntimeException(e);
    }
  }
}