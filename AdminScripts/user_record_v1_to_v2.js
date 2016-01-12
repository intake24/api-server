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

db = connect(db_host + ":" + db_port + "/" + db_name);

function convert (userdata) {
  var result = {};
  for (var i=0; i<userdata.length; i++) {
    result[userdata[i].name] = userdata[i].value;
  }
  return result;
}

function processUsersCollection(name) {
  print("Processing " + name + "...");
  db.getCollection(name).find().forEach( function (rec) { 
    print ("Converting " + rec.username);
    db.getCollection(name).update ({ _id : rec._id }, { $set: { userdata: convert(rec.userdata) }}, { multi: true } );
  });
}

cols = db.getCollectionNames().filter( function (x) { return x.startsWith("users_") } );

for (var i=0; i<cols.length; i++) {
  processUsersCollection(cols[i]);
}