CREATE TABLE ANNEN_VURDERING
(
    ID                    BIGINT PRIMARY KEY,
    FK_PERSON_RESULTAT_ID BIGINT REFERENCES person_resultat (id) NOT NULL,
    RESULTAT              VARCHAR                                NOT NULL,
    TYPE                  VARCHAR                                NOT NULL,
    BEGRUNNELSE           TEXT,
    VERSJON               BIGINT       DEFAULT 0                 NOT NULL,
    OPPRETTET_AV          VARCHAR      DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID         TIMESTAMP(3) DEFAULT localtimestamp    NOT NULL,
    ENDRET_AV             VARCHAR,
    ENDRET_TID            TIMESTAMP(3)
);

CREATE SEQUENCE ANNEN_VURDERING_SEQ INCREMENT BY 50 START WITH 1000000 NO CYCLE;
