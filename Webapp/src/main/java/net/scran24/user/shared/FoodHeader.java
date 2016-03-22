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
*/

package net.scran24.user.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

public class FoodHeader implements IsSerializable {
	public String code;
	public String localDescription;
	
	@Deprecated
	public FoodHeader() {} ;

	public FoodHeader(String code, String localDescription) {		
		this.code = code;
		this.localDescription = localDescription;
	}
	
	@Override
	public String toString() {
		return code + " " + localDescription;
	}
	
	public String description() {
		return localDescription;
	}
}
