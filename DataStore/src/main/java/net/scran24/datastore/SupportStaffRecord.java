/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.datastore;

import org.workcraft.gwt.shared.client.Option;

public class SupportStaffRecord {
	final public String name;
	final public Option<String> phoneNumber;
	final public Option<String> email;

	public SupportStaffRecord(String name, Option<String> phoneNumber, Option<String> email) {
		this.name = name;
		this.phoneNumber = phoneNumber;
		this.email = email;
	}
}
