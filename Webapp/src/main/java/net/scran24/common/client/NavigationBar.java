/*
This file is part of Intake24.

Copyright 2015, 2016 Newcastle University.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

This file is based on Intake24 v1.0.

© Crown copyright, 2012, 2013, 2014

Licensed under the Open Government Licence 3.0: 

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.common.client;

import java.util.List;

import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;

public class NavigationBar extends Composite {
	public NavigationBar(Anchor ... links) {
		UnorderedList<Anchor> contents = new UnorderedList<Anchor>();
		
		for (Anchor a: links) {
			contents.addItem(a);
		}
		
		initWidget(contents);
	}
	
	public NavigationBar(List<Anchor> additionalLinks, Anchor ... links) {
		UnorderedList<Anchor> contents = new UnorderedList<Anchor>();
		
		for (Anchor a: additionalLinks) {
			contents.addItem(a);
		}
		
		for (Anchor a: links) {
			contents.addItem(a);
		}
		
		initWidget(contents);		
	}
}
