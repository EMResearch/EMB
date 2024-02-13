ALTER TABLE behandling_soknadsinfo
    ADD COLUMN IF NOT EXISTS er_digital BOOLEAN DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS journalpost_id VARCHAR DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS brevkode VARCHAR DEFAULT NULL;

CREATE INDEX journalpost_id_idx ON behandling_soknadsinfo (journalpost_id);
