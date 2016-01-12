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
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.scran24.datastore.DataStore;
import net.scran24.datastore.DataStoreException;
import net.scran24.datastore.NutritionMappedSurveyRecordWithId;
import net.scran24.datastore.SecureUserRecord;
import net.scran24.datastore.shared.CustomDataScheme;
import net.scran24.datastore.shared.CustomDataScheme.CustomFieldDef;
import net.scran24.datastore.shared.DataSchemeMap;
import net.scran24.datastore.shared.SurveySchemes;

import org.workcraft.gwt.shared.client.Callback1;

import com.google.inject.Injector;

import au.com.bytecode.opencsv.CSVWriter;

public class DownloadActivityReportService extends HttpServlet {
	private static final long serialVersionUID = -68146683997014578L;
	private DateFormat dateFormat = new SimpleDateFormat("dd.MM.yy");
	private DataStore dataStore;

	private static class SurveyStats {
		public final long endTime;
		public final long timeToComplete;

		public SurveyStats(String username, long startTime, long endTime) {
			this.endTime = endTime;
			this.timeToComplete = endTime - startTime;
		}
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

	private String showDate(long unixTime) {
		return dateFormat.format(new Date(unixTime));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
	 * javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		long timeFrom = Long.parseLong(req.getParameter("timeFrom"));
		long timeTo = Long.parseLong(req.getParameter("timeTo"));
		String surveyId = req.getParameter("surveyId");

		resp.setContentType("text/csv");
		resp.setHeader("Content-Disposition", "attachment; filename=" + "\"intake24_activity_" + surveyId + ".csv\"");

		ServletOutputStream outputStream = resp.getOutputStream();
		final CSVWriter writer = new CSVWriter(new PrintWriter(outputStream));

		try {
			final CustomDataScheme dataScheme = DataSchemeMap.dataSchemeFor(SurveySchemes.schemeForId(dataStore.getSurveyParameters(surveyId).schemeName));

			final Map<String, List<SurveyStats>> surveyStats = new HashMap<String, List<SurveyStats>>();

			List<SecureUserRecord> users = dataStore.getUserRecords(surveyId, "respondent");

			dataStore.processSurveys(surveyId, timeFrom, timeTo, new Callback1<NutritionMappedSurveyRecordWithId>() {
				@Override
				public void call(NutritionMappedSurveyRecordWithId record) {
					List<SurveyStats> stats = surveyStats.get(record.survey.userName);
					if (stats == null)
						stats = new ArrayList<SurveyStats>();

					stats.add(new SurveyStats(record.survey.userName, record.survey.startTime, record.survey.endTime));

					surveyStats.put(record.survey.userName, stats);
				}
			});

			ArrayList<String> header = new ArrayList<String>();

			header.add("User ID");
			
			for (CustomFieldDef f : dataScheme.userCustomFields())
				header.add(f.description);

			header.add("Minimum time to complete, min");
			header.add("Average time to complete, min");
			header.add("Maximum time to complete, min");
			header.add("Total number of submissions");
			header.add("Latest submission date");
			header.add("Submission dates");

			writer.writeNext(header.toArray(new String[header.size()]));

			for (SecureUserRecord user : users) {
				ArrayList<String> row = new ArrayList<String>();

				row.add(user.username);

				// user custom fields
				for (CustomFieldDef f : dataScheme.userCustomFields()) {
					if (user.customFields.containsKey(f.key))
						row.add(user.customFields.get(f.key));
					else
						row.add("N/A");
				}

				double min_ttc = Double.MAX_VALUE;
				double avg_ttc = 0;
				double max_ttc = 0;
				int submissionCount = 0;
				long latest_time = 0;

				List<SurveyStats> stats = surveyStats.get(user.username);

				if (stats == null) {
					row.add("");
					row.add("");
					row.add("");
					row.add("0");
					row.add("");
				} else {

					for (SurveyStats s : stats) {
						double ttc = (s.timeToComplete / 60000.0);

						if (ttc < min_ttc)
							min_ttc = ttc;
						if (ttc > max_ttc)
							max_ttc = ttc;

						avg_ttc += ttc;

						if (s.endTime > latest_time)
							latest_time = s.endTime;

						submissionCount += 1;
					}

					if (submissionCount > 0)
						avg_ttc /= submissionCount;

					row.add(String.format("%.2f", min_ttc));
					row.add(String.format("%.2f", avg_ttc));
					row.add(String.format("%.2f", max_ttc));
					row.add(Integer.toString(submissionCount));
					row.add(showDate(latest_time));

					for (SurveyStats s : stats)
						row.add(showDate(s.endTime));
				}
				
				writer.writeNext(row.toArray(new String[row.size()]));
			}
		} catch (DataStoreException e) {
			throw new ServletException(e);
		} finally {
			writer.close();
		}
	}
}
