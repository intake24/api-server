/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.staff.client;

import net.scran24.datastore.shared.SurveyParameters;
import net.scran24.datastore.shared.SurveyState;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;

public interface SurveyControlServiceAsync
{
    public static final class Util 
    { 
        private static SurveyControlServiceAsync instance;

        public static final SurveyControlServiceAsync getInstance(String surveyId)
        {
            if ( instance == null )
            {
                instance = (SurveyControlServiceAsync) GWT.create( SurveyControlService.class );
                ServiceDefTarget target = (ServiceDefTarget) instance;
                target.setServiceEntryPoint( GWT.getModuleBaseURL() + "../staff/surveyControl?surveyId=" + surveyId );
            }
            return instance;
        }

        private Util() {}
    }

	void getParameters(AsyncCallback<SurveyParameters> callback);

	void setParameters(SurveyParameters state, AsyncCallback<Void> callback);
}
