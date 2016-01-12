/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.services;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;

public interface SurveyProcessingServiceAsync
{
    void submit( net.scran24.user.shared.CompletedSurvey survey, AsyncCallback<Void> callback );

    public static final class Util 
    { 
        private static SurveyProcessingServiceAsync instance;

        public static final SurveyProcessingServiceAsync getInstance(String surveyId)
        {
            if ( instance == null )
            {
                instance = (SurveyProcessingServiceAsync) GWT.create( SurveyProcessingService.class );
                ServiceDefTarget target = (ServiceDefTarget) instance;
                target.setServiceEntryPoint( GWT.getModuleBaseURL() + "processSurvey?surveyId=" + surveyId);
            }
            return instance;
        }

        private Util(){}
    }
}
