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

package net.scran24.admin.server.services;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletException;

import net.scran24.admin.client.services.SurveyManagementService;
import net.scran24.datastore.DataStore;
import net.scran24.datastore.DataStoreException;

import org.apache.commons.io.IOUtils;
import org.workcraft.gwt.shared.client.Option;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.google.inject.Injector;

public class SurveyManagementServiceImpl extends RemoteServiceServlet implements SurveyManagementService {
	private static final long serialVersionUID = -878109524841664825L;
	private DataStore dataStore;
	
	public void copyTemplate (String srcPath, File dstFile, String surveyId, String locale) throws IOException {
		InputStream pageTemplate = getServletContext().getResourceAsStream(srcPath);
		String page = IOUtils.toString(pageTemplate).replace("$AUTH_REALM$",surveyId).replace("$LOCALE$", locale);
		dstFile.createNewFile();
		OutputStream pageOut = new FileOutputStream(dstFile);
		IOUtils.write(page, pageOut);
		pageTemplate.close();
		pageOut.close();
	}
	
	@Override
	public void init() throws ServletException {
		try {
			Injector injector = (Injector) this.getServletContext().getAttribute("intake24.injector");
			dataStore = injector.getInstance(DataStore.class);		
		} catch (Throwable e) {
			throw new ServletException(e);
		}
	}
	
	@Override
	public Option<String> createSurvey(String id, String scheme_id, String locale, boolean allowGenUsers, Option<String> surveyMonkeyUrl) {
		Option<String> idError = checkId(id);
		
		if (idError.isEmpty()) {

			try {		
				dataStore.initSurvey(id, scheme_id, locale, allowGenUsers, surveyMonkeyUrl);
				
				File baseDir = new File (new File(getServletContext().getRealPath("/surveys")), id);
				File loginDir = new File(baseDir, "login");
				File staffDir = new File(baseDir, "staff");
			
				boolean dirs_ok = true;
				
				dirs_ok &= baseDir.mkdir();
				dirs_ok &= loginDir.mkdir();
				dirs_ok &= staffDir.mkdir();
				
				if (!dirs_ok)
					throw new IOException("Failed to create survey directories (" + baseDir.getAbsolutePath() + ")");
			
				File userPageFile = new File(baseDir, "index.html");
				File loginPageFile = new File(loginDir, "index.html");
				File staffPageFile = new File(staffDir, "index.html");
				
				copyTemplate("/WEB-INF/userPageTemplate.html", userPageFile, id, locale);
				copyTemplate("/WEB-INF/loginPageTemplate.html", loginPageFile, id, locale);
				copyTemplate("/WEB-INF/staffPageTemplate.html", staffPageFile, id, locale);

				return Option.none();
			} catch (IOException e) {
				e.printStackTrace();
				return Option.some(e.getMessage());
			} catch (DataStoreException e) {
				e.printStackTrace();
				return Option.some(e.getMessage());
			}
		} else
			return idError;
	}

	private Option<String> checkId(String id) {
		if (id.equals("admin"))
			return Option.some("\"admin\" is a reserved ID and cannot be used for surveys");
		if (!id.matches("[A-Za-z0-9_]+"))
			return Option.some("Survey ID must be a single non-empty word (no spaces), containing only alphanumeric symbols or underscores");
		else {
			File dir = new File (new File(getServletContext().getRealPath("/surveys")), id);
			if (dir.exists())
				return Option.some ("Survey with this ID already exists");
			else
				return Option.none();
		}
	}

	@Override
	public List<String> listSurveys() {
		File dir = new File(getServletContext().getRealPath("/surveys"));
		return Arrays.asList(dir.list(new FilenameFilter() {
			@Override
			public boolean accept(File f, String n) {
				return f.isDirectory();
			}
		}));
	}
}