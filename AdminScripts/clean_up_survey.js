/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

﻿// change these parameters in case of 
// non-default database configuration 

db_host = "workcraft.org"
db_port = 27017 // default mongodb port
db_name = "intake24-layout"

// set survey id here
// all data from this survey will be DELETED
// please use only if you are sure you know what you are doing
survey_id = "test" 

if (survey_id == "") {
  print ();
  print ("*** Please edit the script and set the survey_id var ***");
}
else {
  db = connect(db_host + ":" + db_port + "/" + db_name);
  print ("Deleting " + survey_id);
  db.getCollection("users_" + survey_id).drop();
  db.getCollection("surveys_" + survey_id).drop();
  db.getCollection("survey_state").remove ( { "surveyId" : survey_id } );
  print ("Done");
}
