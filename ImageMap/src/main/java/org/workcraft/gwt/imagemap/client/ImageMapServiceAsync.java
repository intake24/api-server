/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package org.workcraft.gwt.imagemap.client;

import java.util.List;

import org.workcraft.gwt.imagemap.shared.ImageMapDefinition;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

public interface ImageMapServiceAsync
{
    void getImageMap( java.lang.String name, AsyncCallback<org.workcraft.gwt.imagemap.shared.ImageMapDefinition> callback );
    void getMultipleImageMaps ( List<String> ids, AsyncCallback<List<ImageMapDefinition>> callback);


    /**
     * Utility class to get the RPC Async interface from client-side code
     */
    public static final class Util 
    { 
        private static ImageMapServiceAsync instance;

        public static final ImageMapServiceAsync getInstance()
        {
            if ( instance == null )
            {
                instance = (ImageMapServiceAsync) GWT.create( ImageMapService.class );
            }
            return instance;
        }

        private Util()
        {
            // Utility class should not be instanciated
        }
    }
}
