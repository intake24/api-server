/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.datastore;

import org.workcraft.gwt.shared.client.Option;

public class SupportUserRecord {
  final public String surveyId;
  final public String userId;
  final public boolean smsNotificationsEnabled;
  final public Option<String> realName;
  final public Option<String> phoneNumber;
  final public Option<String> email;

  public SupportUserRecord(String surveyId, String userId, Option<String> realName, Option<String> email, Option<String> phoneNumber, boolean smsNotificationsEnabled) {
    this.surveyId = surveyId;
    this.userId = userId;
    this.realName = realName;
    this.phoneNumber = phoneNumber;
    this.email = email;
    this.smsNotificationsEnabled = smsNotificationsEnabled;
  }
}
