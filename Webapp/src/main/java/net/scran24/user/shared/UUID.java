/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.shared;


public class UUID {
	public final String value;
	
	public UUID(String value) {
		this.value = value;
	}
	
	private static native String randomUuidString() /*-{		
		// from http://stackoverflow.com/a/2117523
		// not very high quality, but should be good enough to identify foods 
		return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
			var r = Math.random() * 16 | 0, v = c == 'x' ? r : (r & 0x3 | 0x8);
			return v.toString(16);
		});
	}-*/;
	
	public static UUID randomUUID() {
		return new UUID(randomUuidString());
	}
	
	public static UUID fromString(String string) {
		return new UUID(string);
	}

	@Override
	public boolean equals(Object obj) {
		UUID other = (UUID) obj;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}
	
	@Override
	public String toString() {
		return value;
	}
}