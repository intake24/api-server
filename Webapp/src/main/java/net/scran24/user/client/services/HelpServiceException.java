/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.services;

public class HelpServiceException extends RuntimeException {
	private static final long serialVersionUID = -7951238686902702173L;

	public HelpServiceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public HelpServiceException(String message, Throwable cause) {
		super(message, cause);
	}

	public HelpServiceException(String message) {
		super(message);
	}

	public HelpServiceException(Throwable cause) {
		super(cause);
	}
}
