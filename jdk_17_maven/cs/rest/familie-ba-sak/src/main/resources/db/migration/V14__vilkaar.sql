create table SAMLET_VILKAR_RESULTAT
(
    ID            bigint primary key,
    VERSJON       bigint       default 0              not null,
    OPPRETTET_AV  VARCHAR(20)  default 'VL'           not null,
    OPPRETTET_TID TIMESTAMP(3) default localtimestamp not null,
    ENDRET_AV     VARCHAR(20),
    ENDRET_TID    TIMESTAMP(3)
);
CREATE SEQUENCE SAMLET_VILKAR_RESULTAT_SEQ INCREMENT BY 50 START WITH 1000000 NO CYCLE;

create table VILKAR_RESULTAT
(
    ID                        bigint primary key,
    SAMLET_VILKAR_RESULTAT_ID bigint references SAMLET_VILKAR_RESULTAT (id) not null,
    FK_PERSON_ID              bigint references po_person (id)              not null,
    VILKAR                    VARCHAR(50)                                   not null,
    UTFALL                    VARCHAR(50)                                   not null,
    REGEL_INPUT               text,
    REGEL_OUTPUT              text,
    VERSJON                   bigint       default 0                        not null,
    OPPRETTET_AV              VARCHAR(20)  default 'VL'                     not null,
    OPPRETTET_TID             TIMESTAMP(3) default localtimestamp           not null,
    ENDRET_AV                 VARCHAR(20),
    ENDRET_TID                TIMESTAMP(3)
);
CREATE SEQUENCE VILKAR_RESULTAT_SEQ INCREMENT BY 50 START WITH 1000000 NO CYCLE;

alter table behandling add column samlet_vilkar_resultat_id bigint references SAMLET_VILKAR_RESULTAT (id) default null;