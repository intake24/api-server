/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.frontpage.client.services;

import net.scran24.datastore.shared.UserRecord;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

public interface GenUserServiceAsync
{
	void autoCreateUser(String survey_name, AsyncCallback<UserRecord> callback);

    public static final class Util 
    { 
        private static GenUserServiceAsync instance;

        public static final GenUserServiceAsync getInstance()
        {
            if ( instance == null )
            {
                instance = (GenUserServiceAsync) GWT.create( GenUserService.class );
            }
            return instance;
        }

        private Util(){}
    }
}
