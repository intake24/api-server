/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey.prompts.simple;

import com.google.gwt.safehtml.shared.SafeHtml;

public class OptionalFoodPromptDef {
	public final SafeHtml promptHtml;
	public final String yesButtonText;
	public final String noButtonText;
	public final SafeHtml foodChoicePromptHtml;
	public final String categoryCode;

	public OptionalFoodPromptDef(SafeHtml promptHtml, String yesText, String noText, SafeHtml foodChoicePromptHtml, String categoryCode) {
		this.promptHtml = promptHtml;
		this.yesButtonText = yesText;
		this.noButtonText = noText;
		this.foodChoicePromptHtml = foodChoicePromptHtml;
		this.categoryCode = categoryCode;
	}
}
