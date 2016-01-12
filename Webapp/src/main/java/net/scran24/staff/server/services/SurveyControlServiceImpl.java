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

package net.scran24.staff.server.services;

import javax.servlet.ServletException;

import net.scran24.datastore.DataStore;
import net.scran24.datastore.DataStoreException;
import net.scran24.datastore.shared.SurveyParameters;
import net.scran24.staff.client.SurveyControlService;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.google.inject.Injector;

public class SurveyControlServiceImpl extends RemoteServiceServlet implements SurveyControlService {
	private static final long serialVersionUID = 3760196120426005805L;

	private DataStore dataStore;

	@Override
	public void init() throws ServletException {
		Injector injector = (Injector)this.getServletContext().getAttribute("intake24.injector");
		dataStore = injector.getInstance(DataStore.class);
		
		getServletContext().setAttribute("scran24.surveyControl", this);
	}
	
	@Override
	public SurveyParameters getParameters() {
		String surveyId = getThreadLocalRequest().getParameter("surveyId");

		if (surveyId == null)
			throw new RuntimeException("surveyId not specified");
		
		try {
			return dataStore.getSurveyParameters(getThreadLocalRequest().getParameter("surveyId"));
		} catch (DataStoreException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void setParameters(SurveyParameters state) {
		String surveyId = getThreadLocalRequest().getParameter("surveyId");

		if (surveyId == null)
			throw new RuntimeException("surveyId not specified");
		
		try {
			dataStore.setSurveyParameters(surveyId, state);
		} catch (DataStoreException e) {
			throw new RuntimeException(e);
		}
	}
}