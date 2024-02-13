ALTER TABLE ANDEL_TILKJENT_YTELSE
    ADD COLUMN person_ident varchar;

UPDATE ANDEL_TILKJENT_YTELSE aty
SET PERSON_IDENT = (
    SELECT P.person_ident
    FROM po_person P
             WHERE aty.fk_person_id = p.id limit 1);

ALTER TABLE ANDEL_TILKJENT_YTELSE
    ALTER COLUMN person_ident SET NOT NULL;
