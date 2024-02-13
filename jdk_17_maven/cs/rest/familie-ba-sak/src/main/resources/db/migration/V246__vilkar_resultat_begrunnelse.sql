ALTER TABLE vilkar_resultat
    ADD COLUMN IF NOT EXISTS resultat_begrunnelse VARCHAR DEFAULT NULL;