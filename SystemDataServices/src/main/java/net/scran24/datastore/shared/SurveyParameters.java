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

package net.scran24.datastore.shared;

import com.google.gwt.user.client.rpc.IsSerializable;
import org.workcraft.gwt.shared.client.Option;

public class SurveyParameters implements IsSerializable {
    public SurveyState state;
    public long startDate;
    public long endDate;
    public boolean allowGenUsers;
    public String supportEmail;
    public Option<String> surveyMonkeyUrl;
    public String schemeName;
    public String locale;
    public String suspensionReason;

    @Deprecated
    public SurveyParameters() {
    }

    public SurveyParameters(SurveyState state, long startDate, long endDate, String schemeName, String locale,
                            boolean allowGenUsers, String supportEmail, String suspensionReason, Option<String> surveyMonkeyUrl) {
        this.state = state;
        this.startDate = startDate;
        this.endDate = endDate;
        this.schemeName = schemeName;
        this.locale = locale;
        this.allowGenUsers = allowGenUsers;
        this.supportEmail = supportEmail;
        this.suspensionReason = suspensionReason;
        this.surveyMonkeyUrl = surveyMonkeyUrl;
    }

    public SurveyParameters withState(SurveyState state) {
        return new SurveyParameters(state, this.startDate, this.endDate, this.schemeName, this.locale, this.allowGenUsers,
                this.supportEmail, this.suspensionReason, surveyMonkeyUrl);
    }

    public SurveyParameters withDates(long startDate, long endDate) {
        return new SurveyParameters(this.state, startDate, endDate, this.schemeName, this.locale, this.allowGenUsers,
                this.supportEmail, this.suspensionReason, surveyMonkeyUrl);
    }

    public SurveyParameters withSuspensionReason(String reason) {
        return new SurveyParameters(this.state, this.startDate, this.endDate, this.schemeName, this.locale,
                this.allowGenUsers, this.supportEmail, this.suspensionReason, surveyMonkeyUrl);
    }
}
