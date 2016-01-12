/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey.portionsize.experimental;

import java.util.Map;
import java.util.NoSuchElementException;


/**
 * Creates PortionSizeScriptLoader instances from string portion size method ids.
 */
public class PortionSizeScriptManager {
	private final Map<String, PortionSizeScriptConstructor> scriptCtors;
	
	public PortionSizeScriptManager(Map<String, PortionSizeScriptConstructor> scripts) {
		this.scriptCtors = scripts;
	}
	
	public PortionSizeScriptLoader getInstance(String name) {
		if (scriptCtors.containsKey(name))
			return scriptCtors.get(name).newInstance();
		else
			throw new NoSuchElementException("No portion size script loader for \"" + name + "\"");
	}
}
