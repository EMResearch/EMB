DELETE FROM vilkar_resultat WHERE vilkar='STØNADSPERIODE';
INSERT INTO vilkar_resultat (id, vilkar, begrunnelse, endret_av, endret_tid, fk_periode_resultat_id, opprettet_av, opprettet_tid, regel_input, regel_output, resultat)
    (SELECT nextval('VILKAR_RESULTAT_SEQ'), 'BOR_MED_SØKER', vr.begrunnelse, vr.endret_av, vr.endret_tid, vr.fk_periode_resultat_id, vr.opprettet_av, vr.opprettet_tid, vr.regel_input, vr.regel_output, vr.resultat FROM vilkar_resultat vr WHERE vr.vilkar='UNDER_18_ÅR_OG_BOR_MED_SØKER');
UPDATE vilkar_resultat SET vilkar='UNDER_18_ÅR' WHERE vilkar='UNDER_18_ÅR_OG_BOR_MED_SØKER';
