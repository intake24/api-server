--
-- PostgreSQL database dump
--

-- Dumped from database version 9.5.4
-- Dumped by pg_dump version 9.5.4

SET statement_timeout = 0;
SET lock_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner:
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner:
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


SET search_path = public, pg_catalog;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: external_test_users; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE external_test_users (
  id integer NOT NULL,
  survey_id character varying(32) NOT NULL,
  user_id character varying(512) NOT NULL,
  external_user_id character varying(512) NOT NULL,
  confirmation_code character varying(32) NOT NULL
);


ALTER TABLE external_test_users OWNER TO postgres;

--
-- Name: external_test_users_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE external_test_users_id_seq
START WITH 1
INCREMENT BY 1
NO MINVALUE
NO MAXVALUE
CACHE 1;


ALTER TABLE external_test_users_id_seq OWNER TO postgres;

--
-- Name: external_test_users_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE external_test_users_id_seq OWNED BY external_test_users.id;


--
-- Name: gen_user_counters; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE gen_user_counters (
  id integer NOT NULL,
  survey_id character varying(32) NOT NULL,
  count integer DEFAULT 0 NOT NULL
);


ALTER TABLE gen_user_counters OWNER TO postgres;

--
-- Name: gen_user_counters_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE gen_user_counters_id_seq
START WITH 1
INCREMENT BY 1
NO MINVALUE
NO MAXVALUE
CACHE 1;


ALTER TABLE gen_user_counters_id_seq OWNER TO postgres;

--
-- Name: gen_user_counters_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE gen_user_counters_id_seq OWNED BY gen_user_counters.id;


--
-- Name: global_support_staff; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE global_support_staff (
  user_survey_id character varying(64) NOT NULL,
  user_id character varying(256) NOT NULL,
  sms_notifications boolean DEFAULT true NOT NULL
);


ALTER TABLE global_support_staff OWNER TO postgres;

--
-- Name: global_values; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE global_values (
  name character varying(64) NOT NULL,
  value character varying(512) NOT NULL
);


ALTER TABLE global_values OWNER TO postgres;

--
-- Name: help_requests; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE help_requests (
  id integer NOT NULL,
  survey_id character varying(64) NOT NULL,
  user_id character varying(256) NOT NULL,
  last_help_request_at timestamp without time zone NOT NULL
);


ALTER TABLE help_requests OWNER TO postgres;

--
-- Name: help_requests_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE help_requests_id_seq
START WITH 1
INCREMENT BY 1
NO MINVALUE
NO MAXVALUE
CACHE 1;


ALTER TABLE help_requests_id_seq OWNER TO postgres;

--
-- Name: help_requests_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE help_requests_id_seq OWNED BY help_requests.id;


--
-- Name: last_help_request_times; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE last_help_request_times (
  survey_id character varying(32) NOT NULL,
  user_id character varying(512) NOT NULL,
  last_help_request_time timestamp without time zone NOT NULL
);


ALTER TABLE last_help_request_times OWNER TO postgres;

--
-- Name: local_nutrient_types; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE local_nutrient_types (
  id integer NOT NULL,
  locale_id character varying(16) NOT NULL,
  nutrient_type_id integer NOT NULL
);


ALTER TABLE local_nutrient_types OWNER TO postgres;

--
-- Name: local_nutrient_types_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE local_nutrient_types_id_seq
START WITH 1
INCREMENT BY 1
NO MINVALUE
NO MAXVALUE
CACHE 1;


ALTER TABLE local_nutrient_types_id_seq OWNER TO postgres;

--
-- Name: local_nutrient_types_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE local_nutrient_types_id_seq OWNED BY local_nutrient_types.id;


--
-- Name: locales; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE locales (
  id character varying(16) NOT NULL,
  english_name character varying(64) NOT NULL,
  local_name character varying(64) NOT NULL,
  respondent_language_id character varying(16) NOT NULL,
  admin_language_id character varying(16) NOT NULL,
  country_flag_code character varying(16) NOT NULL,
  prototype_locale_id character varying(16)
);


ALTER TABLE locales OWNER TO postgres;

--
-- Name: missing_foods; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE missing_foods (
  id integer NOT NULL,
  survey_id character varying(32) NOT NULL,
  user_id character varying(256) NOT NULL,
  name character varying(512) NOT NULL,
  brand character varying(512) NOT NULL,
  description character varying(512) NOT NULL,
  portion_size character varying(512) NOT NULL,
  leftovers character varying(512) NOT NULL,
  submitted_at timestamp without time zone NOT NULL
);


ALTER TABLE missing_foods OWNER TO postgres;

--
-- Name: missing_foods_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE missing_foods_id_seq
START WITH 1
INCREMENT BY 1
NO MINVALUE
NO MAXVALUE
CACHE 1;


ALTER TABLE missing_foods_id_seq OWNER TO postgres;

--
-- Name: missing_foods_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE missing_foods_id_seq OWNED BY missing_foods.id;


--
-- Name: nutrient_types; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE nutrient_types (
  id integer NOT NULL,
  description character varying(512) NOT NULL,
  unit_id integer NOT NULL
);


ALTER TABLE nutrient_types OWNER TO postgres;

--
-- Name: nutrient_units; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE nutrient_units (
  id integer NOT NULL,
  description character varying(512) NOT NULL,
  symbol character varying(32) NOT NULL
);


ALTER TABLE nutrient_units OWNER TO postgres;

--
-- Name: popularity_counters; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE popularity_counters (
  food_code character(8) NOT NULL,
  counter integer DEFAULT 0 NOT NULL
);


ALTER TABLE popularity_counters OWNER TO postgres;

--
-- Name: schema_version; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE schema_version (
  version integer NOT NULL
);


ALTER TABLE schema_version OWNER TO postgres;

--
-- Name: survey_submission_custom_fields; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE survey_submission_custom_fields (
  id integer NOT NULL,
  survey_submission_id uuid NOT NULL,
  name character varying(64) NOT NULL,
  value character varying(512) NOT NULL
);


ALTER TABLE survey_submission_custom_fields OWNER TO postgres;

--
-- Name: survey_submission_custom_fields_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE survey_submission_custom_fields_id_seq
START WITH 1
INCREMENT BY 1
NO MINVALUE
NO MAXVALUE
CACHE 1;


ALTER TABLE survey_submission_custom_fields_id_seq OWNER TO postgres;

--
-- Name: survey_submission_custom_fields_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE survey_submission_custom_fields_id_seq OWNED BY survey_submission_custom_fields.id;


--
-- Name: survey_submission_food_custom_fields; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE survey_submission_food_custom_fields (
  id integer NOT NULL,
  food_id integer NOT NULL,
  name character varying(64) NOT NULL,
  value character varying(512) NOT NULL
);


ALTER TABLE survey_submission_food_custom_fields OWNER TO postgres;

--
-- Name: survey_submission_food_custom_fields_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE survey_submission_food_custom_fields_id_seq
START WITH 1
INCREMENT BY 1
NO MINVALUE
NO MAXVALUE
CACHE 1;


ALTER TABLE survey_submission_food_custom_fields_id_seq OWNER TO postgres;

--
-- Name: survey_submission_food_custom_fields_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE survey_submission_food_custom_fields_id_seq OWNED BY survey_submission_food_custom_fields.id;


--
-- Name: survey_submission_foods; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE survey_submission_foods (
  id integer NOT NULL,
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
  nutrient_table_code character varying(64) NOT NULL
);


ALTER TABLE survey_submission_foods OWNER TO postgres;

--
-- Name: survey_submission_foods_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE survey_submission_foods_id_seq
START WITH 1
INCREMENT BY 1
NO MINVALUE
NO MAXVALUE
CACHE 1;


ALTER TABLE survey_submission_foods_id_seq OWNER TO postgres;

--
-- Name: survey_submission_foods_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE survey_submission_foods_id_seq OWNED BY survey_submission_foods.id;


--
-- Name: survey_submission_meal_custom_fields; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE survey_submission_meal_custom_fields (
  id integer NOT NULL,
  meal_id integer NOT NULL,
  name character varying(64) NOT NULL,
  value character varying(512) NOT NULL
);


ALTER TABLE survey_submission_meal_custom_fields OWNER TO postgres;

--
-- Name: survey_submission_meal_custom_fields_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE survey_submission_meal_custom_fields_id_seq
START WITH 1
INCREMENT BY 1
NO MINVALUE
NO MAXVALUE
CACHE 1;


ALTER TABLE survey_submission_meal_custom_fields_id_seq OWNER TO postgres;

--
-- Name: survey_submission_meal_custom_fields_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE survey_submission_meal_custom_fields_id_seq OWNED BY survey_submission_meal_custom_fields.id;


--
-- Name: survey_submission_meals; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE survey_submission_meals (
  id integer NOT NULL,
  survey_submission_id uuid NOT NULL,
  hours integer NOT NULL,
  minutes integer NOT NULL,
  name character varying(64)
);


ALTER TABLE survey_submission_meals OWNER TO postgres;

--
-- Name: survey_submission_meals_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE survey_submission_meals_id_seq
START WITH 1
INCREMENT BY 1
NO MINVALUE
NO MAXVALUE
CACHE 1;


ALTER TABLE survey_submission_meals_id_seq OWNER TO postgres;

--
-- Name: survey_submission_meals_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE survey_submission_meals_id_seq OWNED BY survey_submission_meals.id;


--
-- Name: survey_submission_nutrients; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE survey_submission_nutrients (
  id integer NOT NULL,
  food_id integer NOT NULL,
  amount double precision NOT NULL,
  nutrient_type_id integer NOT NULL
);


ALTER TABLE survey_submission_nutrients OWNER TO postgres;

--
-- Name: survey_submission_nutrients_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE survey_submission_nutrients_id_seq
START WITH 1
INCREMENT BY 1
NO MINVALUE
NO MAXVALUE
CACHE 1;


ALTER TABLE survey_submission_nutrients_id_seq OWNER TO postgres;

--
-- Name: survey_submission_nutrients_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE survey_submission_nutrients_id_seq OWNED BY survey_submission_nutrients.id;


--
-- Name: survey_submission_portion_size_fields; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE survey_submission_portion_size_fields (
  id integer NOT NULL,
  food_id integer NOT NULL,
  name character varying(64) NOT NULL,
  value character varying(512) NOT NULL
);


ALTER TABLE survey_submission_portion_size_fields OWNER TO postgres;

--
-- Name: survey_submission_portion_size_fields_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE survey_submission_portion_size_fields_id_seq
START WITH 1
INCREMENT BY 1
NO MINVALUE
NO MAXVALUE
CACHE 1;


ALTER TABLE survey_submission_portion_size_fields_id_seq OWNER TO postgres;

--
-- Name: survey_submission_portion_size_fields_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE survey_submission_portion_size_fields_id_seq OWNED BY survey_submission_portion_size_fields.id;


--
-- Name: survey_submission_user_custom_fields; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE survey_submission_user_custom_fields (
  id integer NOT NULL,
  survey_submission_id uuid NOT NULL,
  name character varying(64) NOT NULL,
  value character varying(512) NOT NULL
);


ALTER TABLE survey_submission_user_custom_fields OWNER TO postgres;

--
-- Name: survey_submission_user_custom_fields_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE survey_submission_user_custom_fields_id_seq
START WITH 1
INCREMENT BY 1
NO MINVALUE
NO MAXVALUE
CACHE 1;


ALTER TABLE survey_submission_user_custom_fields_id_seq OWNER TO postgres;

--
-- Name: survey_submission_user_custom_fields_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE survey_submission_user_custom_fields_id_seq OWNED BY survey_submission_user_custom_fields.id;


--
-- Name: survey_submissions; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE survey_submissions (
  id uuid NOT NULL,
  survey_id character varying(64) NOT NULL,
  user_id character varying(256) NOT NULL,
  start_time timestamp without time zone NOT NULL,
  end_time timestamp without time zone NOT NULL,
  log text
);


ALTER TABLE survey_submissions OWNER TO postgres;

--
-- Name: survey_support_staff; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE survey_support_staff (
  survey_id character varying(64) NOT NULL,
  user_survey_id character varying(64) NOT NULL,
  user_id character varying(256) NOT NULL,
  sms_notifications boolean DEFAULT true NOT NULL
);


ALTER TABLE survey_support_staff OWNER TO postgres;

--
-- Name: surveys; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE surveys (
  id character varying(64) NOT NULL,
  state integer NOT NULL,
  start_date timestamp without time zone DEFAULT now() NOT NULL,
  end_date timestamp without time zone DEFAULT now() NOT NULL,
  scheme_id character varying(64) NOT NULL,
  locale character varying(16) NOT NULL,
  allow_gen_users boolean NOT NULL,
  suspension_reason character varying(512),
  survey_monkey_url character varying(512),
  support_email character varying(512) DEFAULT 'support@intake24.co.uk'::character varying NOT NULL
);


ALTER TABLE surveys OWNER TO postgres;

--
-- Name: user_custom_fields; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE user_custom_fields (
  id integer NOT NULL,
  survey_id character varying(64) NOT NULL,
  user_id character varying(256) NOT NULL,
  name character varying(128) NOT NULL,
  value character varying(512) NOT NULL
);


ALTER TABLE user_custom_fields OWNER TO postgres;

--
-- Name: user_custom_fields_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE user_custom_fields_id_seq
START WITH 1
INCREMENT BY 1
NO MINVALUE
NO MAXVALUE
CACHE 1;


ALTER TABLE user_custom_fields_id_seq OWNER TO postgres;

--
-- Name: user_custom_fields_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE user_custom_fields_id_seq OWNED BY user_custom_fields.id;


--
-- Name: user_permissions; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE user_permissions (
  id integer NOT NULL,
  survey_id character varying(64) NOT NULL,
  user_id character varying(256) NOT NULL,
  permission character varying(64) NOT NULL
);


ALTER TABLE user_permissions OWNER TO postgres;

--
-- Name: user_permissions_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE user_permissions_id_seq
START WITH 1
INCREMENT BY 1
NO MINVALUE
NO MAXVALUE
CACHE 1;


ALTER TABLE user_permissions_id_seq OWNER TO postgres;

--
-- Name: user_permissions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE user_permissions_id_seq OWNED BY user_permissions.id;


--
-- Name: user_roles; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE user_roles (
  id integer NOT NULL,
  survey_id character varying(64) NOT NULL,
  user_id character varying(256) NOT NULL,
  role character varying(64) NOT NULL
);


ALTER TABLE user_roles OWNER TO postgres;

--
-- Name: user_roles_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE user_roles_id_seq
START WITH 1
INCREMENT BY 1
NO MINVALUE
NO MAXVALUE
CACHE 1;


ALTER TABLE user_roles_id_seq OWNER TO postgres;

--
-- Name: user_roles_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE user_roles_id_seq OWNED BY user_roles.id;


--
-- Name: users; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE users (
  id character varying(256) NOT NULL,
  survey_id character varying(64) DEFAULT ''::character varying NOT NULL,
  password_hash character varying(128) NOT NULL,
  password_salt character varying(128) NOT NULL,
  password_hasher character varying(64) NOT NULL,
  name character varying(512),
  email character varying(512),
  phone character varying(32)
);


ALTER TABLE users OWNER TO postgres;

--
-- Name: id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY external_test_users ALTER COLUMN id SET DEFAULT nextval('external_test_users_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY gen_user_counters ALTER COLUMN id SET DEFAULT nextval('gen_user_counters_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY help_requests ALTER COLUMN id SET DEFAULT nextval('help_requests_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY local_nutrient_types ALTER COLUMN id SET DEFAULT nextval('local_nutrient_types_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY missing_foods ALTER COLUMN id SET DEFAULT nextval('missing_foods_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY survey_submission_custom_fields ALTER COLUMN id SET DEFAULT nextval('survey_submission_custom_fields_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY survey_submission_food_custom_fields ALTER COLUMN id SET DEFAULT nextval('survey_submission_food_custom_fields_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY survey_submission_foods ALTER COLUMN id SET DEFAULT nextval('survey_submission_foods_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY survey_submission_meal_custom_fields ALTER COLUMN id SET DEFAULT nextval('survey_submission_meal_custom_fields_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY survey_submission_meals ALTER COLUMN id SET DEFAULT nextval('survey_submission_meals_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY survey_submission_nutrients ALTER COLUMN id SET DEFAULT nextval('survey_submission_nutrients_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY survey_submission_portion_size_fields ALTER COLUMN id SET DEFAULT nextval('survey_submission_portion_size_fields_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY survey_submission_user_custom_fields ALTER COLUMN id SET DEFAULT nextval('survey_submission_user_custom_fields_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY user_custom_fields ALTER COLUMN id SET DEFAULT nextval('user_custom_fields_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY user_permissions ALTER COLUMN id SET DEFAULT nextval('user_permissions_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY user_roles ALTER COLUMN id SET DEFAULT nextval('user_roles_id_seq'::regclass);


--
-- Name: external_test_user_unique; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY external_test_users
  ADD CONSTRAINT external_test_user_unique UNIQUE (survey_id, user_id, external_user_id);


--
-- Name: external_test_users_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY external_test_users
  ADD CONSTRAINT external_test_users_pk PRIMARY KEY (id);


--
-- Name: gen_user_count_id_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY gen_user_counters
  ADD CONSTRAINT gen_user_count_id_pk PRIMARY KEY (id);


--
-- Name: global_support_staff_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY global_support_staff
  ADD CONSTRAINT global_support_staff_pk PRIMARY KEY (user_survey_id, user_id);


--
-- Name: global_values_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY global_values
  ADD CONSTRAINT global_values_pk PRIMARY KEY (name);


--
-- Name: help_requests_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY help_requests
  ADD CONSTRAINT help_requests_pk PRIMARY KEY (id);


--
-- Name: last_help_request_times_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY last_help_request_times
  ADD CONSTRAINT last_help_request_times_pk PRIMARY KEY (survey_id, user_id);


--
-- Name: local_nutrient_types_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY local_nutrient_types
  ADD CONSTRAINT local_nutrient_types_pk PRIMARY KEY (id);


--
-- Name: locales_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY locales
  ADD CONSTRAINT locales_pk PRIMARY KEY (id);


--
-- Name: missing_foods_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY missing_foods
  ADD CONSTRAINT missing_foods_pk PRIMARY KEY (id);


--
-- Name: no_duplicate_permissions; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY user_permissions
  ADD CONSTRAINT no_duplicate_permissions UNIQUE (survey_id, user_id, permission);


--
-- Name: no_duplicate_roles; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY user_roles
  ADD CONSTRAINT no_duplicate_roles UNIQUE (survey_id, user_id, role);


--
-- Name: nutrient_types_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY nutrient_types
  ADD CONSTRAINT nutrient_types_pk PRIMARY KEY (id);


--
-- Name: nutrient_units_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY nutrient_units
  ADD CONSTRAINT nutrient_units_pk PRIMARY KEY (id);


--
-- Name: popularity_counters_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY popularity_counters
  ADD CONSTRAINT popularity_counters_pk PRIMARY KEY (food_code);


--
-- Name: schema_version_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY schema_version
  ADD CONSTRAINT schema_version_pk PRIMARY KEY (version);


--
-- Name: survey_submission_custom_field_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY survey_submission_custom_fields
  ADD CONSTRAINT survey_submission_custom_field_pk PRIMARY KEY (id);


--
-- Name: survey_submission_food_custom_fields_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY survey_submission_food_custom_fields
  ADD CONSTRAINT survey_submission_food_custom_fields_pk PRIMARY KEY (id);


--
-- Name: survey_submission_foods_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY survey_submission_foods
  ADD CONSTRAINT survey_submission_foods_pk PRIMARY KEY (id);


--
-- Name: survey_submission_meal_custom_fields_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY survey_submission_meal_custom_fields
  ADD CONSTRAINT survey_submission_meal_custom_fields_pk PRIMARY KEY (id);


--
-- Name: survey_submission_meals_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY survey_submission_meals
  ADD CONSTRAINT survey_submission_meals_pk PRIMARY KEY (id);


--
-- Name: survey_submission_nutrients_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY survey_submission_nutrients
  ADD CONSTRAINT survey_submission_nutrients_pk PRIMARY KEY (id);


--
-- Name: survey_submission_portion_size_fields_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY survey_submission_portion_size_fields
  ADD CONSTRAINT survey_submission_portion_size_fields_pk PRIMARY KEY (id);


--
-- Name: survey_submission_user_custom_fields_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY survey_submission_user_custom_fields
  ADD CONSTRAINT survey_submission_user_custom_fields_pk PRIMARY KEY (id);


--
-- Name: survey_submissions_id_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY survey_submissions
  ADD CONSTRAINT survey_submissions_id_pk PRIMARY KEY (id);


--
-- Name: survey_support_staff_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY survey_support_staff
  ADD CONSTRAINT survey_support_staff_pk PRIMARY KEY (survey_id, user_survey_id, user_id);


--
-- Name: surveys_id_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY surveys
  ADD CONSTRAINT surveys_id_pk PRIMARY KEY (id);


--
-- Name: user_custom_fields_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY user_custom_fields
  ADD CONSTRAINT user_custom_fields_pk PRIMARY KEY (id);


--
-- Name: user_permissions_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY user_permissions
  ADD CONSTRAINT user_permissions_pk PRIMARY KEY (id);


--
-- Name: user_roles_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY user_roles
  ADD CONSTRAINT user_roles_pk PRIMARY KEY (id);


--
-- Name: users_id_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY users
  ADD CONSTRAINT users_id_pk PRIMARY KEY (id, survey_id);


--
-- Name: gen_user_counters_survey_id_index; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX gen_user_counters_survey_id_index ON gen_user_counters USING btree (survey_id);


--
-- Name: help_requests_user_id_index; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX help_requests_user_id_index ON help_requests USING btree (survey_id, user_id);


--
-- Name: missing_foods_user_index; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX missing_foods_user_index ON missing_foods USING btree (survey_id, user_id);


--
-- Name: survey_submission_custom_fields_submission_index; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX survey_submission_custom_fields_submission_index ON survey_submission_custom_fields USING btree (survey_submission_id);


--
-- Name: survey_submission_food_custom_fields_food_index; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX survey_submission_food_custom_fields_food_index ON survey_submission_food_custom_fields USING btree (food_id);


--
-- Name: survey_submission_foods_meal_index; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX survey_submission_foods_meal_index ON survey_submission_foods USING btree (meal_id);


--
-- Name: survey_submission_meal_custom_fields_meal_id_index; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX survey_submission_meal_custom_fields_meal_id_index ON survey_submission_meal_custom_fields USING btree (meal_id);


--
-- Name: survey_submission_nutrients_food_index; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX survey_submission_nutrients_food_index ON survey_submission_nutrients USING btree (food_id);


--
-- Name: survey_submission_portion_size_fields_food_index; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX survey_submission_portion_size_fields_food_index ON survey_submission_portion_size_fields USING btree (food_id);


--
-- Name: survey_submission_user_custom_fields_submission_index; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX survey_submission_user_custom_fields_submission_index ON survey_submission_user_custom_fields USING btree (survey_submission_id);


--
-- Name: survey_submissions_meals_submission_index; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX survey_submissions_meals_submission_index ON survey_submission_meals USING btree (survey_submission_id);


--
-- Name: survey_submissions_user_index; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX survey_submissions_user_index ON survey_submissions USING btree (survey_id, user_id);


--
-- Name: user_custom_fields_user_id_index; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX user_custom_fields_user_id_index ON user_custom_fields USING btree (survey_id, user_id);


--
-- Name: user_permissions_user_id_index; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX user_permissions_user_id_index ON user_permissions USING btree (survey_id, user_id);


--
-- Name: user_roles_user_id_index; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX user_roles_user_id_index ON user_roles USING btree (survey_id, user_id);


--
-- Name: external_test_users_survey_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY external_test_users
  ADD CONSTRAINT external_test_users_survey_id_fk FOREIGN KEY (survey_id, user_id) REFERENCES users(survey_id, id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: gen_user_count_survey_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY gen_user_counters
  ADD CONSTRAINT gen_user_count_survey_id_fk FOREIGN KEY (survey_id) REFERENCES surveys(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: global_support_staff_user_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY global_support_staff
  ADD CONSTRAINT global_support_staff_user_id_fk FOREIGN KEY (user_survey_id, user_id) REFERENCES users(survey_id, id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: help_requests_users_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY help_requests
  ADD CONSTRAINT help_requests_users_fk FOREIGN KEY (survey_id, user_id) REFERENCES users(survey_id, id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: last_help_request_times_user_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY last_help_request_times
  ADD CONSTRAINT last_help_request_times_user_fk FOREIGN KEY (survey_id, user_id) REFERENCES users(survey_id, id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: local_nutrient_types_locale_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY local_nutrient_types
  ADD CONSTRAINT local_nutrient_types_locale_fk FOREIGN KEY (locale_id) REFERENCES locales(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: local_nutrient_types_nutrient_type_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY local_nutrient_types
  ADD CONSTRAINT local_nutrient_types_nutrient_type_fk FOREIGN KEY (nutrient_type_id) REFERENCES nutrient_types(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: locales_prototype_locale_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY locales
  ADD CONSTRAINT locales_prototype_locale_id_fk FOREIGN KEY (prototype_locale_id) REFERENCES locales(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: missing_foods_user_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY missing_foods
  ADD CONSTRAINT missing_foods_user_fk FOREIGN KEY (survey_id, user_id) REFERENCES users(survey_id, id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: nutrient_types_nutrient_unit_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY nutrient_types
  ADD CONSTRAINT nutrient_types_nutrient_unit_fk FOREIGN KEY (unit_id) REFERENCES nutrient_units(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: ssn_nutrient_type_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY survey_submission_nutrients
  ADD CONSTRAINT ssn_nutrient_type_id_fk FOREIGN KEY (nutrient_type_id) REFERENCES nutrient_types(id);


--
-- Name: survey_submission_custom_fields_survey_submission_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY survey_submission_custom_fields
  ADD CONSTRAINT survey_submission_custom_fields_survey_submission_id_fk FOREIGN KEY (survey_submission_id) REFERENCES survey_submissions(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: survey_submission_food_custom_fields_food_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY survey_submission_food_custom_fields
  ADD CONSTRAINT survey_submission_food_custom_fields_food_id_fk FOREIGN KEY (food_id) REFERENCES survey_submission_foods(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: survey_submission_foods_survey_submission_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY survey_submission_foods
  ADD CONSTRAINT survey_submission_foods_survey_submission_id_fk FOREIGN KEY (meal_id) REFERENCES survey_submission_meals(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: survey_submission_meal_custom_fields_meal_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY survey_submission_meal_custom_fields
  ADD CONSTRAINT survey_submission_meal_custom_fields_meal_id_fk FOREIGN KEY (meal_id) REFERENCES survey_submission_meals(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: survey_submission_meals_survey_submission_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY survey_submission_meals
  ADD CONSTRAINT survey_submission_meals_survey_submission_id_fk FOREIGN KEY (survey_submission_id) REFERENCES survey_submissions(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: survey_submission_nutrients_food_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY survey_submission_nutrients
  ADD CONSTRAINT survey_submission_nutrients_food_id_fk FOREIGN KEY (food_id) REFERENCES survey_submission_foods(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: survey_submission_portion_size_fields_food_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY survey_submission_portion_size_fields
  ADD CONSTRAINT survey_submission_portion_size_fields_food_id_fk FOREIGN KEY (food_id) REFERENCES survey_submission_foods(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: survey_submission_user_custom_fields_survey_submission_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY survey_submission_user_custom_fields
  ADD CONSTRAINT survey_submission_user_custom_fields_survey_submission_id_fk FOREIGN KEY (survey_submission_id) REFERENCES survey_submissions(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: survey_submissions_users_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY survey_submissions
  ADD CONSTRAINT survey_submissions_users_fk FOREIGN KEY (survey_id, user_id) REFERENCES users(survey_id, id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: survey_support_staff_survey_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY survey_support_staff
  ADD CONSTRAINT survey_support_staff_survey_id_fk FOREIGN KEY (survey_id) REFERENCES surveys(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: survey_support_staff_user_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY survey_support_staff
  ADD CONSTRAINT survey_support_staff_user_id_fk FOREIGN KEY (user_survey_id, user_id) REFERENCES users(survey_id, id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: user_custom_fields_users_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY user_custom_fields
  ADD CONSTRAINT user_custom_fields_users_fk FOREIGN KEY (survey_id, user_id) REFERENCES users(survey_id, id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: user_permissions_users_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY user_permissions
  ADD CONSTRAINT user_permissions_users_fk FOREIGN KEY (survey_id, user_id) REFERENCES users(survey_id, id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: user_roles_users_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY user_roles
  ADD CONSTRAINT user_roles_users_fk FOREIGN KEY (survey_id, user_id) REFERENCES users(survey_id, id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: users_surveys_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY users
  ADD CONSTRAINT users_surveys_id_fk FOREIGN KEY (survey_id) REFERENCES surveys(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

