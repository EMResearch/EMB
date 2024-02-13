CREATE TABLE PO_BOSTEDSADRESSE
(
    id                          bigint primary key,
    type                        varchar(20)                                   NOT NULL,
    bostedskommune              varchar(20),
    husnummer                   varchar(4),
    husbokstav                  varchar(2),
    bruksenhetsnummer           varchar(10),
    adressenavn                 varchar(30),
    kommunenummer               varchar(10),
    tilleggsnavn                varchar(30),
    postnummer                  varchar(5),
    opprettet_av                varchar(20)  DEFAULT 'VL'                     NOT NULL,
    opprettet_tid               TIMESTAMP(3) DEFAULT current_timestamp        NOT NULL,
    endret_av                   varchar(20),
    versjon                     bigint       DEFAULT 0                        NOT NULL,
    endret_tid                  TIMESTAMP(3)
);

CREATE SEQUENCE PO_BOSTEDSADRESSE_SEQ INCREMENT BY 50 START WITH 1000000 NO CYCLE;

ALTER TABLE PO_PERSON ADD COLUMN bostedsadresse_id    bigint  REFERENCES PO_BOSTEDSADRESSE;