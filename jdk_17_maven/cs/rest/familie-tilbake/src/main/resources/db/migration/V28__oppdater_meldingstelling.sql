UPDATE meldingstelling
SET fagsystem = 'EF'
WHERE fagsystem IN ('OVERGANGSSTØNAD', 'BARNETILSYN', 'SKOLEPENGER');

UPDATE meldingstelling
SET fagsystem = 'BA'
WHERE fagsystem = 'BARNETRYGD';
