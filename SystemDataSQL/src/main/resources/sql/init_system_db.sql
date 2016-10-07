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

CREATE TABLE schema_migrations
(
  version bigint NOT NULL,
  CONSTRAINT schema_migrations_pk PRIMARY KEY(version)
);

CREATE TABLE surveys (
  id character varying(64) NOT NULL,
  state integer NOT NULL,
  start_date timestamp NOT NULL DEFAULT now(),
  end_date timestamp NOT NULL DEFAULT now(),
  scheme_id character varying(64) NOT NULL,
  locale character varying(16) NOT NULL,
  allow_gen_users boolean NOT NULL,
  suspension_reason character varying(512),
  survey_monkey_url character varying(512),

  CONSTRAINT surveys_id_pk PRIMARY KEY(id)
);

CREATE TABLE users (
  id character varying(256) NOT NULL,
  survey_id character varying(64) DEFAULT '' NOT NULL,
  password_hash character varying(128) NOT NULL,
  password_salt character varying(128) NOT NULL,
  password_hasher character varying(64) NOT NULL,

  CONSTRAINT users_id_pk PRIMARY KEY(id, survey_id),
  CONSTRAINT users_surveys_id_fk FOREIGN KEY (survey_id)
    REFERENCES surveys(id) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE user_roles (
  id serial NOT NULL,
  survey_id character varying(64) NOT NULL,
  user_id character varying(256) NOT NULL,
  role character varying(64) NOT NULL,

  CONSTRAINT no_duplicate_roles UNIQUE(survey_id, user_id, role),
  CONSTRAINT user_roles_pk PRIMARY KEY (id),
  CONSTRAINT user_roles_users_fk FOREIGN KEY (survey_id, user_id) 
    REFERENCES users(survey_id, id) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE INDEX user_roles_user_id_index ON user_roles(survey_id, user_id);

CREATE TABLE user_permissions (
  id serial NOT NULL,
  survey_id character varying(64) NOT NULL,
  user_id character varying(256) NOT NULL,  
  permission character varying(64) NOT NULL,

  CONSTRAINT no_duplicate_permissions UNIQUE(survey_id, user_id, permission),
  CONSTRAINT user_permissions_pk PRIMARY KEY (id),
  CONSTRAINT user_permissions_users_fk FOREIGN KEY (survey_id, user_id) 
    REFERENCES users(survey_id, id) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE INDEX user_permissions_user_id_index ON user_permissions(survey_id, user_id);

CREATE TABLE user_custom_fields (
  id serial NOT NULL,
  survey_id character varying(64) NOT NULL,
  user_id character varying(256) NOT NULL,
  name character varying(128) NOT NULL,
  value character varying(512) NOT NULL,

  CONSTRAINT user_custom_fields_pk PRIMARY KEY (id),
  CONSTRAINT user_custom_fields_users_fk FOREIGN KEY (survey_id, user_id) 
    REFERENCES users(survey_id, id) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE INDEX user_custom_fields_user_id_index ON user_custom_fields(survey_id, user_id);

CREATE TABLE help_requests (
  id serial NOT NULL,
  survey_id character varying(64) NOT NULL,
  user_id character varying(256) NOT NULL,
  last_help_request_at timestamp NOT NULL,

  CONSTRAINT help_requests_pk PRIMARY KEY (id),
  CONSTRAINT help_requests_users_fk FOREIGN KEY (survey_id, user_id) 
    REFERENCES users(survey_id, id) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE INDEX help_requests_user_id_index ON help_requests(survey_id, user_id);

CREATE TABLE gen_user_counters (
  id serial NOT NULL,
  survey_id character varying(32) NOT NULL,
  count integer NOT NULL DEFAULT 0,
  CONSTRAINT gen_user_count_id_pk PRIMARY KEY (id),
  CONSTRAINT gen_user_count_survey_id_fk FOREIGN KEY (survey_id)
    REFERENCES surveys(id) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE INDEX gen_user_counters_survey_id_index ON gen_user_counters(survey_id);

CREATE TABLE survey_submissions (
  id uuid NOT NULL,
  survey_id character varying(64) NOT NULL,
  user_id character varying(256) NOT NULL,
  start_time timestamp NOT NULL,
  end_time timestamp NOT NULL,
  log TEXT,

  CONSTRAINT survey_submissions_id_pk PRIMARY KEY(id),
  CONSTRAINT survey_submissions_users_fk FOREIGN KEY(survey_id, user_id)
    REFERENCES users(survey_id, id) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE INDEX survey_submissions_user_index ON survey_submissions(survey_id, user_id);

CREATE TABLE survey_submission_custom_fields (
  id serial NOT NULL,
  survey_submission_id uuid NOT NULL,
  name character varying(64) NOT NULL,
  value character varying(512) NOT NULL,

  CONSTRAINT survey_submission_custom_field_pk PRIMARY KEY (id),
  CONSTRAINT survey_submission_custom_fields_survey_submission_id_fk FOREIGN KEY (survey_submission_id)
    REFERENCES survey_submissions(id) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE INDEX survey_submission_custom_fields_submission_index ON survey_submission_custom_fields(survey_submission_id);

CREATE TABLE survey_submission_user_custom_fields (
  id serial NOT NULL,
  survey_submission_id uuid NOT NULL,
  name character varying(64) NOT NULL,
  value character varying(512) NOT NULL,

  CONSTRAINT survey_submission_user_custom_fields_pk PRIMARY KEY (id),
  CONSTRAINT survey_submission_user_custom_fields_survey_submission_id_fk FOREIGN KEY (survey_submission_id)
    REFERENCES survey_submissions(id) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE INDEX survey_submission_user_custom_fields_submission_index ON survey_submission_user_custom_fields (survey_submission_id);

CREATE TABLE survey_submission_meals (
  id serial NOT NULL,
  survey_submission_id uuid NOT NULL,
  hours integer NOT NULL,
  minutes integer NOT NULL,
  name character varying(64),
  
  CONSTRAINT survey_submission_meals_pk PRIMARY KEY (id),
  CONSTRAINT survey_submission_meals_survey_submission_id_fk FOREIGN KEY (survey_submission_id)
    REFERENCES survey_submissions(id) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE INDEX survey_submissions_meals_submission_index ON survey_submission_meals(survey_submission_id);

CREATE TABLE survey_submission_meal_custom_fields (
  id serial NOT NULL,
  meal_id integer NOT NULL,
  name character varying(64) NOT NULL,
  value character varying(512) NOT NULL,

  CONSTRAINT survey_submission_meal_custom_fields_pk PRIMARY KEY (id),
  CONSTRAINT survey_submission_meal_custom_fields_meal_id_fk FOREIGN KEY (meal_id)
    REFERENCES survey_submission_meals(id) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE INDEX survey_submission_meal_custom_fields_meal_id_index ON survey_submission_meal_custom_fields(meal_id);

CREATE TABLE survey_submission_foods (
  id serial NOT NULL,
  meal_id integer NOT NULL,
  code character varying(8) NOT NULL,
  english_description character varying(128) NOT NULL,
  local_description character varying(128),
  ready_meal boolean NOT NULL,
  search_term character varying(256) NOT NULL,
  portion_size_method_id character varying(32) NOT NULL,
  reasonable_amount boolean NOT NULL,
  food_group_id integer NOT NULL,
  food_group_english_description character varying(256) NOT NULL,
  food_group_local_description character varying(256),
  brand character varying(128) NOT NULL,
  nutrient_table_id character varying(64) NOT NULL,
  nutrient_table_code character varying(64) NOT NULL,
    
  CONSTRAINT survey_submission_foods_pk PRIMARY KEY (id),
  CONSTRAINT survey_submission_foods_survey_submission_id_fk FOREIGN KEY (meal_id)
    REFERENCES survey_submission_meals(id) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE INDEX survey_submission_foods_meal_index ON survey_submission_foods (meal_id);

CREATE TABLE survey_submission_portion_size_fields (
  id serial NOT NULL,
  food_id integer NOT NULL,
  name character varying(64) NOT NULL,
  value character varying(512) NOT NULL,

  CONSTRAINT survey_submission_portion_size_fields_pk PRIMARY KEY (id),
  CONSTRAINT survey_submission_portion_size_fields_food_id_fk FOREIGN KEY (food_id)
    REFERENCES survey_submission_foods(id) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE INDEX survey_submission_portion_size_fields_food_index ON survey_submission_portion_size_fields(food_id);

CREATE TABLE survey_submission_food_custom_fields (
  id serial NOT NULL,
  food_id integer NOT NULL,
  name character varying(64) NOT NULL,
  value character varying(512) NOT NULL,

  CONSTRAINT survey_submission_food_custom_fields_pk PRIMARY KEY (id),
  CONSTRAINT survey_submission_food_custom_fields_food_id_fk FOREIGN KEY (food_id)
    REFERENCES survey_submission_foods(id) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE INDEX survey_submission_food_custom_fields_food_index ON survey_submission_food_custom_fields(food_id);

CREATE TABLE survey_submission_nutrients (
  id serial NOT NULL,
  food_id integer NOT NULL,
  name character varying(64) NOT NULL,
  amount float NOT NULL,

  CONSTRAINT survey_submission_nutrients_pk PRIMARY KEY(id),
  CONSTRAINT survey_submission_nutrients_food_id_fk FOREIGN KEY(food_id)
    REFERENCES survey_submission_foods(id) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE INDEX survey_submission_nutrients_food_index ON survey_submission_nutrients(food_id);

INSERT INTO surveys VALUES ('', 0, now(), now(), '', 'en_GB', false, NULL, NULL);

CREATE TABLE missing_foods (
  id serial NOT NULL,
  survey_id character varying(32) NOT NULL,
  user_id character varying(256) NOT NULL,
  name character varying(512) NOT NULL,
  brand character varying(512) NOT NULL,
  description character varying(512) NOT NULL,
  portion_size character varying(512) NOT NULL,
  leftovers character varying(512) NOT NULL,
  submitted_at timestamp NOT NULL,

  CONSTRAINT missing_foods_pk PRIMARY KEY(id),
  CONSTRAINT missing_foods_user_fk FOREIGN KEY(survey_id, user_id)
    REFERENCES users(survey_id, id) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE INDEX missing_foods_user_index ON missing_foods(survey_id, user_id);

CREATE TABLE popularity_counters (
  food_code character(4) NOT NULL,
  counter integer NOT NULL DEFAULT 0,

  CONSTRAINT popularity_counters_pk PRIMARY KEY(food_code)  
);

CREATE TABLE global_values (
  name character varying(64) NOT NULL,
  value character varying(512) NOT NULL,

  CONSTRAINT global_values_pk PRIMARY KEY(name)
);

CREATE TABLE support_staff (
  id serial NOT NULL,
  name character varying(256) NOT NULL,
  phone character varying(32),
  email character varying(512),

  CONSTRAINT support_staff_pk PRIMARY KEY(id)
);

CREATE TABLE last_help_request_times (
  survey_id character varying(32) NOT NULL,
  user_id character varying(512) NOT NULL,
  last_help_request_time timestamp NOT NULL,

  CONSTRAINT last_help_request_times_pk PRIMARY KEY(survey_id, user_id),
  CONSTRAINT last_help_request_times_user_fk FOREIGN KEY(survey_id, user_id)
    REFERENCES users(survey_id, id) ON UPDATE CASCADE ON DELETE CASCADE 
);

CREATE TABLE external_test_users (
  id serial NOT NULL,
  survey_id character varying(32) NOT NULL,
  user_id character varying(512) NOT NULL,
  external_user_id character varying(512) NOT NULL,
  confirmation_code character varying(32) NOT NULL,

  CONSTRAINT external_test_users_pk PRIMARY KEY(id),
  CONSTRAINT external_test_users_survey_id_fk FOREIGN KEY(survey_id, user_id)
    REFERENCES users(survey_id, id) ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT external_test_user_unique UNIQUE(survey_id, user_id, external_user_id)
);

