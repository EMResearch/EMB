CREATE TABLE PO_OPPHOLD
(
    id                bigint primary key,
    fk_po_person_id   bigint references PO_PERSON          NOT NULL,
    type              VARCHAR                              NOT NULL,
    fom               DATE,
    tom               DATE,
    opprettet_av      VARCHAR  DEFAULT 'VL'                NOT NULL,
    opprettet_tid     TIMESTAMP(3) default localtimestamp  NOT NULL,
    endret_av         VARCHAR,
    endret_tid        TIMESTAMP(3),
    versjon           bigint       DEFAULT 0               NOT NULL
);

CREATE SEQUENCE PO_OPPHOLD_SEQ INCREMENT BY 50 START WITH 1000000 NO CYCLE;