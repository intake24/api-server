/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey.prompts.simple;


import net.scran24.user.shared.lookup.DrinkScaleDef;

import com.google.gwt.safehtml.shared.SafeHtml;

public class DrinkScalePromptDef {
	final public DrinkScaleDef scaleDef;
	final public SafeHtml message;

	final public String lessLabel;
	final public String moreLabel;
	final public String acceptLabel;

	final public double initialLevel;
	final public double limit;

	public DrinkScalePromptDef(DrinkScaleDef scaleDef, SafeHtml message, String lessLabel, String moreLabel, String acceptLabel, double limit, double initialLevel) {
		this.scaleDef = scaleDef;
		this.message = message;
		this.lessLabel = lessLabel;
		this.moreLabel = moreLabel;
		this.acceptLabel = acceptLabel;
		this.limit = limit;
		this.initialLevel = initialLevel;
	}
}
