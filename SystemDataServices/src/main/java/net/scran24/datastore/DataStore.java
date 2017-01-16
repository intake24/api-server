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

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.workcraft.gwt.shared.client.Callback1;
import org.workcraft.gwt.shared.client.Option;

import net.scran24.datastore.shared.SurveyParameters;

public interface DataStore {
  public void initSurvey(String survey_id, String scheme_name, String locale, boolean allowGenUsers, Option<String> surveyMonkeyUrl,
      String supportEmail) throws DataStoreException;

  public List<String> getSurveyNames() throws DataStoreException;

  public Map<String, String> getUserData(String survey_id, String user_id) throws DataStoreException;

  public void setUserData(String survey_id, String user_id, Map<String, String> userData) throws DataStoreException;

  public void saveUsers(String survey_id, List<SecureUserRecord> users) throws DataStoreException, DuplicateKeyException;

  public void addUser(String survey_id, SecureUserRecord user) throws DataStoreException, DuplicateKeyException;

  public Option<SecureUserRecord> getUserRecord(String survey_id, String username) throws DataStoreException;

  public List<SecureUserRecord> getUserRecords(String survey_id, String role) throws DataStoreException;

  public List<SecureUserRecord> getUserRecords(String survey_id) throws DataStoreException;

  public void saveSurvey(String survey_id, String username, NutritionMappedSurveyRecord survey) throws DataStoreException;

  public void saveMissingFoods(List<MissingFoodRecord> missingFoods) throws DataStoreException;

  public void processMissingFoods(long timeFrom, long timeTo, Callback1<MissingFoodRecord> processMissingFood) throws DataStoreException;

  /**
   * Applies a side-effectful function to each of the surveys in the given
   * range. The callback is to avoid keeping all surveys in memory at once (can
   * be a large data set).
   * 
   * @throws IOException
   */
  public void processSurveys(String survey_id, long timeFrom, long timeTo, Callback1<NutritionMappedSurveyRecordWithId> processSurvey)
      throws DataStoreException;

  public SurveyParameters getSurveyParameters(String survey_id) throws DataStoreException;

  public void setSurveyParameters(String survey_id, SurveyParameters newParameters) throws DataStoreException;

  /**
   * For each of the given food codes, the popularity counter must be returned
   * from the backing data store. For food codes that do not yet have a record
   * created via <i>incrementPopularityCount</i> zero must be returned.
   * 
   * <b>The resulting map MUST have entries for each of the supplied food
   * codes.</b>
   */
  public Map<String, Integer> getPopularityCount(Set<String> foodCodes) throws DataStoreException;

  /**
   * For each of the given food codes, a "popularity" counter (the number of
   * times that food was chosen by users) must be incremented by 1 (note that
   * same food codes may appear more than once). If there is no record yet for a
   * given code, a new record must be created with an initial value of 0 and
   * immediately incremented by 1.
   */
  public void incrementPopularityCount(List<String> foodCodes) throws DataStoreException;

  public void setGlobalValue(String name, String value) throws DataStoreException;

  public Option<String> getGlobalValue(String name) throws DataStoreException;

  public List<SupportUserRecord> getSupportUserRecords(String surveyId) throws DataStoreException;

  public Option<Long> getLastHelpRequestTime(String survey, String username) throws DataStoreException;

  public void setLastHelpRequestTime(String survey, String username, long time) throws DataStoreException;

  public List<LocalNutrientType> getLocalNutrientTypes(String locale) throws DataStoreException;

  public String generateCompletionCode(String survey, String username, String externalUserName) throws DataStoreException;

  public boolean validateCompletionCode(String survey, String externalUserName, String code) throws DataStoreException;

  public String getSurveySupportEmail(String surveyId) throws DataStoreException;
}
