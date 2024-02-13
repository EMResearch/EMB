alter table PERSONIDENT drop constraint PERSONIDENT_PKEY;

alter table PERSONIDENT add primary key (FOEDSELSNUMMER);

drop sequence PERSONIDENT_SEQ;

alter table PERSONIDENT rename column AKTOER_ID to FK_AKTOER_ID;
alter table PERSONIDENT drop column ID;

create table AKTOER
(
    AKTOER_ID           VARCHAR      PRIMARY KEY,
    VERSJON             BIGINT       DEFAULT 0              NOT NULL,
    OPPRETTET_AV        VARCHAR      DEFAULT 'VL'           NOT NULL,
    OPPRETTET_TID       TIMESTAMP(3) DEFAULT localtimestamp NOT NULL,
    ENDRET_AV           VARCHAR,
    ENDRET_TID          TIMESTAMP(3)
);

alter table PERSONIDENT
    add constraint FK_PERSONIDENT foreign key (FK_AKTOER_ID) references AKTOER (AKTOER_ID);