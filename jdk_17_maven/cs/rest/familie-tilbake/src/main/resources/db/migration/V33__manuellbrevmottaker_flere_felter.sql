ALTER TABLE manuell_brevmottaker ADD COLUMN ident  VARCHAR;
ALTER TABLE manuell_brevmottaker ADD COLUMN org_nr VARCHAR;
ALTER TABLE manuell_brevmottaker ALTER COLUMN adresselinje_1 DROP NOT NULL;
ALTER TABLE manuell_brevmottaker ALTER COLUMN postnummer DROP NOT NULL;
ALTER TABLE manuell_brevmottaker ALTER COLUMN poststed DROP NOT NULL;
ALTER TABLE manuell_brevmottaker ALTER COLUMN landkode DROP NOT NULL;

