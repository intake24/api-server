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

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.inject.Injector;

import net.scran24.datastore.DataStore;
import uk.ac.ncl.openlab.intake24.services.output.CSVOutput;

public class DownloadSurveyDataService extends HttpServlet {
	private static final long serialVersionUID = -68146683997014578L;
	private DataStore dataStore;
	private CSVOutput csvOutput;

	@Override
	public void init() throws ServletException {
		Injector injector = (Injector)this.getServletContext().getAttribute("intake24.injector");
		dataStore = injector.getInstance(DataStore.class);
		csvOutput = new CSVOutput(dataStore);
		
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		final long timeFrom = Long.parseLong(req.getParameter("timeFrom"));
		final long timeTo = Long.parseLong(req.getParameter("timeTo"));
		final String surveyId = req.getParameter("surveyId");
		
		resp.setContentType("text/csv");
		resp.setHeader("Content-Disposition", "attachment; filename=" + "\"intake24_output_" + surveyId + ".csv\"");

		final ServletOutputStream outputStream = resp.getOutputStream();
		
		csvOutput.writeCSV(surveyId, timeFrom, timeTo, outputStream, false);		
	}
}
