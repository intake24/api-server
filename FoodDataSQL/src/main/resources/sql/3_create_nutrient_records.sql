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

CREATE TABLE nutrients
(
	id integer NOT NULL,
	description character varying(512) NOT NULL,

	CONSTRAINT nutrients_pk PRIMARY KEY(id)
);


CREATE TABLE nutrient_units
(
	id integer NOT NULL,
	symbol character varying(32) NOT NULL,

	CONSTRAINT nutrient_units_pk PRIMARY KEY(id)
);

CREATE TABLE nutrient_records
(
	id character varying(32) NOT NULL,
	nutrient_table_id character varying(32) NOT NULL,
	nutrient_id integer NOT NULL,
	nutrient_unit_id integer NOT NULL,
	units_per_100g double precision,

	CONSTRAINT nutrient_records_pk PRIMARY KEY(id, nutrient_table_id, nutrient_id),
	CONSTRAINT nutrient_records_nutrient_tables_id_fk FOREIGN KEY (nutrient_table_id)
		REFERENCES nutrient_tables(id) ON UPDATE CASCADE ON DELETE CASCADE,
	CONSTRAINT nutrient_records_nutrient_id_fk FOREIGN KEY (nutrient_id)
		REFERENCES nutrients(id) ON UPDATE CASCADE ON DELETE CASCADE,
	CONSTRAINT nutrient_records_nutrient_unit_fk FOREIGN KEY (nutrient_unit_id)
		REFERENCES nutrient_units(id) ON UPDATE CASCADE ON DELETE CASCADE
);
