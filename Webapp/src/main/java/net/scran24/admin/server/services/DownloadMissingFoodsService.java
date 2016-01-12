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

import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.scran24.datastore.DataStore;
import net.scran24.datastore.DataStoreException;
import net.scran24.datastore.MissingFoodRecord;

import org.workcraft.gwt.shared.client.Callback1;

import com.google.inject.Injector;

import au.com.bytecode.opencsv.CSVWriter;

@SuppressWarnings("serial")
public class DownloadMissingFoodsService extends HttpServlet {

	private DataStore dataStore;

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
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		final long timeFrom = Long.parseLong(req.getParameter("timeFrom"));
		final long timeTo = Long.parseLong(req.getParameter("timeTo"));

		final ServletOutputStream outputStream = resp.getOutputStream();
		final CSVWriter writer = new CSVWriter(new PrintWriter(outputStream));

		resp.setContentType("text/csv");
		resp.setHeader("Content-Disposition", "attachment; filename=" + "\"intake24_missing_foods.csv\"");

		String[] header = new String[] { "Report date", "Survey id", "User name", "Food name", "Brand", "Description", "Portion size", "Leftovers" };

		final DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.UK);

		try {
			writer.writeNext(header);

			dataStore.processMissingFoods(timeFrom, timeTo, new Callback1<MissingFoodRecord>() {
				public void call(MissingFoodRecord missingFood) {

					String[] row = new String[] { dateFormat.format(new Date(missingFood.submittedAt)), missingFood.surveyId, missingFood.userName,
							missingFood.name, missingFood.brand, missingFood.description, missingFood.portionSize, missingFood.leftovers };

					writer.writeNext(row);

				}
			});
		} catch (DataStoreException e) {
			throw new ServletException(e);
		} finally {
			writer.close();
		}
	}
}
