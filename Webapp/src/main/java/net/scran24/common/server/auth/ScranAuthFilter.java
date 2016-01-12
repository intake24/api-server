/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.common.server.auth;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.filter.PathMatchingFilter;
import org.apache.shiro.web.util.WebUtils;
import org.workcraft.gwt.shared.client.Option;

public class ScranAuthFilter extends PathMatchingFilter {
	private final static String adminScripts = "/admin/**";
	private final static String userScripts = "/user/**";
	private final static String staffScripts = "/staff/**";

	private boolean isLoginPage(HttpServletRequest request) {
		String path = WebUtils.getPathWithinApplication(request);

		if (path.endsWith("/login/"))
			return true;
		else
			return false;
	}

	private Option<String> getLoginPage(HttpServletRequest request) {
		String path = WebUtils.getPathWithinApplication(request);

		if (path.equals("/admin/") || path.equals("/admin"))
			return Option.some("/admin/login/");
		else if (path.startsWith("/surveys/") && path.length() > 9) {
			String s = path.substring(9);
			if (!s.endsWith("/"))
				s += "/";
			String surveyId = s.substring(0, s.indexOf('/'));
			return Option.some("/surveys/" + surveyId + "/login/");
		} else
			// trying to access an invalid page, send an error
			return Option.none();
	}

	private boolean isUserPage(HttpServletRequest request, String surveyId) {
		String path = WebUtils.getPathWithinApplication(request);
		return path.equals("/surveys/" + surveyId + "/");
	}

	private String getUserPage(String surveyId) {
		return "/surveys/" + surveyId + "/";
	}

	private boolean isUserScript(HttpServletRequest request) {
		return pathsMatch(userScripts, request);
	}

	private boolean isStaffScript(HttpServletRequest request) {
		return pathsMatch(staffScripts, request);

	}

	private boolean isStaffPage(HttpServletRequest request, String surveyId) {
		String path = WebUtils.getPathWithinApplication(request);
		return path.equals("/surveys/" + surveyId + "/staff/");
	}

	private String getStaffPage(String surveyId) {
		return "/surveys/" + surveyId + "/staff/";
	}

	private boolean isAdminLoginPage(HttpServletRequest request) {
		String path = WebUtils.getPathWithinApplication(request);
		return path.equals("/admin/login/");
	}

	private boolean isAdminPage(HttpServletRequest request) {
		String path = WebUtils.getPathWithinApplication(request);
		return path.equals("/admin/");
	}

	private boolean isAdminScript(HttpServletRequest request) {
		return pathsMatch(adminScripts, request);
	}

	private boolean isAdmin(Subject subject) {
		return subject.hasRole("admin");
	}

	private boolean isStaff(Subject subject) {
		return subject.hasRole("staff");
	}

	private boolean isUser(Subject subject) {
		return subject.hasRole("respondent");
	}

	@Override
	protected boolean onPreHandle(ServletRequest request, ServletResponse response, Object mappedValue) throws Exception {
		Subject subject = SecurityUtils.getSubject();
		ScranUserId userId = (ScranUserId) subject.getPrincipal();
		final HttpServletResponse httpResponse = WebUtils.toHttp(response);
		final HttpServletRequest httpRequest = WebUtils.toHttp(request);

		// prevent caching issues
		// FIXME: needs to be more fine-grained, e.g. allow to cache scripts
		// http://stackoverflow.com/questions/49547/making-sure-a-web-page-is-not-cached-across-all-browsers

		httpResponse.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP
																																										// 1.1.
		httpResponse.setHeader("Pragma", "no-cache"); // HTTP 1.0.
		httpResponse.setDateHeader("Expires", 0); // Proxies.

		// forward the query string to the redirect target
		// helps when debugging using GWT dev mode
		final String query = (httpRequest.getQueryString() == null) ? "" : "?" + httpRequest.getQueryString();

		final String contextPath = httpRequest.getContextPath();

		// redirect users based on their authentication state
		// and what page they are trying to access
		if (!subject.isAuthenticated()) {
			if (isLoginPage(httpRequest)) {
				return true;
			} else {
				Option<String> loginPage = getLoginPage(httpRequest);

				if (loginPage.isEmpty())
					httpResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
				else
					httpResponse.sendRedirect(contextPath + loginPage.getOrDie() + query);
				return false;
			}
		} else {
			if (isAdmin(subject)) {
				if (!isAdminLoginPage(httpRequest) && (isAdminScript(httpRequest) || isAdminPage(httpRequest))) {
					return true;
				} else {
					httpResponse.sendRedirect(contextPath + "/admin/" + query);
					return false;
				}
			} else if (isUser(subject)) {
				if (isUserScript(httpRequest)) {
					return true;
				} else if (isUserPage(httpRequest, userId.survey)) {
					return true;
				} else {
					httpResponse.sendRedirect(contextPath + getUserPage(userId.survey) + query);
					return false;
				}
			} else if (isStaff(subject)) {
				if (isStaffScript(httpRequest)) {
					return true;
				} else if (isStaffPage(httpRequest, userId.survey)) {
					return true;
				} else {
					httpResponse.sendRedirect(contextPath + getStaffPage(userId.survey) + query);
					return false;
				}
			} else {
				// user has an incorrect role setup, refuse
				httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
				return false;
			}
		}
	}
}
