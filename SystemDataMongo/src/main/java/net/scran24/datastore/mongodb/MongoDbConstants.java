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

package net.scran24.datastore.mongodb;

public class MongoDbConstants {
	protected static final String FIELD_COUNT = "count";
	protected static final String FIELD_CODE = "code";
	protected static final String FIELD_SUSPENSION_REASON = "reason";
	protected static final String FIELD_END_DATE = "endDate";
	protected static final String FIELD_START_DATE = "startDate";
	protected static final String FIELD_SCHEME_NAME = "schemeName";
	protected static final String FIELD_LOCALE_ID = "locale";
	protected static final String FIELD_ALLOW_GEN_USERS = "allowGenUsers";
	
	protected static final String STATE_SUSPENDED = "suspended";
	protected static final String STATE_ACTIVE = "active";
	protected static final String STATE_NOT_INITIALISED = "notinit";
	
	protected static final String FIELD_SURVEY_STATE = "state";
	protected static final String FIELD_SURVEY_ID = "surveyId";
	protected static final String FIELD_END_TIME = "endTime";
	
	protected static final String FIELD_CUSTOM_VALUE = "value";
	protected static final String FIELD_CUSTOM_NAME = "name";
	protected static final String FIELD_USERDATA = "userdata";
	protected static final String FIELD_PERMISSIONS = "permissions";
	
	protected static final String FIELD_ROLES = "roles";
	protected static final String FIELD_SALT = "salt";
	protected static final String FIELD_PASSWORD = "password";
	protected static final String FIELD_HASHER = "hasher";
	protected static final String FIELD_USERNAME = "username";
    protected static final String FIELD_SURVEY_MONKEY_URL = "surveyMonkeyUrl";

	protected static final String USERS_COL_PREFIX = "users_";
	protected static final String SURVEYS_COL_PREFIX = "surveys_";
	protected static final String SURVEY_STATE_COLLECTION = "survey_state";
	protected static final String POPULARITY_COLLECTION = "popularity";
	protected static final String GLOBAL_VALUES_COLLECTION = "global_values";
	protected static final String MISSING_FOODS_COLLECTION = "missing_foods"; 
	protected static final String SUPPORT_STAFF_COLLECTION = "support_staff";
	protected static final String HELP_REQUEST_TIME_COLLECTION = "help_request_time";
}
