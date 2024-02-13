ALTER TABLE vilkar_resultat
    ADD COLUMN vurderes_etter varchar;

UPDATE vilkar_resultat
SET vurderes_etter = 'NASJONALE_REGLER'
WHERE vilkar not in ('UNDER_18_ÅR', 'GIFT_PARTNERSKAP');