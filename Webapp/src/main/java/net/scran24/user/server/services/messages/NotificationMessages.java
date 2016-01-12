/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.server.services.messages;

import com.google.gwt.i18n.client.Messages;

public interface NotificationMessages extends Messages {
	String callRequest_emailSubject();
	String callRequest_emailBody(String name, String phoneNumber);
}
