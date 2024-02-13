ALTER TABLE FAGSAK ADD COLUMN eier VARCHAR(50) DEFAULT 'OMSORGSPERSON' NOT NULL;

CREATE UNIQUE INDEX uidx_fagsak_eier_aktoer_ikke_arkivert ON fagsak(eier, fk_aktoer_id)
    WHERE arkivert = false;