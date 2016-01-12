/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey.flat;

import org.workcraft.gwt.shared.client.Option;

/**
 * Selects the next meal or food element to focus on based on the current survey
 * state.
 */
public interface SelectionManager {
	public Option<Selection> nextSelection(Survey state);
}
