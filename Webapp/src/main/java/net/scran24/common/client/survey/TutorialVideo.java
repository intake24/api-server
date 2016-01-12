/*
This file is part of Intake24.

Copyright 2015, 2016 Newcastle University.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

This file is based on Intake24 v1.0.

© Crown copyright, 2012, 2013, 2014

Licensed under the Open Government Licence 3.0: 

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.common.client.survey;

import net.scran24.common.client.CommonMessages;


public class TutorialVideo {
	private static final CommonMessages commonMessages = CommonMessages.Util.getInstance();	
	public static final String url = commonMessages.walkthroughYouTubeUrl();
	public static final String embedUrl = commonMessages.walkthroughYouTubeEmbedUrl();
	public static final String embedHTML = "<iframe width=\"560\" height=\"315\" src=\""+ embedUrl +"\" frameborder=\"0\" allowfullscreen></iframe>";
}
