DROP TABLE IF EXISTS totrinnsresultatsgrunnlag;
DROP TABLE IF EXISTS revurderingsarsak;
DROP TABLE IF EXISTS arsak_totrinnsvurdering;

ALTER TABLE totrinnsvurdering
    DROP COLUMN aksjonspunktsdefinisjon;

ALTER TABLE totrinnsvurdering
    ADD COLUMN behandlingssteg VARCHAR NOT NULL;

COMMENT ON COLUMN totrinnsvurdering.behandlingssteg
    IS 'Behandlingssteg som kan besluttes';
