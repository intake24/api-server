/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.common.server.auth;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;

public class LogoutService extends HttpServlet {
	private static final long serialVersionUID = 8033143196800386147L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		Subject subject = SecurityUtils.getSubject();
		
		String surveyPath = "";
		
		if (subject.isAuthenticated()) {
			ScranUserId userId = (ScranUserId) subject.getPrincipal();			
			surveyPath = (userId.survey.equals("admin")) ? "admin/" : "surveys/" + userId.survey + "/";
		}

		subject.logout();
		
		String query = req.getQueryString();
		if (query == null)
			query = "";
		else
			query = "?" + query;
		
		
		
		resp.sendRedirect("../" + surveyPath + query);
	}
}
