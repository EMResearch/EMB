CREATE TABLE PO_ARBEIDSFORHOLD
(
    id                  bigint primary key,
    fk_po_person_id     bigint references PO_PERSON          NOT NULL,
    arbeidsgiver_id     VARCHAR,
    arbeidsgiver_type   VARCHAR,
    fom                 DATE,
    tom                 DATE,
    opprettet_av        VARCHAR  DEFAULT 'VL'                NOT NULL,
    opprettet_tid       TIMESTAMP(3) DEFAULT localtimestamp  NOT NULL,
    endret_av           VARCHAR,
    endret_tid          TIMESTAMP(3),
    versjon             bigint       DEFAULT 0               NOT NULL
);

CREATE SEQUENCE PO_ARBEIDSFORHOLD_SEQ INCREMENT BY 50 START WITH 1000000 NO CYCLE;
