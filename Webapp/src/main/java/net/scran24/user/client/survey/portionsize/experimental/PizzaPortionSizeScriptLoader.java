/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey.portionsize.experimental;

import java.util.ArrayList;
import java.util.List;

import org.pcollections.client.HashTreePMap;
import org.pcollections.client.PMap;
import org.workcraft.gwt.imagemap.client.ImageMapServiceAsync;
import org.workcraft.gwt.imagemap.shared.ImageMapDefinition;

import com.google.gwt.user.client.rpc.AsyncCallback;

public class PizzaPortionSizeScriptLoader implements PortionSizeScriptLoader {
	public static final String pizzaImageMapName = "gpizza";
	public static final String pizzaThicknessImageMapName = "gpthick";
	public static final String sliceImageMapPrefix = "gpiz";
	public static final int pizzaTypesCount = 9;

	private final ImageMapServiceAsync imageMapService = ImageMapServiceAsync.Util.getInstance();

	@Override
	public void loadResources(final PMap<String, String> data, final AsyncCallback<PortionSizeScript> onComplete) {
		ArrayList<String> imageMapNames = new ArrayList<String>();

		for (int i = 0; i < pizzaTypesCount; i++)
			imageMapNames.add(sliceImageMapPrefix + Integer.toString(i + 1));

		imageMapNames.add(pizzaImageMapName);
		imageMapNames.add(pizzaThicknessImageMapName);

		imageMapService.getMultipleImageMaps(imageMapNames, new AsyncCallback<List<ImageMapDefinition>>() {
			@Override
			public void onFailure(Throwable caught) {
				onComplete.onFailure(caught);
			}

			@Override
			public void onSuccess(List<ImageMapDefinition> result) {
				PMap<Integer, ImageMapDefinition> sliceMaps = HashTreePMap.<Integer, ImageMapDefinition> empty();

				for (int i = 0; i < pizzaTypesCount; i++)
					sliceMaps = sliceMaps.plus(i + 1, result.get(i));

				onComplete.onSuccess(new PizzaPortionSizeScript(result.get(pizzaTypesCount), result.get(pizzaTypesCount + 1), sliceMaps));
			}
		});
	}
}
