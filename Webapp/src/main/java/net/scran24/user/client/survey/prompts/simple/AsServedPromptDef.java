/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey.prompts.simple;

import org.workcraft.gwt.imagechooser.shared.ImageDef;

import com.google.gwt.safehtml.shared.SafeHtml;

public class AsServedPromptDef {
	public final SafeHtml promptText;
	public final ImageDef[] images;
	public final String nextLabel;
	public final String prevLabel;
	public final String acceptLabel;

	public AsServedPromptDef(SafeHtml promptText, ImageDef[] images, String nextLabel,
			String prevLabel, String acceptLabel) {
		this.promptText = promptText;
		this.images = images;
		this.nextLabel = nextLabel;
		this.prevLabel = prevLabel;
		this.acceptLabel = acceptLabel;
	}
}
