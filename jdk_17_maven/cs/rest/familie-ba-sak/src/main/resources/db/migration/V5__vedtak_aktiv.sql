alter table BEHANDLING_VEDTAK add column aktiv boolean default true;

CREATE UNIQUE INDEX UIDX_BEHANDLING_VEDTAK_01
    ON BEHANDLING_VEDTAK
        (
         (CASE
              WHEN aktiv = true
                  THEN fk_behandling_id
              ELSE NULL END),
         (CASE
              WHEN aktiv = true
                  THEN aktiv
              ELSE NULL END)
            );