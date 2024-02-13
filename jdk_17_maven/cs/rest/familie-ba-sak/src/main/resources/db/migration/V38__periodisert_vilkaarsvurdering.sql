alter table behandling drop column resultat;

CREATE TABLE BEHANDLING_RESULTAT
(
    ID                            BIGINT PRIMARY KEY,
    FK_BEHANDLING_ID              BIGINT REFERENCES behandling (id)   NOT NULL,
    AKTIV                         BOOLEAN      DEFAULT TRUE           NOT NULL,
    VERSJON                       BIGINT       DEFAULT 0              NOT NULL,
    OPPRETTET_AV                  VARCHAR      DEFAULT 'VL'           NOT NULL,
    OPPRETTET_TID                 TIMESTAMP(3) DEFAULT localtimestamp NOT NULL,
    ENDRET_AV                     VARCHAR,
    ENDRET_TID                    TIMESTAMP(3)
);
ALTER SEQUENCE SAMLET_VILKAR_RESULTAT_SEQ RENAME TO BEHANDLING_RESULTAT_SEQ;
insert into BEHANDLING_RESULTAT (id, fk_behandling_id, aktiv, versjon, opprettet_av, opprettet_tid, endret_av, endret_tid)
select id, fk_behandling_id, aktiv, versjon, opprettet_av, opprettet_tid, endret_av, endret_tid from samlet_vilkar_resultat;

CREATE TABLE PERIODE_RESULTAT
(
    ID                            BIGINT PRIMARY KEY,
    FK_BEHANDLING_RESULTAT_ID     BIGINT REFERENCES BEHANDLING_RESULTAT (id)   NOT NULL,
    VERSJON                       BIGINT       DEFAULT 0              NOT NULL,
    OPPRETTET_AV                  VARCHAR      DEFAULT 'VL'           NOT NULL,
    OPPRETTET_TID                 TIMESTAMP(3) DEFAULT localtimestamp NOT NULL,
    ENDRET_AV                     VARCHAR,
    ENDRET_TID                    TIMESTAMP(3),
    PERSON_IDENT                  VARCHAR,
    PERIODE_FOM                   TIMESTAMP(3),
    PERIODE_TOM                   TIMESTAMP(3)
);
CREATE SEQUENCE PERIODE_RESULTAT_SEQ INCREMENT BY 50 START WITH 1000000 NO CYCLE;

alter table vilkar_resultat add column tmp_person_ident varchar;
update vilkar_resultat vr set tmp_person_ident=(select distinct p.person_ident from po_person p where p.id = vr.fk_person_id limit 1);

insert into PERIODE_RESULTAT(id, fk_behandling_resultat_id, person_ident)
select nextval('PERIODE_RESULTAT_SEQ'), samlet_vilkar_resultat_id, tmp_person_ident from vilkar_resultat
group by samlet_vilkar_resultat_id, tmp_person_ident;

update PERIODE_RESULTAT pr
set VERSJON= br.VERSJON, OPPRETTET_AV=br.OPPRETTET_AV,  OPPRETTET_TID=br.OPPRETTET_TID,  ENDRET_AV=br.ENDRET_AV, ENDRET_TID=br.ENDRET_TID
from BEHANDLING_RESULTAT br
where pr.FK_BEHANDLING_RESULTAT_ID = br.ID;

alter table vilkar_resultat add column fk_periode_resultat_id BIGINT REFERENCES PERIODE_RESULTAT (id);
update vilkar_resultat vr set fk_periode_resultat_id=(select pr.id from PERIODE_RESULTAT pr where vr.samlet_vilkar_resultat_id = pr.FK_BEHANDLING_RESULTAT_ID and vr.tmp_person_ident = pr.person_ident limit 1);
ALTER TABLE vilkar_resultat ALTER COLUMN fk_periode_resultat_id SET NOT NULL;

drop table samlet_vilkar_resultat cascade;
alter table vilkar_resultat
    drop column samlet_vilkar_resultat_id,
    drop column fk_person_id,
    drop column tmp_person_ident;