ALTER TABLE TOTRINNSKONTROLL
    ADD COLUMN SAKSBEHANDLER_ID VARCHAR DEFAULT 'ukjent' NOT NULL;

ALTER TABLE TOTRINNSKONTROLL
    ADD COLUMN BESLUTTER_ID VARCHAR;
