/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey.prompts;

import org.workcraft.gwt.shared.client.Callback;

public class SkipFoodHandler {
	public final String skipButtonLabel;
	public final Callback onFoodSkipped;
	
	public SkipFoodHandler(String skipButtonLabel, Callback onFoodSkipped) {
		this.skipButtonLabel = skipButtonLabel;
		this.onFoodSkipped = onFoodSkipped;
	}		
}
