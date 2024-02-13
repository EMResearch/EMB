-- vedtak_simulering_mottaker til br_simulering_mottaker
ALTER TABLE vedtak_simulering_mottaker
    RENAME TO okonomi_simulering_mottaker;

ALTER TABLE okonomi_simulering_mottaker
    ADD COLUMN fk_behandling_id BIGINT REFERENCES behandling (id);

-- Set fk_behandling_id til behandlings id som er relatert til vedtaket.
UPDATE okonomi_simulering_mottaker
SET fk_behandling_id = behandling.id
FROM behandling
         JOIN vedtak
              ON behandling.id = vedtak.fk_behandling_id
WHERE okonomi_simulering_mottaker.fk_vedtak_id = vedtak.id
  AND vedtak.aktiv = TRUE;

ALTER TABLE okonomi_simulering_mottaker
    DROP COLUMN fk_vedtak_id;

ALTER SEQUENCE vedtak_simulering_mottaker_seq RENAME TO okonomi_simulering_mottaker_seq;

-- vedtak_simulering_postering til okonomi_simulering_postering
ALTER TABLE vedtak_simulering_postering
    RENAME TO okonomi_simulering_postering;

ALTER TABLE okonomi_simulering_postering
    RENAME COLUMN fk_vedtak_simulering_mottaker_id TO fk_okonomi_simulering_mottaker_id;

ALTER SEQUENCE vedtak_simulering_postering_seq RENAME TO okonomi_simulering_postering_seq;