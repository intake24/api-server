/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey;

import java.util.List;

import net.scran24.datastore.shared.UserInfo;

public interface SurveyManager<T> {
	public void runNextStage(SurveyStage<T> stage, T data);
	public List<String> log();
	public UserInfo getUserInfo();
}
