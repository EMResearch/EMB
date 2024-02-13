CREATE TABLE ENDRET_UTBETALING_ANDEL
(
    ID                  BIGINT PRIMARY KEY,
    FK_BEHANDLING_ID    BIGINT REFERENCES BEHANDLING (ID)   NOT NULL,
    FK_PO_PERSON_ID     BIGINT REFERENCES PO_PERSON (ID)    NOT NULL,
    FOM                 TIMESTAMP(3)                        NOT NULL,
    TOM                 TIMESTAMP(3)                        NOT NULL,
    PROSENT             NUMERIC                             NOT NULL,
    AARSAK              VARCHAR                             NOT NULL,
    BEGRUNNELSE         TEXT                                NOT NULL,
    VERSJON             BIGINT       DEFAULT 0              NOT NULL,
    OPPRETTET_AV        VARCHAR      DEFAULT 'VL'           NOT NULL,
    OPPRETTET_TID       TIMESTAMP(3) DEFAULT localtimestamp NOT NULL,
    ENDRET_AV           VARCHAR,
    ENDRET_TID          TIMESTAMP(3)
);

CREATE SEQUENCE ENDRET_UTBETALING_ANDEL_SEQ INCREMENT BY 50 START WITH 1000000 NO CYCLE;
CREATE INDEX ON ENDRET_UTBETALING_ANDEL (FK_BEHANDLING_ID);

