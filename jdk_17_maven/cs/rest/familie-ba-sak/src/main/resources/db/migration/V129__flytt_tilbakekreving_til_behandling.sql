ALTER TABLE tilbakekreving
    ADD COLUMN fk_behandling_id BIGINT REFERENCES behandling (id);

-- Set fk_behandling_id til behandlings id som er relatert til vedtaket.
UPDATE tilbakekreving
SET fk_behandling_id = behandling.id
FROM behandling
         JOIN vedtak
              ON behandling.ID = vedtak.fk_behandling_id
WHERE tilbakekreving.fk_vedtak_id = vedtak.id
  AND vedtak.aktiv = true;

-- Slett alle tilbakekrevinger som er relatert til en inaktiv vedtak.
DELETE
FROM tilbakekreving
WHERE fk_behandling_id is null;

ALTER TABLE tilbakekreving
    DROP COLUMN fk_vedtak_id;