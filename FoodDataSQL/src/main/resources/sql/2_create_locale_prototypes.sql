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

CREATE TABLE locale_prototypes
(
  locale_id character varying(16) NOT NULL,
  prototype_locale_id character varying(16) NOT NULL,

  CONSTRAINT locale_prototypes_pk PRIMARY KEY (locale_id, prototype_locale_id),
  CONSTRAINT locale_prototypes_only_one_prototype UNIQUE(locale_id),
  CONSTRAINT locale_prototypes_locale_id_fk FOREIGN KEY (locale_id)
    REFERENCES locales(id) ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT locale_prototypes_prototype_locale_id_fk FOREIGN KEY (prototype_locale_id)
    REFERENCES locales(id) ON UPDATE CASCADE ON DELETE CASCADE    
);

