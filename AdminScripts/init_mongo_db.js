/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

// change these parameters in case of 
// non-default database configuration 

db_host = "localhost"
db_port = 27017 // default mongodb port
db_name = "intake24"

print ("Initialising Intake24 database using host " + db_host);

db = connect(db_host + ":" + db_port + "/" + db_name);

if (db.getCollectionNames().length != 0) {
    print ("The database already contains some data. Please specify an empty database!");
} else {
    db.createCollection("users_admin");
    db.users_admin.ensureIndex ( { username : 1 } , { unique : true } );
    db.users_admin.insert(
	{
	    "username" : "admin", 
	    "password" : "f0/k0CzPpDLD9gUE++8Mg1OMG8ch1jBduoQ0Olvv9L8=", 
	    "salt" : "pPFGwdg1GOO5f/aVj1kzqA==", 
	    "roles" : [ "admin" ], 
	    "permissions" : [ ], 
	    "userdata" : { }
	});

    db.createCollection("popularity");
    db.popularity.ensureIndex( { code : 1 } , { unique : true } );

    db.createCollection("global_values");
    db.global_values.ensureIndex( { name : 1 } , { unique : true } );

    db.createCollection("survey_state");

    print ("Database initialisation complete.");
}
