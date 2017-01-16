/*
This file is part of Intake24.

Copyright 2015, 2016, 2017 Newcastle University.

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

package net.scran24.frontpage.server.services;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletException;

import net.scran24.datastore.DataStore;
import net.scran24.datastore.DataStoreException;
import net.scran24.datastore.DuplicateKeyException;
import net.scran24.datastore.SecureUserRecord;
import net.scran24.datastore.shared.SurveyParameters;
import net.scran24.datastore.shared.UserRecord;
import net.scran24.frontpage.client.services.GenUserService;

import org.apache.shiro.crypto.RandomNumberGenerator;
import org.apache.shiro.crypto.SecureRandomNumberGenerator;
import org.apache.shiro.crypto.hash.Sha256Hash;
import org.apache.shiro.util.ByteSource;
import org.workcraft.gwt.shared.client.Option;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.google.inject.Injector;

public class GenUserServiceImpl extends RemoteServiceServlet implements GenUserService {
  private static final long serialVersionUID = -5525469181691523598L;

  private DataStore dataStore;
  private RandomNumberGenerator rng = new SecureRandomNumberGenerator();
  private final String passwordChars = "abcdefghijklmnopqrstuvwxyz0123456789";
  private final int passwordLength = 5;

  @Override
  public void init() throws ServletException {
    Injector injector = (Injector) this.getServletContext().getAttribute("intake24.injector");
    dataStore = injector.getInstance(DataStore.class);
  }

  @Override
  public UserRecord autoCreateUser(final String survey_name) {
    if (survey_name.isEmpty())
      throw new RuntimeException("This feature is not applicable to system-wide users");

    try {
      SurveyParameters parameters = dataStore.getSurveyParameters(survey_name);

      if (!parameters.allowGenUsers)
        throw new RuntimeException("Automatically generated user records are not allowed for this survey");

      final String counterName = survey_name + "_gen_user_counter";

      int counter = Integer.parseInt(dataStore.getGlobalValue(counterName).getOrElse("0"));

      StringBuilder psb = new StringBuilder();

      ByteSource bytes = rng.nextBytes(passwordLength);

      for (int i = 0; i < passwordLength; i++) {
        int index = ((int) (bytes.getBytes()[i]) + 128) % passwordChars.length();
        psb.append(passwordChars.charAt(index));
      }

      ByteSource salt = rng.nextBytes();

      String password = psb.toString();

      String passwordHashBase64 = new Sha256Hash(password, salt, 1024).toBase64();
      String passwordSaltBase64 = salt.toBase64();

      Set<String> roles = new HashSet<String>();
      roles.add("respondent");

      Set<String> permissions = new HashSet<String>();
      permissions.add("processSurvey:" + survey_name);

      int retries = 20;
      boolean addUserOk = false;
      String username = "";

      while (retries > 0) {
        counter++;
        username = survey_name + counter;

        try {
          dataStore.addUser(survey_name, new SecureUserRecord(username, passwordHashBase64, passwordSaltBase64, Option.<String>none(),
              Option.<String>none(), Option.<String>none(), roles, permissions, new HashMap<String, String>()));
          addUserOk = true;
          break;
        } catch (DataStoreException | DuplicateKeyException e) {
          continue;
        }
      }

      if (!addUserOk)
        throw new RuntimeException("Could not find a unique user name in 20 attempts");

      dataStore.setGlobalValue(counterName, Integer.toString(counter));

      return new UserRecord(username, password, Option.<String>none(), Option.<String>none(), Option.<String>none(), new HashMap<String, String>());

    } catch (NumberFormatException e) {
      throw new RuntimeException(e);
    } catch (DataStoreException e) {
      throw new RuntimeException(e);
    }
  }
}