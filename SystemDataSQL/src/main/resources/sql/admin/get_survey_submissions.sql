SELECT id, ss.survey_id, ss.user_id, a.user_name, start_time, end_time, submission_time, log,
    ARRAY(SELECT ARRAY[name, value] FROM survey_submission_custom_fields AS cf WHERE cf.survey_submission_id = ss.id) AS submission_custom_fields,
    ARRAY(SELECT ARRAY[name, value] FROM user_custom_fields AS ucf WHERE ucf.user_id = ss.user_id) AS user_custom_fields
FROM survey_submissions AS ss
LEFT JOIN user_survey_aliases AS a ON ss.user_id  = a.user_id
WHERE ss.survey_id={survey_id}
AND ({time_from}::timestamp with time zone IS NULL OR submission_time>{time_from})
AND ({time_to}::timestamp with time zone IS NULL OR submission_time<{time_to})
AND ({respondent_id} IS NULL OR ss.user_id = {respondent_id})
ORDER BY submission_time ASC
OFFSET {offset} LIMIT {limit}
