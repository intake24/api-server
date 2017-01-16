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

package net.scran24.staff.server.services;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.scran24.datastore.DataStore;
import net.scran24.datastore.DataStoreException;
import net.scran24.datastore.DuplicateKeyException;
import net.scran24.datastore.SecureUserRecord;
import net.scran24.datastore.UserRecordCSV;
import net.scran24.datastore.shared.UserRecord;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.shiro.crypto.RandomNumberGenerator;
import org.apache.shiro.crypto.SecureRandomNumberGenerator;
import org.apache.shiro.crypto.hash.Sha256Hash;
import org.apache.shiro.util.ByteSource;

import com.google.inject.Injector;

public class UploadUserInfoService extends HttpServlet {
  private static final long serialVersionUID = 6859630591301166393L;
  private DataStore dataStore;

  @Override
  public void init() throws ServletException {
    Injector injector = (Injector) this.getServletContext().getAttribute("intake24.injector");
    dataStore = injector.getInstance(DataStore.class);
  }

  private List<SecureUserRecord> mapToSecureUserRecords(List<UserRecord> userRecords, Set<String> roles, Set<String> permissions) {
    List<SecureUserRecord> result = new ArrayList<SecureUserRecord>();

    RandomNumberGenerator rng = new SecureRandomNumberGenerator();

    for (UserRecord r : userRecords) {
      ByteSource salt = rng.nextBytes();

      String passwordHashBase64 = new Sha256Hash(r.password, salt, 1024).toBase64();
      String passwordSaltBase64 = salt.toBase64();

      result
        .add(new SecureUserRecord(r.username, passwordHashBase64, passwordSaltBase64, r.name, r.email, r.phone, roles, permissions, r.customFields));
    }

    return result;
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    resp.setContentType("text/html");
    ServletOutputStream outputStream = resp.getOutputStream();
    PrintWriter writer = new PrintWriter(outputStream);

    if (!ServletFileUpload.isMultipartContent(req)) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
    } else {
      DiskFileItemFactory factory = new DiskFileItemFactory();
      ServletContext servletContext = this.getServletConfig().getServletContext();
      File repository = (File) servletContext.getAttribute("javax.servlet.context.tempdir");
      factory.setRepository(repository);

      ServletFileUpload upload = new ServletFileUpload(factory);

      try {
        List<FileItem> items = upload.parseRequest(req);

        InputStream file = null;
        String role = null;
        Set<String> permissions = new HashSet<String>();
        String surveyId = req.getParameter("surveyId");

        for (FileItem i : items) {
          if (i.getFieldName().equals("file"))
            file = i.getInputStream();
          else if (i.getFieldName().equals("role"))
            role = i.getString();
          else if (i.getFieldName().equals("permission"))
            permissions.add(i.getString());
        }

        if (file == null)
          throw new ServletException("file field not specified");
        if (role == null)
          throw new ServletException("role field not specified");
        if (surveyId == null)
          throw new ServletException("surveyId field not specified");

        List<UserRecord> userRecords = UserRecordCSV.fromCSV(file);

        try {
          Set<String> roles = new HashSet<String>();
          roles.add(role);

          dataStore.saveUsers(surveyId, mapToSecureUserRecords(userRecords, roles, permissions));
          writer.print("OK");
        } catch (DataStoreException e) {
          writer.print("ERR:" + e.getMessage());
        } catch (DuplicateKeyException e) {
          writer.print("ERR:" + e.getMessage());
        }

      } catch (FileUploadException e) {
        writer.print("ERR:" + e.getMessage());
      } catch (IOException e) {
        writer.print("ERR:" + e.getMessage());
      }
    }

    writer.close();
  }
}
