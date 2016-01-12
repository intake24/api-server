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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import net.scran24.common.server.auth.ScranAuthRealm;
import net.scran24.datastore.DataStore;

import org.apache.shiro.authc.credential.HashedCredentialsMatcher;
import org.apache.shiro.crypto.hash.Sha256Hash;
import org.apache.shiro.mgt.RealmSecurityManager;
import org.apache.shiro.web.env.EnvironmentLoader;
import org.apache.shiro.web.env.WebEnvironment;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class InitListener implements ServletContextListener {

	public void contextInitialized(ServletContextEvent contextEvent) {
		ServletContext context = contextEvent.getServletContext();

		Map<String, String> configParams = new HashMap<String, String>();

		Enumeration<String> paramNames = context.getInitParameterNames();

		while (paramNames.hasMoreElements()) {
			String name = paramNames.nextElement();
			configParams.put(name, context.getInitParameter(name));
		}

		AbstractModule configModule;

		try {
			Constructor<?> ctor = Class.forName(context.getInitParameter("config-module")).getConstructor(Map.class);
			configModule = (AbstractModule) ctor.newInstance(configParams);
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException | NoSuchMethodException | SecurityException
				| IllegalArgumentException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}

		Injector injector = Guice.createInjector(configModule);

		context.setAttribute("intake24.injector", injector);

		WebEnvironment shiroEnvironment = (WebEnvironment) context.getAttribute(EnvironmentLoader.ENVIRONMENT_ATTRIBUTE_KEY);
		RealmSecurityManager securityManager = (RealmSecurityManager) shiroEnvironment.getSecurityManager();

		ScranAuthRealm securityRealm = new ScranAuthRealm(injector.getInstance(DataStore.class));

		HashedCredentialsMatcher credentialsMatcher = new HashedCredentialsMatcher();
		credentialsMatcher.setHashAlgorithmName(Sha256Hash.ALGORITHM_NAME);
		credentialsMatcher.setStoredCredentialsHexEncoded(false);
		credentialsMatcher.setHashIterations(1024);

		securityRealm.setCredentialsMatcher(credentialsMatcher);
		securityManager.setRealm(securityRealm);

	}

	public void contextDestroyed(ServletContextEvent contextEvent) {

	}
}