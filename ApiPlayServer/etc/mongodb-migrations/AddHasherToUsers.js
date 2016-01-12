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
*/

var colNames = db.getCollectionNames();
var userCols = [];

for (var i = 0; i < colNames.length; i++) {
	if (colNames[i].startsWith("users_"))
		userCols.push(colNames[i]);
}

for (var i = 0; i < userCols.length; i++) {
	print("Updating " + userCols[i]);
	db[userCols[i]].update({
		hasher : {
			$exists : false
		}
	}, {
		$set : {
			hasher : "shiro-sha256"
		}
	}, {
		multi : true
	});
}