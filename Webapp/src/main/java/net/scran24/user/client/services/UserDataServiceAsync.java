/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.services;

import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

public interface UserDataServiceAsync
{
    void submit( Map<String, String> data, AsyncCallback<Void> callback );

    public static final class Util 
    { 
        private static UserDataServiceAsync instance;

        public static final UserDataServiceAsync getInstance()
        {
            if ( instance == null )
            {
                instance = (UserDataServiceAsync) GWT.create( UserDataService.class );
            }
            return instance;
        }

        private Util(){}
    }
}
