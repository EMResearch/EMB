CREATE TABLE eos_begrunnelse
(
    id                   BIGINT  NOT NULL PRIMARY KEY,
    fk_vedtaksperiode_id BIGINT REFERENCES vedtaksperiode ON DELETE CASCADE,
    begrunnelse          VARCHAR NOT NULL
);

CREATE INDEX eos_begrunnelse_fk_vedtaksperiode_id_idx
    ON eos_begrunnelse (fk_vedtaksperiode_id);

CREATE SEQUENCE eos_begrunnelse_seq INCREMENT BY 50 START WITH 1000000 NO CYCLE;