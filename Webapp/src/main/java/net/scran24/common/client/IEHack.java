/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.common.client;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Document;

public class IEHack {
	public static void forceReflow() {
		Document.get().getBody().setClassName(Document.get().getBody().getClassName());
	}
	
	public static void forceReflowDeferred() {
		Scheduler.get().scheduleDeferred(new ScheduledCommand() {
			public void execute() {
				forceReflow();
			}
		});
	}
}
