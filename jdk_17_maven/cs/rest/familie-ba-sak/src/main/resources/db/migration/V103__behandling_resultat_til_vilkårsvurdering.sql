ALTER TABLE BEHANDLING_RESULTAT
    RENAME TO VILKAARSVURDERING;
ALTER SEQUENCE BEHANDLING_RESULTAT_SEQ RENAME TO VILKAARSVURDERING_SEQ;

ALTER TABLE PERSON_RESULTAT
    RENAME COLUMN FK_BEHANDLING_RESULTAT_ID TO FK_VILKAARSVURDERING_ID;

