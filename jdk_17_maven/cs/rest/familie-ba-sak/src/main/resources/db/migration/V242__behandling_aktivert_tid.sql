ALTER TABLE behandling
    ADD COLUMN aktivert_tid TIMESTAMP(3) DEFAULT localtimestamp NOT NULL;