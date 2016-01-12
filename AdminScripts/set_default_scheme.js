/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

﻿// database evolution script
// no longer relevant, kept for reference

// change these parameters in case of 
// non-default database configuration 

db_host = "127.0.0.1"
db_port = 27017 // default mongodb port
db_name = "intake24"

db.survey_state.find().forEach( function (state_record) {
  if (!state_record.hasOwnProperty("schemeName"))
    db.survey_state.update( { _id : state_record._id }, { $set: { schemeName : "default" } } );
});