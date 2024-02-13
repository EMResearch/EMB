create table GR_PERSONOPPLYSNINGER
(
    id                 bigint                                       primary key,
    fk_behandling_id   bigint       references BEHANDLING (id)      not null,
    versjon            bigint       default 0                       not null,
    opprettet_av       VARCHAR(20)  default 'VL'                    not null,
    opprettet_tid      TIMESTAMP(3) default localtimestamp          not null,
    endret_av          VARCHAR(20),
    endret_tid         TIMESTAMP(3),
    aktiv              boolean      default true                    not null
);

CREATE SEQUENCE GR_PERSONOPPLYSNINGER_SEQ INCREMENT BY 50 START WITH 1000000 NO CYCLE;
create index on GR_PERSONOPPLYSNINGER (fk_behandling_id);

CREATE UNIQUE INDEX UIDX_GR_PERSONOPPLYSNINGER_01
    ON GR_PERSONOPPLYSNINGER
        (
         (CASE
              WHEN aktiv = true
                  THEN fk_behandling_id
              ELSE NULL END),
         (CASE
              WHEN aktiv = true
                  THEN aktiv
              ELSE NULL END)
            );

CREATE TABLE PO_PERSON
(
    id                          bigint primary key                            NOT NULL,
    fk_gr_personopplysninger_id bigint references gr_personopplysninger (id)  NOT NULL,
    person_ident                VARCHAR(50)                                   NOT NULL,
    type                        varchar(10)                                   NOT NULL,
    opprettet_av                varchar(20)  DEFAULT 'VL'                     NOT NULL,
    opprettet_tid               TIMESTAMP(3) DEFAULT current_timestamp        NOT NULL,
    endret_av                   varchar(20),
    versjon                     bigint       DEFAULT 0                        NOT NULL,
    endret_tid                  TIMESTAMP(3)
);

CREATE SEQUENCE PO_PERSON_SEQ INCREMENT BY 50 START WITH 1000000 NO CYCLE;
create index on PO_PERSON (fk_gr_personopplysninger_id);
