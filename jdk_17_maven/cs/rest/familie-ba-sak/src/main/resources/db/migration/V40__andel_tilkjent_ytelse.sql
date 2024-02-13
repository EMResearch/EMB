ALTER TABLE ANDEL_TILKJENT_YTELSE
    ADD COLUMN tilkjent_ytelse_id bigint references tilkjent_ytelse(id);

UPDATE ANDEL_TILKJENT_YTELSE aty
SET tilkjent_ytelse_id = ty.id
FROM tilkjent_ytelse ty WHERE aty.fk_behandling_id = ty.fk_behandling_id;