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

import net.scran24.user.client.survey.CompoundFoodTemplateManager;
import net.scran24.user.client.survey.portionsize.experimental.PortionSizeScriptManager;
import net.scran24.user.shared.EncodedFood;
import net.scran24.user.shared.FoodEntry;
import net.scran24.user.shared.Meal;

import org.pcollections.client.PVector;
import org.pcollections.client.TreePVector;
import org.workcraft.gwt.shared.client.Option;

import com.google.gwt.storage.client.Storage;

public class StateManagerUtil {
	final public static String LatestStateKeyPrefix = "scran24-survey-state-";
	final public static String HistoryStateKeyPrefix = "scran24-history-";
	final public static String SameAsBeforePrefix = "scran24-sab-";
	
	final public static Storage localStorage = Storage.getLocalStorageIfSupported();
	final public static Storage sessionStorage = Storage.getSessionStorageIfSupported();
		
	final public static Map<Integer, Survey> history = new TreeMap<Integer, Survey>();
	
	public static void setItem(String key, String data) {
		localStorage.setItem(key, data);
	}
	
	public static Option<String> getItem(String key) {
		String data = localStorage.getItem(key);
		
		if (data!=null)
			return Option.some(data);
		else
			return Option.none();
	}
	
	public static String latestStateKey (String userName) {
		return LatestStateKeyPrefix + userName;
	}
	
	public static String historyKey (String userName, int event) {
		return HistoryStateKeyPrefix + userName + "-" + Integer.toString(event);		
	}
	
	public static void clearLatestState(String userName) {
		localStorage.removeItem(latestStateKey(userName));
	}
	
	public static void setLatestState(String userName, Survey survey) {
		saveState(localStorage, latestStateKey(userName), survey);
	}
	
	public static Option<Survey> getLatestState(String userName, PortionSizeScriptManager scriptManager, CompoundFoodTemplateManager templateManager) {
		return getSavedState(localStorage, latestStateKey(userName), scriptManager, templateManager);
	}
	
	public static void saveSameAsBefore (String userName, Meal meal, FoodEntry food) {
		if (!food.isEncoded())
			throw new IllegalStateException ("Only non-compound foods are supported by same-as-before at this time");
		
		EncodedFood mainFood = food.asEncoded();
		
		final String key = SameAsBeforePrefix + userName + "-" + mainFood.data.code; 
		
		PVector<FoodEntry> foods = 
				TreePVector.<FoodEntry>empty()
				.plus(mainFood)
				.plusAll(Meal.linkedFoods(meal.foods, mainFood));
		
		localStorage.setItem(key, SurveyXmlSerialiser.foodsToXml(foods));
	}
	
	public static Option<PVector<FoodEntry>> getSameAsBefore(String userName, String foodCode, PortionSizeScriptManager scriptManager, CompoundFoodTemplateManager templateManager) {
		final String key = SameAsBeforePrefix + userName + "-" + foodCode;
		
		final String xml = localStorage.getItem(key);
		
		if (xml == null)
			return Option.none();
		else
			try {
				return Option.some(SurveyXmlSerialiser.foodsFromXml(scriptManager, templateManager, xml));
			} catch (VersionMismatchException e) {
				return Option.none();
			}
	}
	
	public static void setHistoryState(String userName, int state_id, Survey survey) {
		//saveState(sessionStorage, historyKey(userName, state_id), survey);
		
		history.put(state_id, survey);
	}
	
	public static Option<Survey> getHistoryState (String userName, int state_id, PortionSizeScriptManager scriptManager) {
		if (history.containsKey(state_id))
			return Option.some(history.get(state_id));
		else
			return Option.none();
		//return getSavedState(sessionStorage, historyKey(userName, state_id), scriptManager);
	}

	public static void saveState(Storage storage, String key, Survey survey) {
		//Logger log = Logger.getLogger("StateManager");
		String xml = SurveyXmlSerialiser.toXml(survey);
		storage.setItem(key, xml);
		//log.info("Saved data for key \"" + key + "\":" + xml);
	}

	public static Option<Survey> getSavedState(Storage storage, String key, PortionSizeScriptManager scriptManager, CompoundFoodTemplateManager templateManager) {
		//Logger log = Logger.getLogger("StateManager");
		String data = storage.getItem(key);
		
		//log.info("Data for key \"" + key + "\":" + data);
		
		if (data == null)
			return Option.none();
		else {
			try {
				Survey s = SurveyXmlSerialiser.fromXml(data, scriptManager, templateManager);
				return Option.some(s);
			} catch (Throwable e) {
				e.printStackTrace();
				Logger.getLogger("StateManager").info("Failed to parse saved state: " + e.getMessage() +"\n\n" + data);
				return Option.none();
			}
		}
	}
}
