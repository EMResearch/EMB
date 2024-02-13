alter table tilskudd_periode
    add column refusjon_status varchar;

-- Flytt tidligere UTBETALT status fra status til refusjon_status
UPDATE tilskudd_periode
set REFUSJON_STATUS = 'UTBETALT'
where STATUS = 'UTBETALT';

UPDATE tilskudd_periode
set STATUS = 'GODKJENT'
where STATUS = 'UTBETALT';
