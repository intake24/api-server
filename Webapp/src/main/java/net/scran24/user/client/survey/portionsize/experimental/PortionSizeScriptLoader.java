/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey.portionsize.experimental;

import org.pcollections.client.PMap;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * Responsible for fetching the resources required to display the corresponding portion size prompt sequence from the server.
 */
public interface PortionSizeScriptLoader {
	/**
	 * @param data The initial parameters for the portion size script.
	 * @param onComplete Called when all the necessary resources have been loaded.
	 */
	public void loadResources(final PMap<String, String> data, final AsyncCallback<PortionSizeScript> onComplete);
}
