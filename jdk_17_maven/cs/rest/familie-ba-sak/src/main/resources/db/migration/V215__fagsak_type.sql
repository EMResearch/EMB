ALTER TABLE FAGSAK ADD COLUMN type VARCHAR(50) DEFAULT 'NORMAL' NOT NULL;
ALTER TABLE FAGSAK ADD COLUMN fk_institusjon_id BIGINT;
ALTER TABLE FAGSAK
    ADD FOREIGN KEY (fk_institusjon_id) REFERENCES INSTITUSJON (ID);

UPDATE FAGSAK SET type = 'BARN_ENSLIG_MINDREÅRLIG' WHERE eier = 'BARN';

CREATE UNIQUE INDEX uidx_fagsak_type_aktoer_institusjon_ikke_arkivert ON fagsak(type, fk_aktoer_id, fk_institusjon_id)
    WHERE fagsak.fk_institusjon_id IS NOT NULL
        AND arkivert = false;

CREATE UNIQUE INDEX uidx_fagsak_type_aktoer_ikke_arkivert ON fagsak(type, fk_aktoer_id)
    WHERE fagsak.fk_institusjon_id IS NULL
        AND arkivert = false;
