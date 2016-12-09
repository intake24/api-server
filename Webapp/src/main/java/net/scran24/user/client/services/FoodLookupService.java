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

import net.scran24.user.shared.CategoryHeader;
import net.scran24.user.shared.FoodData;
import net.scran24.user.shared.FoodPrompt;
import net.scran24.user.shared.lookup.AsServedDef;
import net.scran24.user.shared.lookup.DrinkwareDef;
import net.scran24.user.shared.lookup.GuideDef;
import net.scran24.user.shared.lookup.LookupResult;
import net.scran24.user.shared.lookup.PortionSizeMethod;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("foodLookup")
public interface FoodLookupService extends RemoteService {
  List<String> split(String description, String locale);

  LookupResult lookup(String description, String locale, int maxResults);

  LookupResult lookupInCategory(String description, String categoryCode, String locale, int maxResults);

  List<CategoryHeader> getRootCategories(String locale);

  LookupResult browseCategory(String code, String locale);

  FoodData getFoodData(String foodCode, String locale);

  AsServedDef getAsServedDef(String asServedSet, String locale);

  List<AsServedDef> getMultipleAsServedDefs(List<String> ids, String locale);

  GuideDef getGuideDef(String guideId, String locale);

  ImageMapDefinition getImageMap(String id);
  
  List<ImageMapDefinition> getImageMaps(List<String> ids);

  DrinkwareDef getDrinkwareDef(String drinkwareId, String locale);

  List<FoodPrompt> getFoodPrompts(String foodCode, String locale);

  PortionSizeMethod getWeightPortionSizeMethod();
}
