CREATE TABLE IF NOT EXISTS po_sivilstand
(
    id              BIGINT PRIMARY KEY,
    fk_po_person_id BIGINT REFERENCES po_person (id)    NOT NULL,
    fom             DATE,
    type            VARCHAR                             NOT NULL,
    opprettet_av    VARCHAR      DEFAULT 'VL'           NOT NULL,
    opprettet_tid   TIMESTAMP(3) DEFAULT localtimestamp NOT NULL,
    endret_av       VARCHAR,
    endret_tid      TIMESTAMP(3),
    versjon         BIGINT       DEFAULT 0              NOT NULL
);

CREATE SEQUENCE po_sivilstand_seq INCREMENT BY 50 START WITH 1000000 NO CYCLE;

INSERT INTO po_sivilstand (ID, fk_po_person_id, fom, type, opprettet_av, opprettet_tid)
    (SELECT nextval('po_sivilstand_seq'), id, null, sivilstand, opprettet_av, opprettet_tid
     FROM po_person
     WHERE sivilstand is not null);