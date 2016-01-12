/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client;

import java.util.ArrayList;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class LogRecorder {
	final public ArrayList<String> log = new ArrayList<String>();

	public LogRecorder() {
		
		final Logger rootLogger = Logger.getLogger("");

		rootLogger.addHandler(new Handler() {
			@Override
			public void publish(LogRecord rec) {
				log.add(rec.getLoggerName() + " " + rec.getMillis() + " " + rec.getMessage());
			}

			@Override
			public void close() {
			}

			@Override
			public void flush() {
			}
		});
	}
}
