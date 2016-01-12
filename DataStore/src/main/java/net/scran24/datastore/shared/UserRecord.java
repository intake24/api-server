/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.datastore.shared;

import java.util.Map;

import com.google.gwt.user.client.rpc.IsSerializable;

public class UserRecord implements IsSerializable {
	public String username;
	public String password;
	public Map<String, String> customFields;
	
	@Deprecated
	public UserRecord() {
		
	}

	public UserRecord(String username, String password, Map<String, String> customFields) {
		this.username = username;
		this.password = password;
		this.customFields = customFields;
	}
}
