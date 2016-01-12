/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.common.server.auth;

import org.apache.shiro.authc.UsernamePasswordToken;

public class ScranAuthToken extends UsernamePasswordToken {
	private static final long serialVersionUID = -8499626399101788191L;
	
	public final String survey;
	public final String username;
	public final String password;
		
	private final ScranUserId principal;
	
	public ScranAuthToken(String survey, String username, String password) {
		super(username, password);
		this.survey = survey;
		this.username = username;
		this.password = password;
		this.principal = new ScranUserId(username, survey);
	}
	
	@Override
	public Object getPrincipal() {
		return principal;
	}
}
