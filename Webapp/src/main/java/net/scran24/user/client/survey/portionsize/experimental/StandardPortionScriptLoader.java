/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey.portionsize.experimental;

import java.util.ArrayList;
import java.util.List;

import org.pcollections.client.PMap;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class StandardPortionScriptLoader implements PortionSizeScriptLoader {
	@Override
	public void loadResources(final PMap<String, String> data, final AsyncCallback<PortionSizeScript> onComplete) {
		int unitCount = Integer.parseInt(data.get("units-count"));

		final List<StandardUnitDef> units = new ArrayList<StandardUnitDef>();

		for (int i = 0; i < unitCount; i++) {
			final String name = data.get("unit" + i + "-name");
			final boolean omit = data.get("unit" + i + "-omit-food-description").equals("true");
			final double weight = Double.parseDouble(data.get("unit" + i + "-weight"));

			units.add(new StandardUnitDef(name, omit, weight));
		}

		Scheduler.get().scheduleDeferred(new ScheduledCommand() {
			public void execute() {
				onComplete.onSuccess(new StandardPortionScript(units));
			}
		});
	}
}
