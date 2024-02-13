CREATE TABLE VEDTAKSPERIODE
(
    id            BIGINT PRIMARY KEY,
    fk_vedtak_id  BIGINT REFERENCES vedtak (id),

    fom           TIMESTAMP DEFAULT NULL,
    tom           TIMESTAMP DEFAULT NULL,
    type          VARCHAR                                   NOT NULL,

    opprettet_av  VARCHAR   DEFAULT 'VL'::CHARACTER VARYING NOT NULL,
    opprettet_tid TIMESTAMP DEFAULT LOCALTIMESTAMP          NOT NULL,
    endret_av     VARCHAR,
    endret_tid    TIMESTAMP(3),
    versjon       BIGINT    DEFAULT 0,
    UNIQUE (fk_vedtak_id, fom, tom, type)
);

CREATE SEQUENCE vedtaksperiode_seq INCREMENT BY 50 START WITH 1000000 NO CYCLE;
CREATE INDEX ON vedtaksperiode (fk_vedtak_id);

CREATE TABLE VEDTAKSBEGRUNNELSE
(
    id                               BIGINT PRIMARY KEY,
    fk_vedtaksperiode_id             BIGINT REFERENCES vedtaksperiode (id),

    vedtak_begrunnelse_spesifikasjon VARCHAR         NOT NULL,
    person_identer                   TEXT DEFAULT '' NOT NULL
);

CREATE SEQUENCE vedtaksbegrunnelse_seq INCREMENT BY 50 START WITH 1000000 NO CYCLE;
CREATE INDEX ON vedtaksbegrunnelse (fk_vedtaksperiode_id);

CREATE TABLE VEDTAKSBEGRUNNELSE_FRITEKST
(
    id                   BIGINT PRIMARY KEY,
    fk_vedtaksperiode_id BIGINT REFERENCES vedtaksperiode (id),

    fritekst             TEXT DEFAULT '' NOT NULL
);

CREATE SEQUENCE vedtaksbegrunnelse_fritekst_seq INCREMENT BY 50 START WITH 1000000 NO CYCLE;
CREATE INDEX ON vedtaksbegrunnelse_fritekst (fk_vedtaksperiode_id);