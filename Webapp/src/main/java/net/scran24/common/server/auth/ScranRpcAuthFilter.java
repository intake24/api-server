/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.common.server.auth;

import java.io.IOException;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.filter.authz.AuthorizationFilter;
import org.apache.shiro.web.util.WebUtils;

public class ScranRpcAuthFilter extends AuthorizationFilter {
	@Override
	protected boolean onAccessDenied(ServletRequest request, ServletResponse response, Object mappedValue) throws Exception {
		Subject subject = getSubject(request, response);

		HttpServletResponse httpResponse = WebUtils.toHttp(response);

		if (!subject.isAuthenticated()) {
			httpResponse.addHeader("WWW-Authenticate", "8");
			httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED);
		} else {
			httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
		}
		return false;
	}

	@Override
	public boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) throws IOException {
		Subject subject = getSubject(request, response);
		
		if (!subject.isAuthenticated())
			return false;

		String[] restrictions = (String[]) mappedValue;

		if (restrictions == null || restrictions.length == 0) {
			// no restrictions specified, deny
			return false;
		}
		
		if (restrictions.length == 1 && restrictions[0].equals("*"))
			return true;

		for (String r : restrictions) {
			if (r.startsWith("r:") && subject.hasRole(r.substring(2)))
				return true;
			if (r.startsWith("p:")) {
				String perm = r.substring(2);
				if (perm.contains("$surveyId")) {
					HttpServletRequest httpRequest = WebUtils.toHttp(request);
					
					String surveyId = httpRequest.getParameter("surveyId");
					if (surveyId == null)
						return false;
					
					String p = perm.replace("$surveyId", surveyId);
					// System.out.println ("Checking permissions [" + p + "]");
					
					return subject.isPermitted(p);
				} else
				 return subject.isPermitted(perm);
			}
		}

		return false;
	}
}