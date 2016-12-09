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

package net.scran24.user.client.services;

import java.util.List;

import org.workcraft.gwt.imagemap.shared.ImageMapDefinition;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

import net.scran24.user.shared.FoodData;
import net.scran24.user.shared.FoodPrompt;
import net.scran24.user.shared.lookup.AsServedDef;
import net.scran24.user.shared.lookup.DrinkwareDef;
import net.scran24.user.shared.lookup.GuideDef;
import net.scran24.user.shared.lookup.LookupResult;
import net.scran24.user.shared.lookup.PortionSizeMethod;

public interface FoodLookupServiceAsync {
  void split(java.lang.String description, String locale, AsyncCallback<java.util.List<java.lang.String>> callback);

  void lookup(java.lang.String description, String locale, int maxResults, AsyncCallback<net.scran24.user.shared.lookup.LookupResult> callback);

  void lookupInCategory(String description, String categoryCode, String locale, int maxResults, AsyncCallback<LookupResult> callback);

  void getRootCategories(String locale, AsyncCallback<java.util.List<net.scran24.user.shared.CategoryHeader>> callback);

  void browseCategory(String code, String locale, AsyncCallback<LookupResult> callback);

  void getAsServedDef(java.lang.String asServedSet, String locale, AsyncCallback<AsServedDef> callback);

  void getMultipleAsServedDefs(List<String> ids, String locale, AsyncCallback<List<AsServedDef>> callback);

  void getGuideDef(String guideId, String locale, AsyncCallback<GuideDef> callback);

  void getDrinkwareDef(String drinkwareId, String locale, AsyncCallback<DrinkwareDef> callback);

  void getFoodPrompts(String foodCode, String locale, AsyncCallback<List<FoodPrompt>> callback);

  void getFoodData(String foodCode, String locale, AsyncCallback<FoodData> callback);

  void getWeightPortionSizeMethod(AsyncCallback<PortionSizeMethod> callback);

  void getImageMap(String id, AsyncCallback<ImageMapDefinition> callback);

  void getImageMaps(List<String> ids, AsyncCallback<List<ImageMapDefinition>> asyncCallback);

  public static final class Util {
    private static FoodLookupServiceAsync instance;

    public static final FoodLookupServiceAsync getInstance() {
      if (instance == null) {
        instance = (FoodLookupServiceAsync) GWT.create(FoodLookupService.class);
      }
      return instance;
    }

    private Util() {
    }
  }

}
