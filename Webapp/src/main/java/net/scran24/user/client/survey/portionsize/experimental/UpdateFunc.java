/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey.portionsize.experimental;

import java.util.Arrays;

import org.pcollections.client.PMap;
import org.workcraft.gwt.shared.client.Function1;

public class UpdateFunc implements Function1<PMap<String, String>, PMap<String, String>> {
	public UpdateFunc setField(final String key, final String value) {
		final UpdateFunc outer = this;
		
		return new UpdateFunc() {
			@Override
			public PMap<String, String> apply(PMap<String, String> argument) {
				return outer.apply(argument).plus(key, value) ;
			}
		};
	}
	
	public UpdateFunc deleteField(final String key) {
		final UpdateFunc outer = this;
		
		return new UpdateFunc() {
			@Override
			public PMap<String, String> apply(PMap<String, String> argument) {
				return outer.apply(argument).minus(key);
			}
		};
	}
	
	public UpdateFunc deleteFields(final String[] keys) {
		final UpdateFunc outer = this;
		
		return new UpdateFunc() {
			@Override
			public PMap<String, String> apply(PMap<String, String> argument) {
				return outer.apply(argument).minusAll(Arrays.asList(keys));
			}
		};
	}
	
	public UpdateFunc compose(final UpdateFunc g) {
		final UpdateFunc outer = this;

		return new UpdateFunc() {
			@Override
			public PMap<String, String> apply(PMap<String, String> argument) {
				return g.apply(outer.apply(argument));
			}
		};
	}

	@Override
	public PMap<String, String> apply(PMap<String, String> argument) {
		return argument;
	}
};