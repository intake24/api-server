/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.shared;

import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.IsSerializable;

public class CompletedSurvey implements IsSerializable {
	public long startTime;
	public long endTime;
	public List<CompletedMeal> meals;
	public List<CompletedMissingFood> missingFoods;
	public List<String> log;
	public String username;
	public Map<String, String> customData;
	
	@Deprecated
	public CompletedSurvey() { }

	public CompletedSurvey(long startTime, long endTime, List<CompletedMeal> meals, List<CompletedMissingFood> missingFoods, List<String> log, String username, Map<String, String> customData) {
		this.startTime = startTime;
		this.endTime = endTime;
		this.meals = meals;
		this.missingFoods = missingFoods;
		this.log = log;
		this.username = username;
		this.customData = customData;
	}
}