ALTER TABLE PO_PERSON ADD COLUMN medlemskap varchar DEFAULT 'UKJENT' NOT NULL;

CREATE TABLE PO_STATSBORGERSKAP
(
    id                bigint primary key,
    fk_po_person_id   bigint references PO_PERSON          NOT NULL,
    landkode          VARCHAR(3) DEFAULT 'XUK'             NOT NULL,
    fom               DATE,
    tom               DATE,
    opprettet_av      VARCHAR  DEFAULT 'VL'                NOT NULL,
    opprettet_tid     TIMESTAMP DEFAULT current_timestamp  NOT NULL,
    endret_av         VARCHAR,
    endret_tid        TIMESTAMP(3),
    versjon           bigint       DEFAULT 0               NOT NULL
);

CREATE SEQUENCE PO_STATSBORGERSKAP_SEQ INCREMENT BY 50 START WITH 1000000 NO CYCLE;
ALTER TABLE PO_PERSON ADD COLUMN statsborgerskap_id  bigint  REFERENCES PO_STATSBORGERSKAP;
