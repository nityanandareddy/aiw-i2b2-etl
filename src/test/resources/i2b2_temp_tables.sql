--------------------------------------------------------
-- DDL FOR TEMPORARY TABLES
--------------------------------------------------------

CREATE LOCAL TEMPORARY TABLE IF NOT EXISTS EK_TEMP_UNIQUE_IDS (UNIQUE_ID VARCHAR2(700)) ON COMMIT DELETE ROWS;