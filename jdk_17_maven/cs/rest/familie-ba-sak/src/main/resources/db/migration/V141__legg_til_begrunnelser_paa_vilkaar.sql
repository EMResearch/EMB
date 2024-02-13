ALTER TABLE vilkar_resultat
    ADD COLUMN vedtak_begrunnelse_spesifikasjoner TEXT DEFAULT '';

UPDATE vilkar_resultat vr
SET vedtak_begrunnelse_spesifikasjoner=(SELECT string_agg(vb.begrunnelse, ';')
                                        FROM vedtak_begrunnelse vb
                                        WHERE vb.fk_vilkar_resultat_id = vr.id) WHERE vr.er_eksplisitt_avslag_paa_soknad=true;