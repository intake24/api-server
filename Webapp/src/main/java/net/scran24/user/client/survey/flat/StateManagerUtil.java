/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
 */

package net.scran24.user.client.survey.flat;

import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import net.scran24.user.client.json.SerialisableSurveyCodec;
import net.scran24.user.client.json.serialisable.SerialisableSurvey;
import net.scran24.user.client.json.serialisable.sameasbefore.SerialisableSameAsBefore;
import net.scran24.user.client.json.serialisable.sameasbefore.SerialisableSameAsBeforeCodec;
import net.scran24.user.client.survey.CompoundFoodTemplateManager;
import net.scran24.user.client.survey.portionsize.experimental.PortionSizeScriptManager;
import net.scran24.user.shared.EncodedFood;
import net.scran24.user.shared.FoodEntry;
import net.scran24.user.shared.Meal;

import org.pcollections.client.PVector;
import org.workcraft.gwt.shared.client.Option;

import com.google.gwt.core.client.GWT;
import com.google.gwt.storage.client.Storage;

public class StateManagerUtil {
  final public static String LatestStateKeyPrefix = "scran24-survey-state-";
  final public static String HistoryStateKeyPrefix = "scran24-history-";
  final public static String SameAsBeforePrefix = "scran24-sab-";

  final public static Storage localStorage = Storage.getLocalStorageIfSupported();
  final public static Storage sessionStorage = Storage.getSessionStorageIfSupported();

  final public static Map<Integer, Survey> history = new TreeMap<Integer, Survey>();

  final public static SerialisableSameAsBeforeCodec sameAsBeforeCodec = GWT.create(SerialisableSameAsBeforeCodec.class);
  final public static SerialisableSurveyCodec surveyCodec = GWT.create(SerialisableSurveyCodec.class);

  final public static Logger log = Logger.getLogger(StateManagerUtil.class.getSimpleName());

  public static void setItem(String key, String data) {
    localStorage.setItem(key, data);
  }

  public static Option<String> getItem(String key) {
    String data = localStorage.getItem(key);

    if (data != null)
      return Option.some(data);
    else
      return Option.none();
  }

  public static String latestStateKey(String userName) {
    return LatestStateKeyPrefix + userName;
  }

  public static String historyKey(String userName, int event) {
    return HistoryStateKeyPrefix + userName + "-" + Integer.toString(event);
  }

  public static void clearLatestState(String userName) {
    localStorage.removeItem(latestStateKey(userName));
  }

  public static void setLatestState(String userName, Survey survey, String scheme_id, String version_id) {
    saveState(localStorage, latestStateKey(userName), survey, scheme_id, version_id);
  }

  public static Option<String> getLatestStateSerialised(String userName) {
    return Option.fromNullable(localStorage.getItem(localStorage.getItem(latestStateKey(userName))));
	}

  public static Option<Survey> getLatestState(String userName, String scheme_id, String version_id,
      PortionSizeScriptManager scriptManager, CompoundFoodTemplateManager templateManager) {
    return getSavedState(localStorage, latestStateKey(userName), scheme_id, version_id, scriptManager, templateManager);
  }

  public static void saveSameAsBefore(String userName, Meal meal, EncodedFood mainFood, String scheme_id,
      String version_id) {
    final String key = SameAsBeforePrefix + userName + "-" + mainFood.data.code;

    if (!mainFood.isEncoded())
      throw new IllegalArgumentException("Only non-compound foods are supported by same-as-before at this time");

    PVector<FoodEntry> linkedFoods = Meal.linkedFoods(meal.foods, mainFood);

    localStorage.setItem(key,
        sameAsBeforeCodec.encode(new SerialisableSameAsBefore(mainFood, linkedFoods, scheme_id, version_id))
          .toString());
  }

  public static Option<SameAsBefore> getSameAsBefore(String userName, String foodCode, String scheme_id,
      String version_id, PortionSizeScriptManager scriptManager, CompoundFoodTemplateManager templateManager) {
    final String key = SameAsBeforePrefix + userName + "-" + foodCode;

    final String serialised = localStorage.getItem(key);

    if (serialised == null)
      return Option.none();
    else
      try {
        SerialisableSameAsBefore decoded = sameAsBeforeCodec.decode(serialised);

        if (decoded.scheme_id != scheme_id || decoded.version_id != version_id) {
          log.warning("Version mismatch for same as before (" + foodCode + "): stored version is (" + decoded.scheme_id
              + ", " + decoded.version_id + "), runtime version is (" + scheme_id + ", " + version_id
              + "). Ignoring record.");
          return Option.none();
        } else
          return Option.some(decoded.toSameAsBefore(scriptManager, templateManager));
      } catch (Throwable e) {
        e.printStackTrace();
        log.warning("Deserialisation failed for same as before: " + e.getClass()
          .getName() + " (" + e.getMessage() + ")");
        return Option.none();
      }
  }

  public static void setHistoryState(String userName, int state_id, Survey survey) {
    // saveState(sessionStorage, historyKey(userName, state_id), survey);

    history.put(state_id, survey);
  }

  public static Option<Survey> getHistoryState(String userName, int state_id, PortionSizeScriptManager scriptManager) {
    if (history.containsKey(state_id))
      return Option.some(history.get(state_id));
    else
      return Option.none();
    // return getSavedState(sessionStorage, historyKey(userName, state_id),
    // scriptManager);
  }

  public static void saveState(Storage storage, String key, Survey survey, String scheme_id, String version_id) {
    // Logger log = Logger.getLogger("StateManager");
    String serialised = surveyCodec.encode(new SerialisableSurvey(survey, scheme_id, version_id))
      .toString();
    storage.setItem(key, serialised);
    // log.info("Saved data for key \"" + key + "\":" + xml);
  }

  public static Option<Survey> getSavedState(Storage storage, String key, String scheme_id, String version_id,
      PortionSizeScriptManager scriptManager, CompoundFoodTemplateManager templateManager) {
    // Logger log = Logger.getLogger("StateManager");
    String data = storage.getItem(key);

    // log.info("Data for key \"" + key + "\":" + data);

    if (data == null)
      return Option.none();
    else {
      try {
        SerialisableSurvey decoded = surveyCodec.decode(data);

        if (decoded.scheme_id != scheme_id || decoded.version_id != version_id) {
          log.warning("Survey version mismatch: stored version is (" + decoded.scheme_id + ", " + decoded.version_id
              + "), runtime version is (" + scheme_id + ", " + version_id + "). Ignoring stored survey.");
          return Option.none();
        } else
          return Option.some(decoded.toSurvey(scriptManager, templateManager));
      } catch (Throwable e) {
        e.printStackTrace();
        log.warning("Failed to parse saved survey state: " + e.getMessage() + "\n\n" + data);
        return Option.none();
      }
    }
  }
}
