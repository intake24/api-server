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

package net.scran24.user.server.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.scran24.datastore.DataStore;
import net.scran24.datastore.DataStoreException;
import net.scran24.datastore.mongodb.MongoDbDataStore;

import org.workcraft.gwt.shared.client.Pair;

import uk.ac.ncl.openlab.intake24.foodxml.FoodIndexDataServiceXmlImpl;
import uk.ac.ncl.openlab.intake24.foodxml.UserFoodDataServiceXmlImpl;
import uk.ac.ncl.openlab.intake24.nutrientsndns.CsvNutrientTableParser;
import uk.ac.ncl.openlab.intake24.nutrientsndns.LegacyNutrientMappingServiceImpl;
import uk.ac.ncl.openlab.intake24.nutrientsndns.LegacyNutrientTables;

import uk.ac.ncl.openlab.intake24.nutrientsndns.NutrientTable;
import uk.ac.ncl.openlab.intake24.services.fooddb.user.FoodDatabaseService;
import uk.ac.ncl.openlab.intake24.services.foodindex.FoodIndex;
import uk.ac.ncl.openlab.intake24.services.foodindex.FoodIndexDataService;
import uk.ac.ncl.openlab.intake24.services.foodindex.Splitter;
import uk.ac.ncl.openlab.intake24.services.foodindex.english.FoodIndexImpl_en_GB;
import uk.ac.ncl.openlab.intake24.services.foodindex.english.SplitterImpl_en_GB;
import uk.ac.ncl.openlab.intake24.services.nutrition.NutrientMappingService;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Names;

public class NZConfig extends AbstractModule {
	public final Map<String, String> webXmlConfig;

	public NZConfig(Map<String, String> webXmlConfig) {
		this.webXmlConfig = webXmlConfig;
	}

	@Provides
	@Singleton
	protected Map<String, FoodIndex> localFoodIndex(Injector injector) {
		FoodIndexDataService indexFoodDataService = injector.getInstance(FoodIndexDataService.class);

		Map<String, FoodIndex> result = new HashMap<String, FoodIndex>();
		result.put("en_GB", new FoodIndexImpl_en_GB(indexFoodDataService));
		return result;
	}

	@Provides
	@Singleton
	protected Map<String, Splitter> localSplitter(Injector injector) {
		FoodIndexDataService foodDataService = injector.getInstance(FoodIndexDataService.class);

		Map<String, Splitter> result = new HashMap<String, Splitter>();
		result.put("en_GB", new SplitterImpl_en_GB(foodDataService));
		return result;
	}

	@Provides
	@Singleton
	protected Map<String, NutrientTable> nutrientTables() {
		Map<String, NutrientTable> result = new HashMap<String, NutrientTable>();
				
		result.put("NDNS", CsvNutrientTableParser.parseTable(webXmlConfig.get("ndns-data-path"), LegacyNutrientTables.ndnsCsvTableMapping()));
		result.put("NZ", CsvNutrientTableParser.parseTable(webXmlConfig.get("nz-data-path"), LegacyNutrientTables.nzCsvTableMapping()));
		
		return result;
	}

	@Provides
	@Singleton
	protected DataStore dataStore() {
		try {
			return new MongoDbDataStore(webXmlConfig.get("mongodb-host"), Integer.parseInt(webXmlConfig.get("mongodb-port")),
					webXmlConfig.get("mongodb-database"), webXmlConfig.get("mongodb-user"), webXmlConfig.get("mongodb-password"));
		} catch (NumberFormatException e) {
			throw new RuntimeException(e);
		} catch (DataStoreException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void configure() {
		bindConstant().annotatedWith(Names.named("xml-data-path")).to(webXmlConfig.get("xml-data-path"));
		bind(FoodIndexDataService.class).to(FoodIndexDataServiceXmlImpl.class);
		bind(FoodDatabaseService.class).to(UserFoodDataServiceXmlImpl.class);
		bind(NutrientMappingService.class).to(LegacyNutrientMappingServiceImpl.class);
	}
}
