/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey.prompts;

import com.google.gwt.safehtml.shared.SafeHtml;

import net.scran24.user.client.survey.flat.FoodOperation;

import org.workcraft.gwt.shared.client.Function1;

public class TextBoxPromptDef {
	public final SafeHtml description;
	public final Function1<String, FoodOperation> f;

	public TextBoxPromptDef(SafeHtml description, Function1<String, FoodOperation> f) {
		this.description = description;
		this.f = f;
	}
}
