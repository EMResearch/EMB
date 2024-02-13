DROP INDEX fagsak_ekstern_fagsak_id_idx;
DROP INDEX fagsak_ytelsestype_idx;

CREATE UNIQUE INDEX ON fagsak (ekstern_fagsak_id, ytelsestype);

ALTER TABLE fagsak
    ALTER COLUMN ekstern_fagsak_id SET NOT NULL;

ALTER TABLE fagsak
    ALTER COLUMN bruker_ident SET NOT NULL;
