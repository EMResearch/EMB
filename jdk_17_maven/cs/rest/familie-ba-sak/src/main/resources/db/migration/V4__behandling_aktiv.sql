alter table BEHANDLING add column aktiv boolean default true;

CREATE UNIQUE INDEX UIDX_BEHANDLING_01
    ON BEHANDLING
        (
         (CASE
              WHEN aktiv = true
                  THEN FK_FAGSAK_ID
              ELSE NULL END),
         (CASE
              WHEN aktiv = true
                  THEN aktiv
              ELSE NULL END)
            );