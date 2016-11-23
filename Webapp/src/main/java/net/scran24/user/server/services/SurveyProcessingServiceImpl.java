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

package net.scran24.user.server.services;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;

import javax.servlet.ServletException;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.workcraft.gwt.shared.client.Option;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.google.inject.Injector;

import net.scran24.common.server.auth.ScranUserId;
import net.scran24.datastore.DataStore;
import net.scran24.datastore.DataStoreException;
import net.scran24.datastore.MissingFoodRecord;
import net.scran24.datastore.NutritionMappedFood;
import net.scran24.datastore.NutritionMappedMeal;
import net.scran24.datastore.NutritionMappedSurvey;
import net.scran24.datastore.NutritionMappedSurveyRecord;
import net.scran24.datastore.SecureUserRecord;
import net.scran24.datastore.shared.SurveyParameters;
import net.scran24.user.client.services.SurveyProcessingService;
import net.scran24.user.shared.CompletedFood;
import net.scran24.user.shared.CompletedMeal;
import net.scran24.user.shared.CompletedMissingFood;
import net.scran24.user.shared.CompletedSurvey;
import net.scran24.user.shared.Meal;

public class SurveyProcessingServiceImpl extends RemoteServiceServlet implements SurveyProcessingService {
	private static final long serialVersionUID = -5525469181691523598L;
	
	private final Logger log = LoggerFactory.getLogger(SurveyProcessingServiceImpl.class);
	
	private NutrientMapper nutrientMapper;
	
	private DataStore dataStore;
	
	@Override
	public void init() throws ServletException {
		Injector injector = (Injector) this.getServletContext().getAttribute("intake24.injector"); 
		
		dataStore = injector.getInstance(DataStore.class);
		nutrientMapper = injector.getInstance(NutrientMapper.class);
	}

	@Override
	public void submit(CompletedSurvey survey) {
		Subject subject = SecurityUtils.getSubject();
		ScranUserId userId = (ScranUserId) subject.getPrincipal();				

		if (userId == null)
			throw new RuntimeException("User must be logged in");		
		
		try {
			log.info("Survey submission from " + userId.username);
			log.debug("Mapping nutrients");
			
			SurveyParameters surveyParameters = dataStore.getSurveyParameters(userId.survey);
			NutritionMappedSurvey nutritionMappedSurvey = nutrientMapper.map(survey, surveyParameters.locale);
			
			for (NutritionMappedMeal m: nutritionMappedSurvey.meals){
			  for (NutritionMappedFood f: m.foods) {
			    System.out.println("Food name: " + f.englishDescription);
			    for (Entry<Long, Double> e: f.nutrients.entrySet()) {
			      System.out.print(String.format("%d: %.2f", e.getKey(), e.getValue()));
			    }
			  }
			}
			  

			log.debug("Storing to database");
			
			Option<SecureUserRecord> userRecord = dataStore.getUserRecord(userId.survey, userId.username);		
		if (userRecord.isEmpty()) {
			log.error("User " + survey.username + " not found!");
			throw new RuntimeException("User " + survey.username + " not found!");
		} else {
			dataStore.saveSurvey(userId.survey, userId.username, new NutritionMappedSurveyRecord(nutritionMappedSurvey, 
					userRecord.getOrDie().customFields));
			
			List<String> foodCodes = new ArrayList<String>(); 
			
			for (CompletedMeal m: survey.meals)
				for (CompletedFood f: m.foods )
					foodCodes.add(f.code);
			
			log.debug("Updating food popularity counters");
			
			dataStore.incrementPopularityCount(foodCodes);
			
			log.debug("Storing missing foods");
						
			ArrayList<MissingFoodRecord> missingFoodRecords = new ArrayList<MissingFoodRecord>();
			
			for (CompletedMissingFood missingFood: survey.missingFoods)
				missingFoodRecords.add(new MissingFoodRecord(new Date().getTime(), userId.survey, userId.username, missingFood.name, missingFood.brand, missingFood.description, missingFood.portionSize, missingFood.leftovers));
			
			dataStore.saveMissingFoods(missingFoodRecords);
		}
		
			log.info("Submission successful");
		} catch (DataStoreException e) {
			throw new RuntimeException(e);
		}
	}
}
