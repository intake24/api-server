/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey.portionsize.experimental;

import net.scran24.user.client.survey.flat.SimplePrompt;
import net.scran24.user.shared.FoodData;

import org.pcollections.client.PMap;
import org.workcraft.gwt.shared.client.Option;

/**
 * Generates prompts for portion size estimation.
 */
public interface PortionSizeScript {
	public Option<SimplePrompt<UpdateFunc>> nextPrompt(PMap<String, String> data, FoodData foodData);
}
