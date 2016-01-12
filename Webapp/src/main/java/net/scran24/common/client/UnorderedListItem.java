/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.common.client;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

public class UnorderedListItem<T extends Widget> extends SimplePanel {

	public final T item;

	public UnorderedListItem(T w) {
		super((Element) Document.get().createLIElement().cast());
		this.add(w);
		item = w;
	}
}
