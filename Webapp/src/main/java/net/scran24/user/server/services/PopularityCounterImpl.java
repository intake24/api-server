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

import java.util.Map;
import java.util.Set;

import net.scran24.datastore.DataStore;
import net.scran24.datastore.DataStoreException;
import uk.ac.ncl.openlab.intake24.services.foodlookup.PopularityCounter;

public class PopularityCounterImpl implements PopularityCounter {
	
	private final DataStore dataStore;
	
	public PopularityCounterImpl(DataStore dataStore) {
			this.dataStore = dataStore;
	}

	@Override
	public Map<String, Integer> getCount(Set<String> codes) {
		try {
			return dataStore.getPopularityCount(codes);
		} catch (DataStoreException e) {
			throw new RuntimeException(e); // let it crash rather than silently fail			
		}
	}
}
