ALTER TABLE vedtak_simulering_mottaker
    ADD COLUMN versjon BIGINT DEFAULT 0;

ALTER TABLE vedtak_simulering_postering
    ADD COLUMN versjon BIGINT DEFAULT 0;