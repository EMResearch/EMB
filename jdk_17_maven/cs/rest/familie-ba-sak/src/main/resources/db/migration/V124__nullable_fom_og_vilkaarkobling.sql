ALTER TABLE VEDTAK_BEGRUNNELSE
    ALTER COLUMN fom DROP NOT NULL;
ALTER TABLE VEDTAK_BEGRUNNELSE
    ADD COLUMN fk_vilkar_resultat_id BIGINT REFERENCES vilkar_resultat (id);