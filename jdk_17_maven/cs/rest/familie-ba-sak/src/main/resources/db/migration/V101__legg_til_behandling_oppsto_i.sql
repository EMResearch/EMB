ALTER TABLE andel_tilkjent_ytelse
    ADD COLUMN kilde_behandling_id BIGINT REFERENCES behandling (id);

UPDATE andel_tilkjent_ytelse
SET kilde_behandling_id = andelMedKildeBehandling.kilde_behandling_id
FROM (
         WITH andelMedFagsak AS (SELECT andel_tilkjent_ytelse.id             AS andelId,
                                        andel_tilkjent_ytelse.periode_offset AS andelPeriodeId,
                                        behandling.fk_fagsak_id              AS fagsakId
                                 FROM andel_tilkjent_ytelse,
                                      behandling
                                 WHERE andel_tilkjent_ytelse.fk_behandling_id = behandling.id),
              kildeBehandling AS (SELECT behandling.fk_fagsak_id              AS fagsakId,
                                         andel_tilkjent_ytelse.periode_offset AS fagsakPeriodeId,
                                         min(behandling.id)                   AS kilde_behandling_id
                                  FROM behandling
                                           INNER JOIN andel_tilkjent_ytelse
                                                      ON behandling.id = andel_tilkjent_ytelse.fk_behandling_id
                                  GROUP BY fagsakId, fagsakPeriodeId)
         SELECT andelMedFagsak.andelId,
                kildeBehandling.kilde_behandling_id
         FROM andelMedFagsak
                  INNER JOIN kildeBehandling ON andelMedFagsak.andelPeriodeId = kildeBehandling.fagsakPeriodeId AND
                                                andelMedFagsak.fagsakId = kildeBehandling.fagsakId) AS andelMedKildeBehandling
WHERE andel_tilkjent_ytelse.id = andelMedKildeBehandling.andelId;