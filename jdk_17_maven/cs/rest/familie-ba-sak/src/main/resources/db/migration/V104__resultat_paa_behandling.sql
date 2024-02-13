/* De gamle sakene i produksjon vil få resultat IKKE_VURDERT og vil være feil til vi migrerer over resultat */
ALTER TABLE BEHANDLING
    ADD COLUMN resultat VARCHAR DEFAULT 'IKKE_VURDERT' NOT NULL;

