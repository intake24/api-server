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

package net.scran24.user.client.survey.portionsize.experimental;

import java.util.ArrayList;
import java.util.List;

import net.scran24.user.client.services.FoodLookupServiceAsync;
import net.scran24.user.shared.lookup.AsServedDef;

import org.pcollections.client.HashTreePMap;
import org.pcollections.client.PMap;

import org.workcraft.gwt.imagemap.shared.ImageMapDefinition;

import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class CerealPortionSizeScriptLoader implements PortionSizeScriptLoader {
  private final FoodLookupServiceAsync lookupService = FoodLookupServiceAsync.Util.getInstance();

  public static final String bowlImageMap = "gbowl";

  @Override
  public void loadResources(final PMap<String, String> data, final AsyncCallback<PortionSizeScript> onComplete) {
    final String cerealType = data.get("type");

    lookupService.getImageMap(bowlImageMap, new AsyncCallback<ImageMapDefinition>() {
      @Override
      public void onFailure(Throwable caught) {
        onComplete.onFailure(caught);
      }

      @Override
      public void onSuccess(final ImageMapDefinition imageMapDef) {
        final ArrayList<String> ids = new ArrayList<String>();

        for (String bowl : CerealPortionSizeScript.bowlCodes) {
          ids.add("cereal_" + cerealType + bowl);
          ids.add("cereal_" + cerealType + bowl + "_leftovers");
        }

        lookupService.getMultipleAsServedDefs(ids, LocaleInfo.getCurrentLocale().getLocaleName(), new AsyncCallback<List<AsServedDef>>() {
          @Override
          public void onSuccess(List<AsServedDef> result) {
            PMap<String, AsServedDef> defs = HashTreePMap.empty();

            for (int i = 0; i < ids.size(); i++)
              defs = defs.plus(ids.get(i), result.get(i));

            onComplete.onSuccess(new CerealPortionSizeScript(imageMapDef, defs));
          }

          @Override
          public void onFailure(Throwable caught) {
            onComplete.onFailure(caught);
          }
        });
      }
    });
  }
}