ALTER TABLE totrinnsresultatsgrunnlag
    DROP COLUMN gruppering_vurdert_foreldelse_id;

ALTER TABLE totrinnsresultatsgrunnlag
    ADD COLUMN vurdert_foreldelse_id UUID REFERENCES vurdert_foreldelse;

COMMENT ON COLUMN totrinnsresultatsgrunnlag.vurdert_foreldelse_id
    IS 'Fk til aktivt vurdertforeldelse ved totrinnsbehandlingen';

DROP TABLE gruppering_vurdert_foreldelse;
