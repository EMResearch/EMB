ALTER TABLE vedtak_simulering_mottaker
ADD COLUMN opprettet_av VARCHAR(512) DEFAULT 'VL'::CHARACTER VARYING NOT NULL;
ALTER TABLE vedtak_simulering_mottaker
ADD COLUMN opprettet_tid TIMESTAMP(3) DEFAULT LOCALTIMESTAMP NOT NULL;
ALTER TABLE vedtak_simulering_mottaker
ADD COLUMN endret_av VARCHAR(512);
ALTER TABLE vedtak_simulering_mottaker
ADD COLUMN endret_tid TIMESTAMP(3);

ALTER TABLE vedtak_simulering_postering
ADD COLUMN opprettet_av VARCHAR(512) DEFAULT 'VL'::CHARACTER VARYING NOT NULL;
ALTER TABLE vedtak_simulering_postering
ADD COLUMN opprettet_tid TIMESTAMP(3) DEFAULT LOCALTIMESTAMP NOT NULL;
ALTER TABLE vedtak_simulering_postering
ADD COLUMN endret_av VARCHAR(512);
ALTER TABLE vedtak_simulering_postering
ADD COLUMN endret_tid TIMESTAMP(3);