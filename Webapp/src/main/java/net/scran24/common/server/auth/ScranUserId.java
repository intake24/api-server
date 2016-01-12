/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.common.server.auth;

public class ScranUserId {
	public final String username;
	public final String survey;

	public ScranUserId(String username, String survey) {
		this.username = username;
		this.survey = survey;
	}
}
