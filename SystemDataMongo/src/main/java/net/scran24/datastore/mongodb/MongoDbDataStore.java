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

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.scran24.datastore.DataStore;
import net.scran24.datastore.DataStoreException;
import net.scran24.datastore.DuplicateKeyException;
import net.scran24.datastore.LocalNutrientType;
import net.scran24.datastore.MissingFoodRecord;
import net.scran24.datastore.NutritionMappedSurveyRecord;
import net.scran24.datastore.NutritionMappedSurveyRecordWithId;
import net.scran24.datastore.SecureUserRecord;
import net.scran24.datastore.SupportStaffRecord;
import net.scran24.datastore.shared.SurveyParameters;
import net.scran24.datastore.shared.SurveyState;

import org.workcraft.gwt.shared.client.Callback1;
import org.workcraft.gwt.shared.client.Option;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoException;
import com.mongodb.MongoException.DuplicateKey;

@Singleton
public class MongoDbDataStore extends MongoDbConstants implements DataStore {

	private final MongoClient client;
	public final DB db;

	private final MongoDbDeserializer deserialiser = new MongoDbDeserializer();
	private final MongoDbSerializer serialiser = new MongoDbSerializer();

	@Inject
	public MongoDbDataStore(@Named("mongodb-host") String host, @Named("mongodb-port") int port, @Named("mongodb-database") String database,
			@Named("mongodb-user") String user, @Named("mongodb-password") String password) throws DataStoreException {
		try {

			if (!user.isEmpty() || !password.isEmpty())
				throw new IllegalArgumentException("Non-local MongoDB configuration is not supported");

			client = new MongoClient(host, port);
			db = client.getDB(database);
		} catch (UnknownHostException e) {
			throw new DataStoreException(e);
		}
	}

	private DBCollection getUsersCollection(String survey_id) {
		return db.getCollection(USERS_COL_PREFIX + survey_id);
	}

	private DBCollection getSurveysCollection(String survey_id) {
		return db.getCollection(SURVEYS_COL_PREFIX + survey_id);
	}

	private DBCollection getSurveyStateCollection() {
		return db.getCollection(SURVEY_STATE_COLLECTION);
	}

	private DBCollection getPopularityCollection() {
		return db.getCollection(POPULARITY_COLLECTION);
	}

	private DBCollection getGlobalValuesCollection() {
		return db.getCollection(GLOBAL_VALUES_COLLECTION);
	}

	private DBCollection getMissingFoodsCollection() {
		return db.getCollection(MISSING_FOODS_COLLECTION);
	}

	private DBCollection getSupportStaffCollection() {
		return db.getCollection(SUPPORT_STAFF_COLLECTION);
	}

	private DBCollection getHelpRequestTimeCollection() {
		return db.getCollection(HELP_REQUEST_TIME_COLLECTION);
	}

	public void deleteUsers(String survey_id, String role) throws DataStoreException {
		DBCollection col = getUsersCollection(survey_id);
		BasicDBObject removeQuery = new BasicDBObject(FIELD_ROLES, role);

		try {
			col.remove(removeQuery);
		} catch (MongoException e) {
			throw new DataStoreException(e);
		}
	}

	@Override
	public void addUser(String survey_id, SecureUserRecord user) throws DataStoreException, DuplicateKeyException {
		DBCollection col = getUsersCollection(survey_id);

		BasicDBList roles = new BasicDBList();
		roles.addAll(user.roles);

		BasicDBList permissions = new BasicDBList();
		permissions.addAll(user.permissions);

		BasicDBObject customFields = new BasicDBObject();

		for (String k : user.customFields.keySet())
			customFields.put(k, user.customFields.get(k));

		try {
			col.insert(new BasicDBObject(FIELD_USERNAME, user.username).append(FIELD_PASSWORD, user.passwordHashBase64)
					.append(FIELD_SALT, user.passwordSaltBase64).append(FIELD_ROLES, roles).append(FIELD_PERMISSIONS, permissions)
					.append(FIELD_USERDATA, customFields));
		} catch (DuplicateKey e) {
			throw new DuplicateKeyException(e);
		} catch (MongoException e) {
			throw new DataStoreException(e);
		}
	}

	@Override
	public void saveUsers(String survey_id, List<SecureUserRecord> users) throws DataStoreException, DuplicateKeyException {
		for (SecureUserRecord r : users)
			addUser(survey_id, r);
	}

	@Override
	public void processSurveys(String survey_id, long timeFrom, long timeTo, Callback1<NutritionMappedSurveyRecordWithId> processSurvey)
			throws DataStoreException {
		DBCollection col = getSurveysCollection(survey_id);
		DBObject query = new BasicDBObject(FIELD_END_TIME, new BasicDBObject("$gte", timeFrom).append("$lt", timeTo));
		DBCursor cursor = col.find(query);
		try {
			for (DBObject o : cursor) {
				processSurvey.call(deserialiser.deserialize(o));
			}
		} catch (MongoException e) {
			throw new DataStoreException(e);
		} finally {
			cursor.close();
		}
	}

	@Override
	public SurveyParameters getSurveyParameters(String survey_id) throws DataStoreException {
		DBCollection col = getSurveyStateCollection();

		BasicDBObject query = new BasicDBObject(FIELD_SURVEY_ID, survey_id);

		DBCursor cursor = col.find(query);
		try {
			if (!cursor.hasNext())
				throw new DataStoreException("Survey state record missing");
			else {
				DBObject stateObj = cursor.next();

				String stateStr = stateObj.get(FIELD_SURVEY_STATE).toString();

				SurveyState state;

				switch (stateStr) {
				case STATE_NOT_INITIALISED:
					state = SurveyState.NOT_INITIALISED;
					break;
				case STATE_ACTIVE:
					state = SurveyState.ACTIVE;
					break;
				case STATE_SUSPENDED:
					state = SurveyState.SUSPENDED;
					break;
				default:
					throw new DataStoreException("bad format of survey state object");
				}

				Option<String> surveyMonkeyUrl = Option.none();

				if (stateObj.containsField(FIELD_SURVEY_MONKEY_URL))
					surveyMonkeyUrl = Option.some((String) stateObj.get(FIELD_SURVEY_MONKEY_URL));

				String locale = (String) stateObj.get(FIELD_LOCALE_ID);

				if (locale == null)
					locale = "en_GB"; // use default for old records

				return new SurveyParameters(state, (Long) stateObj.get(FIELD_START_DATE), (Long) stateObj.get(FIELD_END_DATE),
						(String) stateObj.get(FIELD_SCHEME_NAME), locale, (Boolean) stateObj.get(FIELD_ALLOW_GEN_USERS),
						(String) stateObj.get(FIELD_SUSPENSION_REASON), surveyMonkeyUrl);

			}
		} catch (MongoException e) {
			throw new DataStoreException(e);
		}
	}

	@Override
	public void setSurveyParameters(String survey_id, SurveyParameters newParameters) throws DataStoreException {
		DBCollection col = getSurveyStateCollection();

		final BasicDBObject stateObj = new BasicDBObject(FIELD_SURVEY_ID, survey_id);

		switch (newParameters.state) {
		case NOT_INITIALISED:
			stateObj.append(FIELD_SURVEY_STATE, STATE_NOT_INITIALISED);
			break;
		case SUSPENDED:
			stateObj.append(FIELD_SURVEY_STATE, STATE_SUSPENDED);
			break;
		case ACTIVE:
			stateObj.append(FIELD_SURVEY_STATE, STATE_ACTIVE);
			break;
		}

		stateObj.append(FIELD_START_DATE, newParameters.startDate);
		stateObj.append(FIELD_END_DATE, newParameters.endDate);
		stateObj.append(FIELD_SCHEME_NAME, newParameters.schemeName);
		stateObj.append(FIELD_LOCALE_ID, newParameters.locale);
		stateObj.append(FIELD_ALLOW_GEN_USERS, newParameters.allowGenUsers);
		stateObj.append(FIELD_SUSPENSION_REASON, newParameters.suspensionReason);

		newParameters.surveyMonkeyUrl.accept(new Option.SideEffectVisitor<String>() {
			@Override
			public void visitSome(String item) {
				stateObj.append(FIELD_SURVEY_MONKEY_URL, item);
			}

			@Override
			public void visitNone() {
				if (stateObj.containsField(FIELD_SURVEY_MONKEY_URL))
					stateObj.removeField(FIELD_SURVEY_MONKEY_URL);
			}
		});

		BasicDBObject query = new BasicDBObject(FIELD_SURVEY_ID, survey_id);

		try {
			col.update(query, stateObj, true, false);
		} catch (MongoException e) {
			throw new DataStoreException(e);
		}
	}

	private SecureUserRecord parseUserRecord(BasicDBObject record) {
		String username = record.getString(FIELD_USERNAME);
		String passwordHashBase64 = record.getString(FIELD_PASSWORD);
		String passwordSaltBase64 = record.getString(FIELD_SALT);
		String hasher = record.getString(FIELD_HASHER);

		if (hasher == null)
			hasher = "shiro-sha256";

		BasicDBList rolesList = (BasicDBList) record.get(FIELD_ROLES);

		Set<String> roles = new HashSet<String>();

		if (rolesList != null)
			for (Object s : rolesList)
				roles.add((String) s);

		BasicDBList permissionsList = (BasicDBList) record.get(FIELD_PERMISSIONS);

		Set<String> permissions = new HashSet<String>();
		if (permissionsList != null)
			for (Object s : permissionsList)
				permissions.add((String) s);

		HashMap<String, String> customFields = new HashMap<String, String>();

		if (record.containsField(FIELD_USERDATA)) {
			BasicDBObject customData = (BasicDBObject) record.get(FIELD_USERDATA);

			for (String k : customData.keySet())
				customFields.put(k, customData.getString(k));
		}

		return new SecureUserRecord(username, passwordHashBase64, passwordSaltBase64, hasher, roles, permissions, customFields);
	}

	@Override
	public List<SecureUserRecord> getUserRecords(String survey_id, String role) throws DataStoreException {
		DBCollection col = getUsersCollection(survey_id);

		DBObject q = new BasicDBObject(FIELD_ROLES, role);

		DBCursor cursor = col.find(q);

		try {
			ArrayList<SecureUserRecord> result = new ArrayList<SecureUserRecord>();

			for (DBObject obj : cursor)
				result.add(parseUserRecord((BasicDBObject) obj));

			return result;
		} catch (MongoException e) {
			throw new DataStoreException(e);
		} finally {
			cursor.close();
		}
	}

	@Override
	public Option<SecureUserRecord> getUserRecord(String survey_id, String username) throws DataStoreException {
		DBCollection col = getUsersCollection(survey_id);

		DBObject q = new BasicDBObject(FIELD_USERNAME, username);

		DBCursor result = col.find(q);

		try {
			if (result.size() == 0)
				return Option.none();
			else {
				BasicDBObject userData = (BasicDBObject) result.next();
				return Option.some(parseUserRecord(userData));
			}
		} catch (MongoException e) {
			throw new DataStoreException(e);
		} finally {
			result.close();
		}
	}

	@Override
	public Map<String, Integer> getPopularityCount(Set<String> foodCodes) throws DataStoreException {
		DBCollection col = getPopularityCollection();
		DBObject query = new BasicDBObject(FIELD_CODE, new BasicDBObject("$in", foodCodes));

		Set<String> missing = new HashSet<String>();
		missing.addAll(foodCodes);

		DBCursor cursor = col.find(query);
		try {
			Map<String, Integer> result = new HashMap<String, Integer>();

			for (DBObject o : cursor) {
				String code = (String) o.get(FIELD_CODE);
				Integer count = (Integer) o.get(FIELD_COUNT);
				missing.remove(code);

				result.put(code, count);
			}

			for (String code : missing) {
				result.put(code, 0);
			}

			return result;
		} catch (MongoException e) {
			throw new DataStoreException(e);
		} finally {
			cursor.close();
		}
	}

	@Override
	public void incrementPopularityCount(List<String> foodCodes) throws DataStoreException {
		DBCollection col = getPopularityCollection();

		Set<String> missing = new HashSet<String>();
		missing.addAll(foodCodes);

		// FIXME
		// This query is incorrect: foods may appear multiple times if
		// foodCodes,
		// but the counter will only be incremented once, although this is
		// efficient.
		// Change this to do that correctly without generating a request for
		// each
		// code.
		DBObject query = new BasicDBObject(FIELD_CODE, new BasicDBObject("$in", foodCodes));

		try {
			DBCursor cursor = col.find(query);

			for (DBObject o : cursor)
				missing.remove(o.get(FIELD_CODE));

			List<DBObject> newRecords = new ArrayList<DBObject>();

			for (String code : missing)
				newRecords.add(new BasicDBObject(FIELD_CODE, code).append(FIELD_COUNT, 0));

			col.insert(newRecords);

			col.update(query, new BasicDBObject("$inc", new BasicDBObject(FIELD_COUNT, 1)), false, true);
		} catch (MongoException e) {
			throw new DataStoreException(e);
		}
	}

	@Override
	public void saveSurvey(String survey_id, String username, NutritionMappedSurveyRecord survey) throws DataStoreException {
		DBCollection col = getSurveysCollection(survey_id);

		try {
			col.insert(serialiser.serialize(survey, username));
		} catch (MongoException e) {
			throw new DataStoreException(e);
		}
	}

	@Override
	public void initSurvey(String survey_id, String scheme_name, String locale, boolean allowGenUsers, Option<String> surveyMonkeyUrl)
			throws DataStoreException {
		final String usersColName = USERS_COL_PREFIX + survey_id;
		final String surveysCollectionName = SURVEYS_COL_PREFIX + survey_id;

		if (db.collectionExists(usersColName) || db.collectionExists(surveysCollectionName))
			throw new DataStoreException("Database collections for " + survey_id
					+ " already exist! Survey directories and database are probably de-synchronised.");

		db.createCollection(usersColName, new BasicDBObject("capped", false));
		db.createCollection(surveysCollectionName, new BasicDBObject("capped", false));

		setSurveyParameters(survey_id, new SurveyParameters(SurveyState.NOT_INITIALISED, 0, 0, scheme_name, locale, allowGenUsers, "",
				surveyMonkeyUrl));
	}

	@Override
	public Map<String, String> getUserData(String survey_id, String user_id) throws DataStoreException {
		DBCollection usersCollection = getUsersCollection(survey_id);

		DBCursor find = usersCollection.find(new BasicDBObject(FIELD_USERNAME, user_id));

		if (!find.hasNext())
			throw new DataStoreException("No user record for " + user_id + " in " + survey_id);

		DBObject userData = (DBObject) find.next().get(FIELD_USERDATA);

		HashMap<String, String> result = new HashMap<String, String>();

		for (String k : userData.keySet())
			result.put(k, (String) userData.get(k));

		return result;
	}

	@Override
	public void setUserData(String survey_id, String user_id, Map<String, String> userData) throws DataStoreException {
		DBCollection usersCollection = getUsersCollection(survey_id);

		BasicDBObject newUserData = new BasicDBObject();

		for (String k : userData.keySet())
			newUserData.append(k, userData.get(k));

		usersCollection.update(new BasicDBObject(FIELD_USERNAME, user_id), new BasicDBObject("$set", new BasicDBObject(FIELD_USERDATA, newUserData)));
	}

	@Override
	public void setGlobalValue(String name, String value) throws DataStoreException {
		DBCollection col = getGlobalValuesCollection();

		BasicDBObject query = new BasicDBObject("name", name);
		BasicDBObject update = new BasicDBObject("$set", new BasicDBObject("value", value));

		try {
			col.update(query, update, true, false);
		} catch (MongoException e) {
			throw new DataStoreException(e);
		}
	}

	@Override
	public Option<String> getGlobalValue(String name) throws DataStoreException {
		DBCollection col = getGlobalValuesCollection();

		BasicDBObject query = new BasicDBObject("name", name);

		try {
			DBCursor val = col.find(query);
			if (val.hasNext())
				return Option.some((String) val.next().get("value"));
			else
				return Option.none();
		} catch (MongoException e) {
			throw new DataStoreException(e);
		}
	}

	@Override
	public void saveMissingFoods(List<MissingFoodRecord> missingFoods) throws DataStoreException {
		DBCollection col = getMissingFoodsCollection();

		try {
			col.insert(serialiser.missingFoodsAsDBObject(missingFoods));
		} catch (MongoException e) {
			throw new DataStoreException(e);
		}

	}

	@Override
	public void processMissingFoods(long timeFrom, long timeTo, Callback1<MissingFoodRecord> processMissingFood) throws DataStoreException {
		DBCollection col = getMissingFoodsCollection();
		DBObject query = new BasicDBObject("submitted_at", new BasicDBObject("$gte", timeFrom).append("$lt", timeTo));
		DBCursor cursor = col.find(query);
		try {
			for (DBObject o : cursor) {
				processMissingFood.call(deserialiser.deserializeMissingFood(o));
			}
		} catch (MongoException e) {
			throw new DataStoreException(e);
		} finally {
			cursor.close();
		}
	}

	@Override
	public List<SupportStaffRecord> getSupportStaffRecords() throws DataStoreException {
		DBCollection col = getSupportStaffCollection();

		DBCursor cursor = col.find();

		ArrayList<SupportStaffRecord> result = new ArrayList<SupportStaffRecord>();

		try {
			for (DBObject o : cursor) {

				String name = (String) o.get("name");
				Option<String> phoneNumber = Option.fromNullable((String) o.get("phoneNumber"));
				Option<String> email = Option.fromNullable((String) o.get("email"));

				result.add(new SupportStaffRecord(name, phoneNumber, email));
			}

			return result;
		} catch (MongoException e) {
			throw new DataStoreException(e);
		} finally {
			cursor.close();
		}
	}

	@Override
	public Option<Long> getLastHelpRequestTime(String survey, String username) throws DataStoreException {
		DBCollection col = getHelpRequestTimeCollection();

		DBObject query = new BasicDBObject("user_id", survey + "/" + username);
		DBCursor cursor = col.find(query);

		try {
			if (!cursor.hasNext())
				return Option.none();
			else {
				DBObject obj = cursor.next();
				return Option.some((long) obj.get("last_help_request_at"));
			}
		} catch (Throwable e) {
			throw new DataStoreException(e);
		} finally {
			cursor.close();
		}
	}

	@Override
	public void setLastHelpRequestTime(String survey, String username, long time) throws DataStoreException {
		DBCollection col = getHelpRequestTimeCollection();

		BasicDBObject query = new BasicDBObject("user_id", survey + "/" + username);
		BasicDBObject update = new BasicDBObject("$set", new BasicDBObject("last_help_request_at", time));

		try {
			col.update(query, update, true, false);
		} catch (MongoException e) {
			throw new DataStoreException(e);
		}
	}

	public List<String> getSurveyNames() throws DataStoreException {
		DBCollection col = getSurveyStateCollection();

		DBCursor cursor = col.find();

		List<String> result = new ArrayList<String>();

		try {
			while (cursor.hasNext()) {
				DBObject obj = cursor.next();
				result.add((String) obj.get("surveyId"));
			}
			return result;
		} catch (Throwable e) {
			throw new DataStoreException(e);
		} finally {
			cursor.close();
		}
	}

	@Override
	public List<SecureUserRecord> getUserRecords(String survey_id) throws DataStoreException {
		DBCollection col = getUsersCollection(survey_id);
		DBCursor cursor = col.find();

		try {
			ArrayList<SecureUserRecord> result = new ArrayList<SecureUserRecord>();

			for (DBObject obj : cursor)
				result.add(parseUserRecord((BasicDBObject) obj));

			return result;
		} catch (MongoException e) {
			throw new DataStoreException(e);
		} finally {
			cursor.close();
		}
	}

	@Override
	public String generateCompletionCode(String survey, String username, String externalUserName) throws DataStoreException {
		throw new DataStoreException("Not supported by this backend");
	}

	@Override
	public boolean validateCompletionCode(String survey, String externalUserName, String code) throws DataStoreException {
		throw new DataStoreException("Not supported by this backend");
	}

  @Override
  public List<LocalNutrientType> getLocalNutrientTypes(String locale) throws DataStoreException {
    throw new DataStoreException("Operation not implemented");
  }
}