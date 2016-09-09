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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import net.scran24.datastore.DataStore;
import scala.concurrent.duration.Duration;
import scala.runtime.AbstractFunction0;
import uk.ac.ncl.openlab.intake24.datastoresql.DataStoreJavaAdapter;
import uk.ac.ncl.openlab.intake24.datastoresql.DataStoreScala;
import uk.ac.ncl.openlab.intake24.datastoresql.DataStoreSqlImpl;
import uk.ac.ncl.openlab.intake24.foodsql.NutrientMappingServiceSqlImpl;
import uk.ac.ncl.openlab.intake24.foodsql.admin.FoodDatabaseAdminImpl;
import uk.ac.ncl.openlab.intake24.foodsql.foodindex.FoodIndexDataImpl;
import uk.ac.ncl.openlab.intake24.foodsql.user.FoodDatabaseUserImpl;
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.FoodDatabaseAdminService;
import uk.ac.ncl.openlab.intake24.services.fooddb.user.FoodDatabaseService;
import uk.ac.ncl.openlab.intake24.services.foodindex.AbstractFoodIndex;
import uk.ac.ncl.openlab.intake24.services.foodindex.AutoReloadIndex;
import uk.ac.ncl.openlab.intake24.services.foodindex.FoodIndex;
import uk.ac.ncl.openlab.intake24.services.foodindex.FoodIndexDataService;
import uk.ac.ncl.openlab.intake24.services.foodindex.Splitter;
import uk.ac.ncl.openlab.intake24.services.foodindex.danish.FoodIndexImpl_da_DK;
import uk.ac.ncl.openlab.intake24.services.foodindex.danish.SplitterImpl_da_DK;
import uk.ac.ncl.openlab.intake24.services.foodindex.english.FoodIndexImpl_en_GB;
import uk.ac.ncl.openlab.intake24.services.foodindex.english.SplitterImpl_en_GB;
import uk.ac.ncl.openlab.intake24.services.foodindex.portuguese.FoodIndexImpl_pt_PT;
import uk.ac.ncl.openlab.intake24.services.foodindex.portuguese.SplitterImpl_pt_PT;
import uk.ac.ncl.openlab.intake24.services.nutrition.NutrientMappingService;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class SqlConfig extends AbstractModule {
	public final Map<String, String> webXmlConfig;

	public SqlConfig(Map<String, String> webXmlConfig) {
		this.webXmlConfig = webXmlConfig;
	}

	@Provides
	@Singleton
	protected Map<String, FoodIndex> localFoodIndex(Injector injector) {
		final FoodIndexDataService foodDataService = injector.getInstance(FoodIndexDataService.class);
		Map<String, FoodIndex> result = new HashMap<String, FoodIndex>();

		result.put("en_GB", new AutoReloadIndex(new AbstractFunction0<AbstractFoodIndex>() {
			@Override
			public AbstractFoodIndex apply() {
				return new FoodIndexImpl_en_GB(foodDataService);
			}
		}, Duration.create(60, TimeUnit.MINUTES), Duration.create(60, TimeUnit.MINUTES), "English"));

		result.put("pt_PT", new AutoReloadIndex(new AbstractFunction0<AbstractFoodIndex>() {
			@Override
			public AbstractFoodIndex apply() {
				return new FoodIndexImpl_pt_PT(foodDataService);
			}
		}, Duration.create(30, TimeUnit.MINUTES), Duration.create(60, TimeUnit.MINUTES), "Portuguese"));
		
		result.put("da_DK", new AutoReloadIndex(new AbstractFunction0<AbstractFoodIndex>() {
			@Override
			public AbstractFoodIndex apply() {
				return new FoodIndexImpl_da_DK(foodDataService);
			}
		}, Duration.create(30, TimeUnit.MINUTES), Duration.create(60, TimeUnit.MINUTES), "Danish"));
		

		return result;
	}

	@Provides
	@Singleton
	protected Map<String, Splitter> localSplitter(Injector injector) {
		FoodIndexDataService foodDataService = injector.getInstance(FoodIndexDataService.class);

		Map<String, Splitter> result = new HashMap<String, Splitter>();
		result.put("en_GB", new SplitterImpl_en_GB(foodDataService));
		result.put("pt_PT", new SplitterImpl_pt_PT(foodDataService));
		result.put("da_DK", new SplitterImpl_da_DK(foodDataService));
		return result;
	}
	

	@Provides
	@Singleton
	protected DataStoreScala dataStoreSqlImpl(Injector injector) {
		HikariConfig cpConfig = new HikariConfig();
		cpConfig.setJdbcUrl(webXmlConfig.get("sql-system-db-url"));
		cpConfig.setUsername(webXmlConfig.get("sql-system-db-user"));
		cpConfig.setPassword(webXmlConfig.get("sql-system-db-password"));

		return new DataStoreSqlImpl(new HikariDataSource(cpConfig));
	}

	@Provides
	@Singleton
	protected FoodDatabaseService foodDataServiceSqlImpl(Injector injector) {
		HikariConfig cpConfig = new HikariConfig();
		cpConfig.setJdbcUrl(webXmlConfig.get("sql-foods-db-url"));
		cpConfig.setUsername(webXmlConfig.get("sql-foods-db-user"));
		cpConfig.setPassword(webXmlConfig.get("sql-foods-db-password"));

		return new FoodDatabaseUserImpl(new HikariDataSource(cpConfig));
	}
	
	@Provides
	@Singleton
	protected FoodDatabaseAdminService adminDataServiceSqlImpl(Injector injector) {
		HikariConfig cpConfig = new HikariConfig();
		cpConfig.setJdbcUrl(webXmlConfig.get("sql-foods-db-url"));
		cpConfig.setUsername(webXmlConfig.get("sql-foods-db-user"));
		cpConfig.setPassword(webXmlConfig.get("sql-foods-db-password"));

		return new FoodDatabaseAdminImpl(new HikariDataSource(cpConfig));
	}
	
	@Provides
	@Singleton
	protected FoodIndexDataService indexDataServiceSqlImpl(Injector injector) {
		HikariConfig cpConfig = new HikariConfig();
		cpConfig.setJdbcUrl(webXmlConfig.get("sql-foods-db-url"));
		cpConfig.setUsername(webXmlConfig.get("sql-foods-db-user"));
		cpConfig.setPassword(webXmlConfig.get("sql-foods-db-password"));

		return new FoodIndexDataImpl(new HikariDataSource(cpConfig));		
	}
	
	@Provides
	@Singleton
	protected NutrientMappingService nutrientMappingServiceSqlImpl(Injector injector) {
		HikariConfig cpConfig = new HikariConfig();
		cpConfig.setJdbcUrl(webXmlConfig.get("sql-foods-db-url"));
		cpConfig.setUsername(webXmlConfig.get("sql-foods-db-user"));
		cpConfig.setPassword(webXmlConfig.get("sql-foods-db-password"));

		return new NutrientMappingServiceSqlImpl(new HikariDataSource(cpConfig));		
	}

	@Override
	protected void configure() {
		bind(DataStore.class).to(DataStoreJavaAdapter.class);
	}
}
