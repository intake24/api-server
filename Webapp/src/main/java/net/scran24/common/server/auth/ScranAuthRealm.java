/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.common.server.auth;

import net.scran24.datastore.DataStore;
import net.scran24.datastore.DataStoreException;
import net.scran24.datastore.SecureUserRecord;

import org.apache.shiro.authc.AccountException;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.codec.Base64;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.util.ByteSource;
import org.workcraft.gwt.shared.client.Option;

public class ScranAuthRealm extends AuthorizingRealm {
	private final DataStore dataStore;

	public ScranAuthRealm(DataStore dataStore) {
		this.dataStore = dataStore;
	}

	@Override
	protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
		if (principals == null) {
			throw new AuthorizationException("PrincipalCollection method argument cannot be null.");
		}

		ScranUserId userid = (ScranUserId) getAvailablePrincipal(principals);
		
		try {
			Option<SecureUserRecord> userRecord = dataStore.getUserRecord(userid.survey, userid.username);
			if (userRecord.isEmpty()) {
				throw new UnknownAccountException("No account found for user [" + userid.username + "] using collection " + "users_" + userid.survey);
			} else {
				SecureUserRecord record = userRecord.getOrDie();
				SimpleAuthorizationInfo info = new SimpleAuthorizationInfo(record.roles);
				info.setStringPermissions(record.permissions);
				return info;
			}
		} catch (DataStoreException e) {
			throw new UnknownAccountException(e);
		}
	}

	@Override
	protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
		ScranAuthToken tk = (ScranAuthToken) token;
		String username = tk.getUsername();

		// Null username is invalid
		if (username == null) {
			throw new AccountException("Null usernames are not allowed by this realm.");
		}
		
		try {
			Option<SecureUserRecord> userRecord = dataStore.getUserRecord(tk.survey, username);
			if (userRecord.isEmpty()) {
				throw new UnknownAccountException("No account found for user [" + username + "] using collection " + "users_" + tk.survey);
			} else {
				SecureUserRecord record = userRecord.getOrDie();
				SimpleAuthenticationInfo info = new SimpleAuthenticationInfo(new ScranUserId(username, tk.survey), record.passwordHashBase64.toCharArray(), getName());
				info.setCredentialsSalt(ByteSource.Util.bytes((Base64.decode(record.passwordSaltBase64))));
				return info;
			}
		} catch (DataStoreException e) {
			throw new DataStoreAuthenticationException(e);
		}
	}
}
