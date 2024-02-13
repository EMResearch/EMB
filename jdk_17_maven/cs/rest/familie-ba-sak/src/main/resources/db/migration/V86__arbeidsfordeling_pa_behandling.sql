CREATE TABLE ARBEIDSFORDELING_PA_BEHANDLING
(
    id                     BIGINT PRIMARY KEY,
    fk_behandling_id       BIGINT UNIQUE NOT NULL,
    behandlende_enhet_id   VARCHAR       NOT NULL,
    behandlende_enhet_navn VARCHAR       NOT NULL,
    manuelt_overstyrt      BOOLEAN       NOT NULL
);

CREATE SEQUENCE ARBEIDSFORDELING_PA_BEHANDLING_SEQ INCREMENT BY 50 START WITH 1000000 NO CYCLE;
