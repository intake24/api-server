--
-- PostgreSQL database dump
--

-- Dumped from database version 12.7 (Ubuntu 12.7-1.pgdg18.04+1)
-- Dumped by pg_dump version 14.1 (Ubuntu 14.1-1.pgdg18.04+1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: sex_enum; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.sex_enum AS ENUM (
    'f',
    'm'
);


--
-- Name: weight_target_enum; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.weight_target_enum AS ENUM (
    'keep_weight',
    'lose_weight',
    'gain_weight'
);


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: client_error_reports; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.client_error_reports (
    id integer NOT NULL,
    user_id character varying(256),
    survey_id character varying(64),
    reported_at timestamp without time zone NOT NULL,
    stack_trace character varying(1024)[] NOT NULL,
    survey_state_json text NOT NULL,
    new boolean DEFAULT true NOT NULL
);


--
-- Name: data_export_downloads; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.data_export_downloads (
    id integer NOT NULL,
    task_id integer NOT NULL,
    upload_successful boolean,
    stack_trace character varying(256),
    download_url character varying(1024),
    download_url_expires_at timestamp with time zone
);


--
-- Name: data_export_downloads_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.data_export_downloads_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: data_export_downloads_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.data_export_downloads_id_seq OWNED BY public.data_export_downloads.id;


--
-- Name: data_export_scheduled; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.data_export_scheduled (
    id integer NOT NULL,
    user_id integer NOT NULL,
    survey_id character varying(64) NOT NULL,
    period_days integer,
    days_of_week integer DEFAULT 127,
    "time" time without time zone NOT NULL,
    time_zone character varying(32) NOT NULL,
    action character varying(32) NOT NULL,
    action_config character varying(1024) NOT NULL,
    next_run_utc timestamp without time zone NOT NULL,
    CONSTRAINT data_export_scheduled_days_of_week_check CHECK (((days_of_week > 0) AND (days_of_week <= 127)))
);


--
-- Name: data_export_scheduled_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.data_export_scheduled_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: data_export_scheduled_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.data_export_scheduled_id_seq OWNED BY public.data_export_scheduled.id;


--
-- Name: data_export_tasks; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.data_export_tasks (
    id integer NOT NULL,
    survey_id character varying(64) NOT NULL,
    date_from timestamp with time zone NOT NULL,
    date_to timestamp with time zone NOT NULL,
    user_id integer NOT NULL,
    created_at timestamp with time zone NOT NULL,
    started_at timestamp with time zone,
    completed_at timestamp with time zone,
    progress real,
    successful boolean,
    stack_trace character varying(256)[],
    purpose character varying(16) DEFAULT 'download'::character varying NOT NULL
);


--
-- Name: data_export_tasks_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.data_export_tasks_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: data_export_tasks_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.data_export_tasks_id_seq OWNED BY public.data_export_tasks.id;


--
-- Name: external_test_users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.external_test_users (
    id integer NOT NULL,
    user_id integer NOT NULL,
    external_user_id character varying(512) NOT NULL,
    confirmation_code character varying(32) NOT NULL
);


--
-- Name: external_test_users_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.external_test_users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: external_test_users_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.external_test_users_id_seq OWNED BY public.external_test_users.id;


--
-- Name: fixed_food_ranking; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.fixed_food_ranking (
    id integer NOT NULL,
    locale_id character varying(16) NOT NULL,
    food_code character varying(8) NOT NULL,
    rank integer NOT NULL
);


--
-- Name: fixed_food_ranking_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.fixed_food_ranking_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: fixed_food_ranking_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.fixed_food_ranking_id_seq OWNED BY public.fixed_food_ranking.id;


--
-- Name: flyway_migrations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.flyway_migrations (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);


--
-- Name: gen_user_counters; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.gen_user_counters (
    survey_id character varying(64) NOT NULL,
    count integer DEFAULT 0 NOT NULL
);


--
-- Name: gwt_client_error_reports_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.gwt_client_error_reports_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: gwt_client_error_reports_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.gwt_client_error_reports_id_seq OWNED BY public.client_error_reports.id;


--
-- Name: local_fields; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.local_fields (
    id integer NOT NULL,
    locale_id character varying(16) NOT NULL,
    field_name character varying(32) NOT NULL,
    description character varying(256) NOT NULL
);


--
-- Name: local_fields_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.local_fields_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: local_fields_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.local_fields_id_seq OWNED BY public.local_fields.id;


--
-- Name: local_nutrient_types; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.local_nutrient_types (
    id integer NOT NULL,
    locale_id character varying(16) NOT NULL,
    nutrient_type_id integer NOT NULL
);


--
-- Name: local_nutrient_types_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.local_nutrient_types_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: local_nutrient_types_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.local_nutrient_types_id_seq OWNED BY public.local_nutrient_types.id;


--
-- Name: locales; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.locales (
    id character varying(16) NOT NULL,
    english_name character varying(64) NOT NULL,
    local_name character varying(64) NOT NULL,
    respondent_language_id character varying(16) NOT NULL,
    admin_language_id character varying(16) NOT NULL,
    country_flag_code character varying(16) NOT NULL,
    prototype_locale_id character varying(16),
    text_direction character varying(8) DEFAULT 'ltr'::character varying NOT NULL
);


--
-- Name: missing_foods; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.missing_foods (
    id integer NOT NULL,
    survey_id character varying(32) NOT NULL,
    user_id integer NOT NULL,
    name character varying(512) NOT NULL,
    brand character varying(512) NOT NULL,
    description character varying(512) NOT NULL,
    portion_size character varying(512) NOT NULL,
    leftovers character varying(512) NOT NULL,
    submitted_at timestamp without time zone NOT NULL
);


--
-- Name: missing_foods_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.missing_foods_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: missing_foods_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.missing_foods_id_seq OWNED BY public.missing_foods.id;


--
-- Name: nutrient_types; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.nutrient_types (
    id integer NOT NULL,
    description character varying(512) NOT NULL,
    unit_id integer NOT NULL
);


--
-- Name: nutrient_units; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.nutrient_units (
    id integer NOT NULL,
    description character varying(512) NOT NULL,
    symbol character varying(32) NOT NULL
);


--
-- Name: pairwise_associations_co_occurrences; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.pairwise_associations_co_occurrences (
    locale character varying(64) NOT NULL,
    antecedent_food_code character varying(50) NOT NULL,
    consequent_food_code character varying(50) NOT NULL,
    occurrences integer NOT NULL
);


--
-- Name: pairwise_associations_occurrences; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.pairwise_associations_occurrences (
    locale character varying(64) NOT NULL,
    food_code character varying(50) NOT NULL,
    occurrences integer NOT NULL
);


--
-- Name: pairwise_associations_state; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.pairwise_associations_state (
    id integer primary key not null,
    last_submission_time timestamp with time zone DEFAULT '1970-01-01 00:00:00+00'::timestamp with time zone NOT NULL
);


--
-- Name: pairwise_associations_transactions_count; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.pairwise_associations_transactions_count (
    locale character varying(64) NOT NULL,
    transactions_count integer NOT NULL
);


--
-- Name: popularity_counters; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.popularity_counters (
    food_code character varying(8) NOT NULL,
    counter integer DEFAULT 0 NOT NULL
);


--
-- Name: schema_version; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.schema_version (
    version bigint NOT NULL
);


--
-- Name: short_urls; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.short_urls (
    long_url character varying(1000) NOT NULL,
    short_url character varying(100) NOT NULL
);


--
-- Name: signin_log; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.signin_log (
    id integer NOT NULL,
    date timestamp with time zone DEFAULT now() NOT NULL,
    remote_address character varying(64),
    provider character varying(64) NOT NULL,
    provider_key character varying(512) NOT NULL,
    successful boolean NOT NULL,
    user_id integer,
    message text,
    user_agent character varying(512)
);


--
-- Name: signin_log_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.signin_log_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: signin_log_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.signin_log_id_seq OWNED BY public.signin_log.id;


--
-- Name: survey_submission_custom_fields; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.survey_submission_custom_fields (
    id integer NOT NULL,
    survey_submission_id uuid NOT NULL,
    name character varying(64) NOT NULL,
    value character varying(512) NOT NULL
);


--
-- Name: survey_submission_custom_fields_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.survey_submission_custom_fields_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: survey_submission_custom_fields_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.survey_submission_custom_fields_id_seq OWNED BY public.survey_submission_custom_fields.id;


--
-- Name: survey_submission_fields; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.survey_submission_fields (
    id integer NOT NULL,
    food_id integer NOT NULL,
    field_name character varying(32) NOT NULL,
    value character varying(512) NOT NULL
);


--
-- Name: survey_submission_fields_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.survey_submission_fields_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: survey_submission_fields_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.survey_submission_fields_id_seq OWNED BY public.survey_submission_fields.id;


--
-- Name: survey_submission_food_custom_fields; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.survey_submission_food_custom_fields (
    id integer NOT NULL,
    food_id integer NOT NULL,
    name character varying(64) NOT NULL,
    value character varying(512) NOT NULL
);


--
-- Name: survey_submission_food_custom_fields_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.survey_submission_food_custom_fields_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: survey_submission_food_custom_fields_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.survey_submission_food_custom_fields_id_seq OWNED BY public.survey_submission_food_custom_fields.id;


--
-- Name: survey_submission_foods; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.survey_submission_foods (
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


--
-- Name: survey_submission_foods_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.survey_submission_foods_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: survey_submission_foods_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.survey_submission_foods_id_seq OWNED BY public.survey_submission_foods.id;


--
-- Name: survey_submission_meal_custom_fields; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.survey_submission_meal_custom_fields (
    id integer NOT NULL,
    meal_id integer NOT NULL,
    name character varying(64) NOT NULL,
    value character varying(512) NOT NULL
);


--
-- Name: survey_submission_meal_custom_fields_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.survey_submission_meal_custom_fields_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: survey_submission_meal_custom_fields_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.survey_submission_meal_custom_fields_id_seq OWNED BY public.survey_submission_meal_custom_fields.id;


--
-- Name: survey_submission_meals; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.survey_submission_meals (
    id integer NOT NULL,
    survey_submission_id uuid NOT NULL,
    hours integer NOT NULL,
    minutes integer NOT NULL,
    name character varying(64)
);


--
-- Name: survey_submission_meals_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.survey_submission_meals_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: survey_submission_meals_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.survey_submission_meals_id_seq OWNED BY public.survey_submission_meals.id;


--
-- Name: survey_submission_missing_foods; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.survey_submission_missing_foods (
    id integer NOT NULL,
    meal_id integer NOT NULL,
    name character varying(512) NOT NULL,
    brand character varying(512) NOT NULL,
    description character varying(512) NOT NULL,
    portion_size character varying(512) NOT NULL,
    leftovers character varying(512) NOT NULL
);


--
-- Name: survey_submission_missing_foods_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.survey_submission_missing_foods_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: survey_submission_missing_foods_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.survey_submission_missing_foods_id_seq OWNED BY public.survey_submission_missing_foods.id;


--
-- Name: survey_submission_nutrients; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.survey_submission_nutrients (
    id integer NOT NULL,
    food_id integer NOT NULL,
    amount double precision NOT NULL,
    nutrient_type_id integer NOT NULL
);


--
-- Name: survey_submission_nutrients_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.survey_submission_nutrients_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: survey_submission_nutrients_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.survey_submission_nutrients_id_seq OWNED BY public.survey_submission_nutrients.id;


--
-- Name: survey_submission_portion_size_fields; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.survey_submission_portion_size_fields (
    id integer NOT NULL,
    food_id integer NOT NULL,
    name character varying(64) NOT NULL,
    value character varying(512) NOT NULL
);


--
-- Name: survey_submission_portion_size_fields_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.survey_submission_portion_size_fields_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: survey_submission_portion_size_fields_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.survey_submission_portion_size_fields_id_seq OWNED BY public.survey_submission_portion_size_fields.id;


--
-- Name: survey_submissions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.survey_submissions (
    id uuid NOT NULL,
    survey_id character varying(64) NOT NULL,
    user_id integer NOT NULL,
    start_time timestamp with time zone NOT NULL,
    end_time timestamp with time zone NOT NULL,
    log text[],
    ux_session_id uuid NOT NULL,
    submission_time timestamp with time zone NOT NULL
);


--
-- Name: surveys; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.surveys (
    id character varying(64) NOT NULL,
    state integer NOT NULL,
    start_date timestamp without time zone DEFAULT now() NOT NULL,
    end_date timestamp without time zone DEFAULT now() NOT NULL,
    scheme_id character varying(64) NOT NULL,
    locale character varying(16) NOT NULL,
    allow_gen_users boolean NOT NULL,
    suspension_reason character varying(512),
    survey_monkey_url character varying(512),
    support_email character varying(512) DEFAULT 'support@intake24.co.uk'::character varying NOT NULL,
    originating_url character varying(512),
    description character varying(10000),
    feedback_enabled boolean DEFAULT false NOT NULL,
    feedback_style character varying(50) DEFAULT 'default'::character varying NOT NULL,
    submission_notification_url character varying(2048),
    store_user_session_on_server boolean,
    number_of_submissions_for_feedback integer DEFAULT 1 NOT NULL,
    final_page_html character varying(10000),
    maximum_daily_submissions integer DEFAULT 3 NOT NULL,
    minimum_submission_interval integer DEFAULT 600 NOT NULL,
    maximum_total_submissions integer,
    gen_user_key character varying(256),
    auth_url_domain_override character varying(512),
    client_error_report_state boolean DEFAULT true NOT NULL,
    client_error_report_stack_trace boolean DEFAULT true NOT NULL,
    search_sorting_algorithm character varying(10) DEFAULT 'paRules'::character varying NOT NULL,
    search_match_score_weight integer DEFAULT 0 NOT NULL,
    CONSTRAINT surveys_id_characters CHECK (((id)::text ~ '^[A-Za-z0-9_-]+$'::text)),
    CONSTRAINT surveys_maximum_daily_submissions_at_least_one CHECK ((maximum_daily_submissions > 0))
);


--
-- Name: surveys_ux_events_settings; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.surveys_ux_events_settings (
    survey_id character varying(64) NOT NULL,
    enable_search_events boolean NOT NULL,
    enable_associated_foods_events boolean NOT NULL
);


--
-- Name: tools_tasks; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tools_tasks (
    id integer NOT NULL,
    type character varying(64) NOT NULL,
    user_id integer NOT NULL,
    created_at timestamp with time zone NOT NULL,
    started_at timestamp with time zone,
    completed_at timestamp with time zone,
    download_url character varying(1024),
    download_url_expires_at timestamp with time zone,
    progress real,
    successful boolean,
    stack_trace character varying(2048)
);


--
-- Name: tools_tasks_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.tools_tasks_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: tools_tasks_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.tools_tasks_id_seq OWNED BY public.tools_tasks.id;


--
-- Name: user_custom_fields; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_custom_fields (
    id integer NOT NULL,
    user_id integer NOT NULL,
    name character varying(128) NOT NULL,
    value character varying(512) NOT NULL
);


--
-- Name: user_custom_fields_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.user_custom_fields_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: user_custom_fields_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.user_custom_fields_id_seq OWNED BY public.user_custom_fields.id;


--
-- Name: users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.users (
    id integer NOT NULL,
    name character varying(512),
    email character varying(512),
    phone character varying(32),
    simple_name character varying(512),
    email_notifications boolean DEFAULT true NOT NULL,
    sms_notifications boolean DEFAULT true NOT NULL
);


--
-- Name: user_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.user_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: user_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.user_id_seq OWNED BY public.users.id;


--
-- Name: user_notification_schedule; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_notification_schedule (
    id integer NOT NULL,
    user_id integer NOT NULL,
    survey_id character varying(64),
    datetime timestamp with time zone NOT NULL,
    notification_type character varying(100)
);


--
-- Name: user_notification_schedule_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.user_notification_schedule_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: user_notification_schedule_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.user_notification_schedule_id_seq OWNED BY public.user_notification_schedule.id;


--
-- Name: user_passwords; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_passwords (
    user_id integer NOT NULL,
    password_hash character varying(128) NOT NULL,
    password_salt character varying(128) NOT NULL,
    password_hasher character varying(64) NOT NULL
);


--
-- Name: user_permissions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_permissions (
    survey_id character varying(64) NOT NULL,
    user_id integer NOT NULL,
    permission character varying(64) NOT NULL
);


--
-- Name: user_physical_data; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_physical_data (
    user_id integer NOT NULL,
    sex public.sex_enum,
    weight_kg numeric(10,3),
    height_cm numeric(10,3),
    physical_activity_level_id integer,
    birthdate date,
    weight_target public.weight_target_enum,
    CONSTRAINT realistic_height CHECK (((height_cm > (0)::numeric) AND (height_cm < (500)::numeric))),
    CONSTRAINT realistic_weight CHECK (((weight_kg > (0)::numeric) AND (weight_kg < (1000)::numeric)))
);


--
-- Name: user_roles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_roles (
    user_id integer NOT NULL,
    role character varying(64) NOT NULL
);


--
-- Name: user_sessions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_sessions (
    user_id integer NOT NULL,
    survey_id character varying(64) NOT NULL,
    session_data character varying(5000000) NOT NULL,
    created timestamp with time zone NOT NULL
);


--
-- Name: user_survey_aliases; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_survey_aliases (
    user_id integer NOT NULL,
    survey_id character varying(64) NOT NULL,
    user_name character varying(256) NOT NULL,
    url_auth_token character varying(32)
);


--
-- Name: ux_events; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ux_events (
    id integer NOT NULL,
    event_categories character varying(500)[] NOT NULL,
    event_type character varying(500) NOT NULL,
    data json NOT NULL,
    created timestamp with time zone DEFAULT now() NOT NULL,
    session_id uuid NOT NULL,
    user_id integer,
    local_timestamp bigint
);


--
-- Name: ux_events_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.ux_events_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: ux_events_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.ux_events_id_seq OWNED BY public.ux_events.id;


--
-- Name: client_error_reports id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.client_error_reports ALTER COLUMN id SET DEFAULT nextval('public.gwt_client_error_reports_id_seq'::regclass);


--
-- Name: data_export_downloads id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.data_export_downloads ALTER COLUMN id SET DEFAULT nextval('public.data_export_downloads_id_seq'::regclass);


--
-- Name: data_export_scheduled id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.data_export_scheduled ALTER COLUMN id SET DEFAULT nextval('public.data_export_scheduled_id_seq'::regclass);


--
-- Name: data_export_tasks id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.data_export_tasks ALTER COLUMN id SET DEFAULT nextval('public.data_export_tasks_id_seq'::regclass);


--
-- Name: external_test_users id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.external_test_users ALTER COLUMN id SET DEFAULT nextval('public.external_test_users_id_seq'::regclass);


--
-- Name: fixed_food_ranking id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fixed_food_ranking ALTER COLUMN id SET DEFAULT nextval('public.fixed_food_ranking_id_seq'::regclass);


--
-- Name: local_fields id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.local_fields ALTER COLUMN id SET DEFAULT nextval('public.local_fields_id_seq'::regclass);


--
-- Name: local_nutrient_types id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.local_nutrient_types ALTER COLUMN id SET DEFAULT nextval('public.local_nutrient_types_id_seq'::regclass);


--
-- Name: missing_foods id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.missing_foods ALTER COLUMN id SET DEFAULT nextval('public.missing_foods_id_seq'::regclass);


--
-- Name: signin_log id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.signin_log ALTER COLUMN id SET DEFAULT nextval('public.signin_log_id_seq'::regclass);


--
-- Name: survey_submission_custom_fields id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.survey_submission_custom_fields ALTER COLUMN id SET DEFAULT nextval('public.survey_submission_custom_fields_id_seq'::regclass);


--
-- Name: survey_submission_fields id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.survey_submission_fields ALTER COLUMN id SET DEFAULT nextval('public.survey_submission_fields_id_seq'::regclass);


--
-- Name: survey_submission_food_custom_fields id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.survey_submission_food_custom_fields ALTER COLUMN id SET DEFAULT nextval('public.survey_submission_food_custom_fields_id_seq'::regclass);


--
-- Name: survey_submission_foods id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.survey_submission_foods ALTER COLUMN id SET DEFAULT nextval('public.survey_submission_foods_id_seq'::regclass);


--
-- Name: survey_submission_meal_custom_fields id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.survey_submission_meal_custom_fields ALTER COLUMN id SET DEFAULT nextval('public.survey_submission_meal_custom_fields_id_seq'::regclass);


--
-- Name: survey_submission_meals id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.survey_submission_meals ALTER COLUMN id SET DEFAULT nextval('public.survey_submission_meals_id_seq'::regclass);


--
-- Name: survey_submission_missing_foods id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.survey_submission_missing_foods ALTER COLUMN id SET DEFAULT nextval('public.survey_submission_missing_foods_id_seq'::regclass);


--
-- Name: survey_submission_nutrients id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.survey_submission_nutrients ALTER COLUMN id SET DEFAULT nextval('public.survey_submission_nutrients_id_seq'::regclass);


--
-- Name: survey_submission_portion_size_fields id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.survey_submission_portion_size_fields ALTER COLUMN id SET DEFAULT nextval('public.survey_submission_portion_size_fields_id_seq'::regclass);


--
-- Name: tools_tasks id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tools_tasks ALTER COLUMN id SET DEFAULT nextval('public.tools_tasks_id_seq'::regclass);


--
-- Name: user_custom_fields id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_custom_fields ALTER COLUMN id SET DEFAULT nextval('public.user_custom_fields_id_seq'::regclass);


--
-- Name: user_notification_schedule id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_notification_schedule ALTER COLUMN id SET DEFAULT nextval('public.user_notification_schedule_id_seq'::regclass);


--
-- Name: users id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users ALTER COLUMN id SET DEFAULT nextval('public.user_id_seq'::regclass);


--
-- Name: ux_events id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ux_events ALTER COLUMN id SET DEFAULT nextval('public.ux_events_id_seq'::regclass);


--
-- Name: data_export_downloads data_export_downloads_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.data_export_downloads
    ADD CONSTRAINT data_export_downloads_pkey PRIMARY KEY (id);


--
-- Name: data_export_scheduled data_export_scheduled_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.data_export_scheduled
    ADD CONSTRAINT data_export_scheduled_pkey PRIMARY KEY (id);


--
-- Name: data_export_tasks data_export_tasks_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.data_export_tasks
    ADD CONSTRAINT data_export_tasks_pkey PRIMARY KEY (id);


--
-- Name: external_test_users external_test_users_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.external_test_users
    ADD CONSTRAINT external_test_users_pk PRIMARY KEY (id);


--
-- Name: fixed_food_ranking fixed_food_ranking_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fixed_food_ranking
    ADD CONSTRAINT fixed_food_ranking_pkey PRIMARY KEY (id);


--
-- Name: flyway_migrations flyway_migrations_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.flyway_migrations
    ADD CONSTRAINT flyway_migrations_pk PRIMARY KEY (installed_rank);


--
-- Name: gen_user_counters gen_user_counters_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.gen_user_counters
    ADD CONSTRAINT gen_user_counters_pkey PRIMARY KEY (survey_id);


--
-- Name: client_error_reports gwt_client_error_reports_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.client_error_reports
    ADD CONSTRAINT gwt_client_error_reports_pkey PRIMARY KEY (id);


--
-- Name: local_fields local_fields_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.local_fields
    ADD CONSTRAINT local_fields_pk PRIMARY KEY (id);


--
-- Name: local_fields local_fields_unique; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.local_fields
    ADD CONSTRAINT local_fields_unique UNIQUE (locale_id, field_name);


--
-- Name: local_nutrient_types local_nutrient_types_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.local_nutrient_types
    ADD CONSTRAINT local_nutrient_types_pk PRIMARY KEY (id);


--
-- Name: locales locales_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.locales
    ADD CONSTRAINT locales_pk PRIMARY KEY (id);


--
-- Name: missing_foods missing_foods_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.missing_foods
    ADD CONSTRAINT missing_foods_pk PRIMARY KEY (id);


--
-- Name: user_permissions no_duplicate_permissions; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_permissions
    ADD CONSTRAINT no_duplicate_permissions UNIQUE (survey_id, user_id, permission);


--
-- Name: nutrient_types nutrient_types_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.nutrient_types
    ADD CONSTRAINT nutrient_types_pk PRIMARY KEY (id);


--
-- Name: nutrient_units nutrient_units_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.nutrient_units
    ADD CONSTRAINT nutrient_units_pk PRIMARY KEY (id);


--
-- Name: pairwise_associations_co_occurrences pairwise_associations_co_occurrences_copy_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pairwise_associations_co_occurrences
    ADD CONSTRAINT pairwise_associations_co_occurrences_copy_pkey PRIMARY KEY (locale, antecedent_food_code, consequent_food_code);


--
-- Name: pairwise_associations_occurrences pairwise_associations_occurrences_copy_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pairwise_associations_occurrences
    ADD CONSTRAINT pairwise_associations_occurrences_copy_pkey PRIMARY KEY (locale, food_code);


--
-- Name: pairwise_associations_transactions_count pairwise_associations_transactions_count_copy_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pairwise_associations_transactions_count
    ADD CONSTRAINT pairwise_associations_transactions_count_copy_pkey PRIMARY KEY (locale);


--
-- Name: popularity_counters popularity_counters_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.popularity_counters
    ADD CONSTRAINT popularity_counters_pk PRIMARY KEY (food_code);


--
-- Name: schema_version schema_migrations_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.schema_version
    ADD CONSTRAINT schema_migrations_pk PRIMARY KEY (version);


--
-- Name: short_urls short_urls_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.short_urls
    ADD CONSTRAINT short_urls_pkey PRIMARY KEY (long_url);


--
-- Name: short_urls short_urls_unique; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.short_urls
    ADD CONSTRAINT short_urls_unique UNIQUE (short_url);


--
-- Name: signin_log signin_log_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.signin_log
    ADD CONSTRAINT signin_log_pkey PRIMARY KEY (id);


--
-- Name: survey_submission_custom_fields survey_submission_custom_field_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.survey_submission_custom_fields
    ADD CONSTRAINT survey_submission_custom_field_pk PRIMARY KEY (id);


--
-- Name: survey_submission_fields survey_submission_fields_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.survey_submission_fields
    ADD CONSTRAINT survey_submission_fields_pk PRIMARY KEY (id);


--
-- Name: survey_submission_food_custom_fields survey_submission_food_custom_fields_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.survey_submission_food_custom_fields
    ADD CONSTRAINT survey_submission_food_custom_fields_pk PRIMARY KEY (id);


--
-- Name: survey_submission_foods survey_submission_foods_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.survey_submission_foods
    ADD CONSTRAINT survey_submission_foods_pk PRIMARY KEY (id);


--
-- Name: survey_submission_meal_custom_fields survey_submission_meal_custom_fields_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.survey_submission_meal_custom_fields
    ADD CONSTRAINT survey_submission_meal_custom_fields_pk PRIMARY KEY (id);


--
-- Name: survey_submission_meals survey_submission_meals_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.survey_submission_meals
    ADD CONSTRAINT survey_submission_meals_pk PRIMARY KEY (id);


--
-- Name: survey_submission_nutrients survey_submission_nutrients_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.survey_submission_nutrients
    ADD CONSTRAINT survey_submission_nutrients_pk PRIMARY KEY (id);


--
-- Name: survey_submission_portion_size_fields survey_submission_portion_size_fields_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.survey_submission_portion_size_fields
    ADD CONSTRAINT survey_submission_portion_size_fields_pk PRIMARY KEY (id);


--
-- Name: survey_submissions survey_submissions_id_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.survey_submissions
    ADD CONSTRAINT survey_submissions_id_pk PRIMARY KEY (id);


--
-- Name: surveys surveys_id_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.surveys
    ADD CONSTRAINT surveys_id_pk PRIMARY KEY (id);


--
-- Name: surveys_ux_events_settings surveys_ux_events_settings_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.surveys_ux_events_settings
    ADD CONSTRAINT surveys_ux_events_settings_pkey PRIMARY KEY (survey_id);


--
-- Name: tools_tasks tools_tasks_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tools_tasks
    ADD CONSTRAINT tools_tasks_pkey PRIMARY KEY (id);


--
-- Name: user_survey_aliases user_aliases_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_survey_aliases
    ADD CONSTRAINT user_aliases_pkey PRIMARY KEY (survey_id, user_name);


--
-- Name: user_custom_fields user_custom_fields_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_custom_fields
    ADD CONSTRAINT user_custom_fields_pk PRIMARY KEY (id);


--
-- Name: user_physical_data user_info_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_physical_data
    ADD CONSTRAINT user_info_pkey PRIMARY KEY (user_id);


--
-- Name: user_notification_schedule user_notification_schedule_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_notification_schedule
    ADD CONSTRAINT user_notification_schedule_pkey PRIMARY KEY (id);


--
-- Name: user_passwords user_passwords_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_passwords
    ADD CONSTRAINT user_passwords_pkey PRIMARY KEY (user_id);


--
-- Name: user_permissions user_permissions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_permissions
    ADD CONSTRAINT user_permissions_pkey PRIMARY KEY (user_id, permission);


--
-- Name: user_roles user_roles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT user_roles_pkey PRIMARY KEY (user_id, role);


--
-- Name: user_sessions user_sessions_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_sessions
    ADD CONSTRAINT user_sessions_pk PRIMARY KEY (user_id, survey_id);


--
-- Name: user_survey_aliases user_survey_aliases_auth_token_unique; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_survey_aliases
    ADD CONSTRAINT user_survey_aliases_auth_token_unique UNIQUE (url_auth_token);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: ux_events ux_events_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ux_events
    ADD CONSTRAINT ux_events_pkey PRIMARY KEY (id);


--
-- Name: data_export_downloads_task_id_index; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX data_export_downloads_task_id_index ON public.data_export_downloads USING btree (task_id);


--
-- Name: data_export_scheduled_next_run_utc_index; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX data_export_scheduled_next_run_utc_index ON public.data_export_scheduled USING btree (next_run_utc);


--
-- Name: data_export_tasks_purpose_index; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX data_export_tasks_purpose_index ON public.data_export_tasks USING btree (purpose);


--
-- Name: fixed_food_ranking_index; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX fixed_food_ranking_index ON public.fixed_food_ranking USING btree (locale_id, food_code);


--
-- Name: flyway_migrations_s_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX flyway_migrations_s_idx ON public.flyway_migrations USING btree (success);


--
-- Name: gen_user_counters_survey_id_index; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX gen_user_counters_survey_id_index ON public.gen_user_counters USING btree (survey_id);


--
-- Name: missing_foods_user_index; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX missing_foods_user_index ON public.missing_foods USING btree (survey_id, user_id);


--
-- Name: survey_submission_custom_fields_submission_index; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX survey_submission_custom_fields_submission_index ON public.survey_submission_custom_fields USING btree (survey_submission_id);


--
-- Name: survey_submission_fields_food_index; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX survey_submission_fields_food_index ON public.survey_submission_fields USING btree (food_id);


--
-- Name: survey_submission_food_custom_fields_food_index; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX survey_submission_food_custom_fields_food_index ON public.survey_submission_food_custom_fields USING btree (food_id);


--
-- Name: survey_submission_foods_meal_index; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX survey_submission_foods_meal_index ON public.survey_submission_foods USING btree (meal_id);


--
-- Name: survey_submission_meal_custom_fields_meal_id_index; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX survey_submission_meal_custom_fields_meal_id_index ON public.survey_submission_meal_custom_fields USING btree (meal_id);


--
-- Name: survey_submission_nutrients_food_index; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX survey_submission_nutrients_food_index ON public.survey_submission_nutrients USING btree (food_id);


--
-- Name: survey_submission_portion_size_fields_food_index; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX survey_submission_portion_size_fields_food_index ON public.survey_submission_portion_size_fields USING btree (food_id);


--
-- Name: survey_submissions_meals_submission_index; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX survey_submissions_meals_submission_index ON public.survey_submission_meals USING btree (survey_submission_id);


--
-- Name: survey_submissions_survey_id_index; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX survey_submissions_survey_id_index ON public.survey_submissions USING btree (survey_id);


--
-- Name: survey_submissions_user_index; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX survey_submissions_user_index ON public.survey_submissions USING btree (survey_id, user_id);


--
-- Name: survey_submissions_ux_session_index; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX survey_submissions_ux_session_index ON public.survey_submissions USING btree (ux_session_id);


--
-- Name: user_custom_fields_user_id_index; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX user_custom_fields_user_id_index ON public.user_custom_fields USING btree (user_id);


--
-- Name: user_permissions_user_id_index; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX user_permissions_user_id_index ON public.user_permissions USING btree (survey_id, user_id);


--
-- Name: user_survey_aliases_auth_token; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX user_survey_aliases_auth_token ON public.user_survey_aliases USING btree (url_auth_token);


--
-- Name: users_email_case_insensitive_index; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX users_email_case_insensitive_index ON public.users USING btree (lower((email)::text));


--
-- Name: users_simple_name_index; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX users_simple_name_index ON public.users USING btree (simple_name);


--
-- Name: data_export_downloads data_export_downloads_task_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.data_export_downloads
    ADD CONSTRAINT data_export_downloads_task_id_fkey FOREIGN KEY (task_id) REFERENCES public.data_export_tasks(id);


--
-- Name: data_export_scheduled data_export_scheduled_survey_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.data_export_scheduled
    ADD CONSTRAINT data_export_scheduled_survey_id_fkey FOREIGN KEY (survey_id) REFERENCES public.surveys(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: data_export_scheduled data_export_scheduled_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.data_export_scheduled
    ADD CONSTRAINT data_export_scheduled_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: data_export_tasks data_export_tasks_survey_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.data_export_tasks
    ADD CONSTRAINT data_export_tasks_survey_id_fk FOREIGN KEY (survey_id) REFERENCES public.surveys(id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: data_export_tasks data_export_tasks_user_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.data_export_tasks
    ADD CONSTRAINT data_export_tasks_user_id_fk FOREIGN KEY (user_id) REFERENCES public.users(id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: external_test_users external_test_users_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.external_test_users
    ADD CONSTRAINT external_test_users_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: fixed_food_ranking fixed_food_ranking_locale_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fixed_food_ranking
    ADD CONSTRAINT fixed_food_ranking_locale_fk FOREIGN KEY (locale_id) REFERENCES public.locales(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: gen_user_counters gen_user_count_survey_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.gen_user_counters
    ADD CONSTRAINT gen_user_count_survey_id_fk FOREIGN KEY (survey_id) REFERENCES public.surveys(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: local_fields local_fields_locale_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.local_fields
    ADD CONSTRAINT local_fields_locale_fk FOREIGN KEY (locale_id) REFERENCES public.locales(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: local_nutrient_types local_nutrient_types_locale_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.local_nutrient_types
    ADD CONSTRAINT local_nutrient_types_locale_fk FOREIGN KEY (locale_id) REFERENCES public.locales(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: local_nutrient_types local_nutrient_types_nutrient_type_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.local_nutrient_types
    ADD CONSTRAINT local_nutrient_types_nutrient_type_fk FOREIGN KEY (nutrient_type_id) REFERENCES public.nutrient_types(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: locales locales_prototype_locale_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.locales
    ADD CONSTRAINT locales_prototype_locale_id_fk FOREIGN KEY (prototype_locale_id) REFERENCES public.locales(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: missing_foods missing_foods_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.missing_foods
    ADD CONSTRAINT missing_foods_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: nutrient_types nutrient_types_nutrient_unit_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.nutrient_types
    ADD CONSTRAINT nutrient_types_nutrient_unit_fk FOREIGN KEY (unit_id) REFERENCES public.nutrient_units(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: survey_submission_nutrients ssn_nutrient_type_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.survey_submission_nutrients
    ADD CONSTRAINT ssn_nutrient_type_id_fk FOREIGN KEY (nutrient_type_id) REFERENCES public.nutrient_types(id);


--
-- Name: surveys_ux_events_settings survey_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.surveys_ux_events_settings
    ADD CONSTRAINT survey_id_fk FOREIGN KEY (survey_id) REFERENCES public.surveys(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: survey_submission_custom_fields survey_submission_custom_fields_survey_submission_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.survey_submission_custom_fields
    ADD CONSTRAINT survey_submission_custom_fields_survey_submission_id_fk FOREIGN KEY (survey_submission_id) REFERENCES public.survey_submissions(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: survey_submission_fields survey_submission_fields_food_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.survey_submission_fields
    ADD CONSTRAINT survey_submission_fields_food_id_fk FOREIGN KEY (food_id) REFERENCES public.survey_submission_foods(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: survey_submission_food_custom_fields survey_submission_food_custom_fields_food_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.survey_submission_food_custom_fields
    ADD CONSTRAINT survey_submission_food_custom_fields_food_id_fk FOREIGN KEY (food_id) REFERENCES public.survey_submission_foods(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: survey_submission_foods survey_submission_foods_survey_submission_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.survey_submission_foods
    ADD CONSTRAINT survey_submission_foods_survey_submission_id_fk FOREIGN KEY (meal_id) REFERENCES public.survey_submission_meals(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: survey_submission_meal_custom_fields survey_submission_meal_custom_fields_meal_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.survey_submission_meal_custom_fields
    ADD CONSTRAINT survey_submission_meal_custom_fields_meal_id_fk FOREIGN KEY (meal_id) REFERENCES public.survey_submission_meals(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: survey_submission_meals survey_submission_meals_survey_submission_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.survey_submission_meals
    ADD CONSTRAINT survey_submission_meals_survey_submission_id_fk FOREIGN KEY (survey_submission_id) REFERENCES public.survey_submissions(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: survey_submission_missing_foods survey_submission_missing_foods_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.survey_submission_missing_foods
    ADD CONSTRAINT survey_submission_missing_foods_fkey FOREIGN KEY (meal_id) REFERENCES public.survey_submission_meals(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: survey_submission_nutrients survey_submission_nutrients_food_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.survey_submission_nutrients
    ADD CONSTRAINT survey_submission_nutrients_food_id_fk FOREIGN KEY (food_id) REFERENCES public.survey_submission_foods(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: survey_submission_portion_size_fields survey_submission_portion_size_fields_food_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.survey_submission_portion_size_fields
    ADD CONSTRAINT survey_submission_portion_size_fields_food_id_fk FOREIGN KEY (food_id) REFERENCES public.survey_submission_foods(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: survey_submissions survey_submissions_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.survey_submissions
    ADD CONSTRAINT survey_submissions_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: surveys surveys_locale_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.surveys
    ADD CONSTRAINT surveys_locale_fk FOREIGN KEY (locale) REFERENCES public.locales(id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: tools_tasks tools_tasks_user_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tools_tasks
    ADD CONSTRAINT tools_tasks_user_id_fk FOREIGN KEY (user_id) REFERENCES public.users(id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: user_survey_aliases user_aliases_survey_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_survey_aliases
    ADD CONSTRAINT user_aliases_survey_id_fkey FOREIGN KEY (survey_id) REFERENCES public.surveys(id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: user_survey_aliases user_aliases_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_survey_aliases
    ADD CONSTRAINT user_aliases_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: user_custom_fields user_custom_fields_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_custom_fields
    ADD CONSTRAINT user_custom_fields_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: ux_events user_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ux_events
    ADD CONSTRAINT user_id_fk FOREIGN KEY (user_id) REFERENCES public.users(id) ON UPDATE CASCADE;


--
-- Name: user_notification_schedule user_notification_schedule_surveys_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_notification_schedule
    ADD CONSTRAINT user_notification_schedule_surveys_fk FOREIGN KEY (survey_id) REFERENCES public.surveys(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: user_notification_schedule user_notification_schedule_users_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_notification_schedule
    ADD CONSTRAINT user_notification_schedule_users_fk FOREIGN KEY (user_id) REFERENCES public.users(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: user_passwords user_passwords_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_passwords
    ADD CONSTRAINT user_passwords_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: user_permissions user_permissions_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_permissions
    ADD CONSTRAINT user_permissions_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: user_roles user_roles_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT user_roles_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: user_sessions user_sessions_surveys_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_sessions
    ADD CONSTRAINT user_sessions_surveys_fk FOREIGN KEY (survey_id) REFERENCES public.surveys(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: user_sessions user_sessions_users_pk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_sessions
    ADD CONSTRAINT user_sessions_users_pk FOREIGN KEY (user_id) REFERENCES public.users(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: user_physical_data users_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_physical_data
    ADD CONSTRAINT users_id_fk FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

