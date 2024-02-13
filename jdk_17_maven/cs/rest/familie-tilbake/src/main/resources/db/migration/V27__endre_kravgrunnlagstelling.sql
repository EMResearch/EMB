ALTER TABLE meldingstelling
    RENAME COLUMN ytelsestype TO fagsystem;

UPDATE meldingstelling
SET fagsystem = 'EF'
WHERE fagsystem LIKE 'EF%';
