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

package net.scran24.datastore.shared;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.user.client.rpc.IsSerializable;

public class CompletedPortionSize implements IsSerializable {
	public String scriptName;
	public Map<String, String> data;

	@Deprecated
	public CompletedPortionSize() {
	}

	public CompletedPortionSize(String scriptName, Map<String, String> data) {
		this.scriptName = scriptName;
		
		// this is to ensure that the data is stored internally as a standard HashMap
		// (it could have come in as a PMap) to prevent serialization exceptions
		this.data = new HashMap<String, String>();
		this.data.putAll(data);
	}

	public double servingWeight() {
		return Double.parseDouble(data.get("servingWeight"));
	}

	public double leftoversWeight() {
		return Double.parseDouble(data.get("leftoversWeight"));
	}
	
	public static CompletedPortionSize ignore(String reason) {
		Map<String, String> data = new HashMap<String, String>();
		
		data.put("reason", reason);
		data.put("servingWeight", "0.0");
		data.put("leftoversWeight", "0.0");
		
		return new CompletedPortionSize("ignored", data);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((data == null) ? 0 : data.hashCode());
		result = prime * result + ((scriptName == null) ? 0 : scriptName.hashCode());
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
		CompletedPortionSize other = (CompletedPortionSize) obj;
		
		// System.out.println("CPS begin");
		
		if (data == null) {
			if (other.data != null)
				return false;
		} else if (!data.equals(other.data))
			return false;
		
		// System.out.println("CPS data ok");
		
		if (scriptName == null) {
			if (other.scriptName != null)
				return false;
		} else if (!scriptName.equals(other.scriptName))
			return false;
		
		// System.out.println("CPS script OK");
		
		return true;
	}
	
}
