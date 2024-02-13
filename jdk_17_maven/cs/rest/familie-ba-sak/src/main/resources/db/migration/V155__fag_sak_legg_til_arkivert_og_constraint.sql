ALTER TABLE fagsak_person
    ADD COLUMN arkivert     BOOLEAN      DEFAULT FALSE       NOT NULL;

ALTER TABLE fagsak
    ADD COLUMN arkivert     BOOLEAN      DEFAULT FALSE       NOT NULL;