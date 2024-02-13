ALTER TABLE VILKAR_RESULTAT
    ADD COLUMN er_eksplisitt_avslag_paa_soknad BOOLEAN;

UPDATE VILKAR_RESULTAT
SET er_eksplisitt_avslag_paa_soknad = false
WHERE resultat = 'IKKE_OPPFYLT';