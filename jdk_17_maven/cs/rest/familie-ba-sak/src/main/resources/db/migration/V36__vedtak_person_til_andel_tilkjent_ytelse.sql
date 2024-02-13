-- Skal endre navn på VEDTAK_PERSON til ANDEL_TILKJENT_YTELSE og peke på BEHANDLING i stedet for VEDTAK
-- Må passe på at en kjørende instans fortsatt har VEDTAK_PERSON å skrive til under migrering
-- Derfor opprettes ny tabell ANDEL_TILKJENT_YTELSE og migrerer eksisterende data i VEDTAK_PERSON
-- og legger på en trigger som oppdaterer ANDEL_TILKJENT_YTELSE når det skjer endringer i VEDTAK_PERSON
-- Del 2 blir å fjerne VEDTAK_PERSON og trigger

create table ANDEL_TILKJENT_YTELSE
(
    id                      bigint                                                  primary key,
    fk_behandling_id        bigint       references BEHANDLING (id)                 not null,
    fk_person_id            bigint       references po_person (id)                  not null,
    versjon                 bigint       default 0                                  not null,
    opprettet_av            VARCHAR(512) default 'VL'                               not null,
    opprettet_tid           TIMESTAMP(3) default localtimestamp                     not null,
    stonad_fom              TIMESTAMP(3)                                            not null,
    stonad_tom              TIMESTAMP(3)                                            not null,
    type                    varchar(50)                                             not null,
    belop                   numeric,
    endret_av               VARCHAR(512),
    endret_tid              TIMESTAMP(3)
);

CREATE SEQUENCE ANDEL_TILKJENT_YTELSE_SEQ INCREMENT BY 50 START WITH 2000000 NO CYCLE;
create index on ANDEL_TILKJENT_YTELSE (fk_behandling_id);
create index on ANDEL_TILKJENT_YTELSE (fk_person_id);

-- Satser på at det er nok :/
SELECT setval('ANDEL_TILKJENT_YTELSE_SEQ', COALESCE((SELECT MAX(id)+1000000 FROM vedtak_person),2000000));

-- Overfør alt fra VEDTAK_PERSON
INSERT INTO ANDEL_TILKJENT_YTELSE (
    id,
    fk_behandling_id,
    fk_person_id,
    versjon,
    opprettet_av,
    opprettet_tid,
    stonad_fom,
    stonad_tom,
    type,
    belop,
    endret_av,
    endret_tid)
SELECT
    vp.id,
    v.fk_behandling_id,
    vp.fk_person_id,
    vp.versjon,
    vp.opprettet_av,
    vp.opprettet_tid,
    vp.stonad_fom,
    vp.stonad_tom,
    vp.type,
    vp.belop,
    vp.endret_av,
    vp.endret_tid
FROM VEDTAK_PERSON vp JOIN VEDTAK v ON vp.fk_vedtak_id=v.id;

-- Pass på at endringer i VEDTAK_PERSON etter dette migreres til ANDEL_TILKJENT_YTELSE
-- VEDTAK_PERSON og denne triggeren skal slettes i egen migrering
CREATE OR REPLACE FUNCTION oppdater_andel_tilkjent_ytelse() RETURNS TRIGGER AS
$body$
    DECLARE
        behandlingId bigint;
    BEGIN
        IF (TG_OP = 'DELETE') THEN
            DELETE FROM ANDEL_TILKJENT_YTELSE WHERE id = OLD.id;

            RETURN OLD;
        ELSIF (TG_OP = 'UPDATE') THEN
            SELECT vedtak.fk_behandling_id INTO behandlingId FROM vedtak WHERE vedtak.id = NEW.fk_vedtak_id;
            UPDATE ANDEL_TILKJENT_YTELSE
            SET fk_behandling_id=behandlingId,
                versjon = NEW.versjon,
                stonad_fom=NEW.stonad_fom,
                stonad_tom=NEW.stonad_tom,
                type=NEW.type,
                belop=NEW.belop,
                endret_av = NEW.endret_av,
                endret_tid=NEW.endret_tid
            WHERE id = NEW.id;

            RETURN NEW;
        ELSIF (TG_OP = 'INSERT') THEN
            SELECT vedtak.fk_behandling_id INTO behandlingId FROM vedtak WHERE vedtak.id = NEW.fk_vedtak_id;
            INSERT INTO ANDEL_TILKJENT_YTELSE (
                id,
                fk_behandling_id,
                fk_person_id,
                versjon,
                opprettet_av,
                opprettet_tid,
                stonad_fom,
                stonad_tom,
                type,
                belop,
                endret_av,
                endret_tid)
            VALUES(
                NEW.id,
                behandlingId,
                NEW.fk_person_id,
                NEW.versjon,
                NEW.opprettet_av,
                NEW.opprettet_tid,
                NEW.stonad_fom,
                NEW.stonad_tom,
                NEW.type,
                NEW.belop,
                NEW.endret_av,
                NEW.endret_tid);

            RETURN NEW;
        END IF;
    END;
$body$ LANGUAGE plpgsql;

CREATE TRIGGER oppdater_andel_tilkjent_ytelse
    AFTER INSERT OR UPDATE OR DELETE ON vedtak_person
    FOR EACH ROW EXECUTE PROCEDURE oppdater_andel_tilkjent_ytelse();
