

Intake24
========

Intake24 is an open-source web-based dietary recall system.

see https://intake24.co.uk

Building
========

Install the infiauto-datastr library from https://github.com/digitalinteraction/infiauto

Run

    mvn package


Running
=======

**Requires Java 8.**

Download the latest (standard) image database from https://intake24.co.uk/info/sources.html

Clone the current food definition database from https://github.com/digitalinteraction/intake24-data

Install mongodb

Modify and run 

    mongo init_mongo_db.js 

in the AdminScripts directory. **Make sure to set the correct mongodb host parameters before running this script.**

The default account for the admin user (accessible at <webapp host url>/admin) is "admin" and the password is "intake24". 

Copy the contents of ''Webapp/target'' in the source directory to your servlet container webapps directory. 

Edit web.xml to configure the system for your specific deployment.
