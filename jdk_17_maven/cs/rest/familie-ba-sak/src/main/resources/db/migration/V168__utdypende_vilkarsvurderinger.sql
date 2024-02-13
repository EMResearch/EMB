ALTER TABLE vilkar_resultat
    ADD COLUMN utdypende_vilkarsvurderinger VARCHAR;

-- er_skjonnsmessig_vurdert -> VURDERING_ANNET_GRUNNLAG
-- er_medlemskap_vurdert -> VURDERT_MEDLEMSKAP
-- er_delt_bosted -> DELT_BOSTED
UPDATE vilkar_resultat
SET utdypende_vilkarsvurderinger
        = concat_ws(';'
        , CASE WHEN er_skjonnsmessig_vurdert = TRUE THEN 'VURDERING_ANNET_GRUNNLAG' END
        , CASE WHEN er_medlemskap_vurdert = TRUE THEN 'VURDERT_MEDLEMSKAP' END
        , CASE WHEN er_delt_bosted = TRUE THEN 'DELT_BOSTED' END)
WHERE vilkar_resultat.utdypende_vilkarsvurderinger IS NULL
