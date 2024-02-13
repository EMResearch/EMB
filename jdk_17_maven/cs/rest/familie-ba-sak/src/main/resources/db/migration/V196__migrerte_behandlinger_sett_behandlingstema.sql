update behandling b
set underkategori = 'UTVIDET'
FROM andel_tilkjent_ytelse aty
WHERE aty.fk_behandling_id = b.id AND
    aty.type = 'UTVIDET_BARNETRYGD' AND
    b.underkategori = 'ORDINÆR' and
    b.opprettet_aarsak = 'ENDRE_MIGRERINGSDATO';