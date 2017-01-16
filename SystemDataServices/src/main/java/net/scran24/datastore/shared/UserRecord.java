/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.datastore.shared;

import com.google.gwt.user.client.rpc.IsSerializable;
import org.workcraft.gwt.shared.client.Option;

import java.util.Map;

public class UserRecord implements IsSerializable {
    public String username;
    public String password;
    public Option<String> name;
    public Option<String> email;
    public Option<String> phone;
    public Map<String, String> customFields;

    @Deprecated
    public UserRecord() {

    }

    public UserRecord(String username, String password, Option<String> name, Option<String> email, Option<String> phone, Map<String, String> customFields) {
        this.username = username;
        this.password = password;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.customFields = customFields;
    }
}
