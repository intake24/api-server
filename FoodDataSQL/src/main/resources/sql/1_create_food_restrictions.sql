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

CREATE TABLE foods_restrictions
(
  food_code character(4) NOT NULL,
  locale_id character varying(16) NOT NULL,

  CONSTRAINT foods_restrictions_pk PRIMARY KEY (food_code, locale_id),
  CONSTRAINT foods_restrictions_food_code_fk FOREIGN KEY (food_code)
    REFERENCES foods(code) ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT foods_restrictions_locale_id_fk FOREIGN KEY (locale_id)
    REFERENCES locales(id) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE categories_restrictions
(
  category_code character(4) NOT NULL,
  locale_id character varying(16) NOT NULL,

  CONSTRAINT categories_restrictions_pk PRIMARY KEY (category_code, locale_id),
  CONSTRAINT categories_restrictions_food_code_fk FOREIGN KEY (category_code)
    REFERENCES categories(code) ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT categories_restrictions_locale_id_fk FOREIGN KEY (locale_id)
    REFERENCES locales(id) ON UPDATE CASCADE ON DELETE CASCADE
);

