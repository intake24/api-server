WITH a AS (
  INSERT INTO user_survey_aliases (user_id, survey_id, user_name) VALUES (nextval('user_id_seq'), {survey_id}, {user_name})
  ON CONFLICT(survey_id, user_name) DO UPDATE SET user_id = user_survey_aliases.user_id RETURNING user_id
)
INSERT INTO users (id, name, email, phone, simple_name)
  SELECT user_id, {name}, {email}, {phone}, {simple_name} FROM a
  ON CONFLICT (id) DO UPDATE
   SET name = EXCLUDED.name,
       email = EXCLUDED.email,
       phone = EXCLUDED.phone,
       simple_name = EXCLUDED.simple_name
