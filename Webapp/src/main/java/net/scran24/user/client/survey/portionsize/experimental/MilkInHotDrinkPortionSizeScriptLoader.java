/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey.portionsize.experimental;

import org.pcollections.client.PMap;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class MilkInHotDrinkPortionSizeScriptLoader implements PortionSizeScriptLoader {

	@Override
	public void loadResources(final PMap<String, String> data, final AsyncCallback<PortionSizeScript> onComplete) {
		Scheduler.get().scheduleDeferred(new ScheduledCommand() {
			@Override
			public void execute() {
				onComplete.onSuccess(new MilkInHotDrinkPortionSizeScript());
			}
		});
	}
}