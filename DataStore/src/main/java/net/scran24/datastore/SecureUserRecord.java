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

package net.scran24.datastore;

import java.util.Map;
import java.util.Set;

public class SecureUserRecord {	
	final public String username;
	final public String passwordHashBase64;
	final public String passwordSaltBase64;
	final public String passwordHasher;
	final public Set<String> roles;
	final public Set<String> permissions;
	final public Map<String, String> customFields;

	public SecureUserRecord(String username, String passwordHashBase64, String passwordSaltBase64, Set<String> roles, Set<String> permissions,
			Map<String, String> customFields) {
		this(username, passwordHashBase64, passwordSaltBase64, "shiro-sha256", roles, permissions, customFields);
	}

	public SecureUserRecord(String username, String passwordHashBase64, String passwordSaltBase64, String passwordHasher, Set<String> roles,
			Set<String> permissions, Map<String, String> customFields) {
		this.username = username;
		this.passwordHashBase64 = passwordHashBase64;
		this.passwordSaltBase64 = passwordSaltBase64;
		this.passwordHasher = passwordHasher;
		this.roles = roles;
		this.permissions = permissions;
		this.customFields = customFields;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((customFields == null) ? 0 : customFields.hashCode());
		result = prime * result + ((passwordHashBase64 == null) ? 0 : passwordHashBase64.hashCode());
		result = prime * result + ((passwordHasher == null) ? 0 : passwordHasher.hashCode());
		result = prime * result + ((passwordSaltBase64 == null) ? 0 : passwordSaltBase64.hashCode());
		result = prime * result + ((permissions == null) ? 0 : permissions.hashCode());
		result = prime * result + ((roles == null) ? 0 : roles.hashCode());
		result = prime * result + ((username == null) ? 0 : username.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SecureUserRecord other = (SecureUserRecord) obj;
		
		// System.err.println("Custom fields");
		
		if (customFields == null) {
			if (other.customFields != null)
				return false;
		} else if (!customFields.equals(other.customFields))
			return false;
		
		// System.err.println("Password");
		
		if (passwordHashBase64 == null) {
			if (other.passwordHashBase64 != null)
				return false;
		} else if (!passwordHashBase64.equals(other.passwordHashBase64))
			return false;
		if (passwordHasher == null) {
			if (other.passwordHasher != null)
				return false;
		} else if (!passwordHasher.equals(other.passwordHasher))
			return false;
		if (passwordSaltBase64 == null) {
			if (other.passwordSaltBase64 != null)
				return false;
		} else if (!passwordSaltBase64.equals(other.passwordSaltBase64))
			return false;
		
		// System.err.println("Permissions");
		
		if (permissions == null) {
			if (other.permissions != null)
				return false;
		} else if (!permissions.equals(other.permissions))
			return false;
		
		// System.err.println("Roles");
		
		if (roles == null) {
			if (other.roles != null)
				return false;
		} else if (!roles.equals(other.roles))
			return false;
		
		// System.err.println("Username");
		
		if (username == null) {
			if (other.username != null)
				return false;
		} else if (!username.equals(other.username))
			return false;
		return true;
	}
}
