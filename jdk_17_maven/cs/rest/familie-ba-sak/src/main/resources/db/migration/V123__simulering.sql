CREATE TABLE vedtak_simulering_mottaker (
    id              BIGINT PRIMARY KEY,
    fk_vedtak_id    BIGINT REFERENCES vedtak (id),
    mottaker_nummer VARCHAR(50),
    mottaker_type   VARCHAR(50)
);

CREATE SEQUENCE vedtak_simulering_mottaker_seq INCREMENT BY 50 START WITH 1000000 NO CYCLE;
CREATE INDEX ON vedtak_simulering_mottaker (fk_vedtak_id);

CREATE TABLE vedtak_simulering_postering (
    id                               BIGINT PRIMARY KEY,
    fk_vedtak_simulering_mottaker_id BIGINT REFERENCES vedtak_simulering_mottaker (id),
    fag_omraade_kode                 VARCHAR(50),
    fom                              TIMESTAMP(3),
    tom                              TIMESTAMP(3),
    betaling_type                    VARCHAR(50),
    belop                            BIGINT,
    postering_type                   VARCHAR(50),
    forfallsdato                     TIMESTAMP(3),
    uten_inntrekk                    BOOLEAN
);

CREATE SEQUENCE vedtak_simulering_postering_seq INCREMENT BY 50 START WITH 1000000 NO CYCLE;
CREATE INDEX ON vedtak_simulering_postering (fk_vedtak_simulering_mottaker_id);