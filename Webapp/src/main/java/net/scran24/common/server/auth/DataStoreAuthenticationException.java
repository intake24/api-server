/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.common.server.auth;

import net.scran24.datastore.DataStoreException;

import org.apache.shiro.authc.AuthenticationException;

/**
 * Indicates that an exception occured while trying to access accounts
 * in the backing Scran24 datastore.
 */
public class DataStoreAuthenticationException extends AuthenticationException {
	public DataStoreAuthenticationException(DataStoreException e) {
		super(e);
	}

	private static final long serialVersionUID = -5800689610327164080L;

}
