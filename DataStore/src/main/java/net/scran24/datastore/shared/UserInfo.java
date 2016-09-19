/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.datastore.shared;


import java.util.Map;

import com.google.gwt.user.client.rpc.IsSerializable;

public class UserInfo implements IsSerializable {
	public String userName;
	public String surveyId;
	public Map<String, String> userData;
	public SurveyParameters surveyParameters;
	public String imageBaseUrl;
	
	@Deprecated
	public UserInfo() { }

	public UserInfo(String userName, String surveyId, SurveyParameters surveyParameters, Map<String, String> userData, String imageBaseUrl) {
		this.userName = userName;
		this.surveyId = surveyId;
		this.surveyParameters = surveyParameters;
		this.userData = userData;
		this.imageBaseUrl = imageBaseUrl;
	}
}
