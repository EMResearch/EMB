INSERT INTO ANNEN_VURDERING(id,
                            fk_person_resultat_id,
                            resultat,
                            type,
                            begrunnelse,
                            versjon,
                            opprettet_av,
                            opprettet_tid,
                            endret_av,
                            endret_tid)
SELECT nextval('ANNEN_VURDERING_SEQ'),
       p.id,
       CASE o.status
           WHEN 'IKKE_SATT' THEN 'IKKE_VURDERT'
           WHEN 'MOTTATT' THEN 'OPPFYLT'
           WHEN 'IKKE_MOTTATT_AVSLAG' THEN 'IKKE_OPPFYLT'
           WHEN 'IKKE_MOTTATT_FORTSETT' THEN 'IKKE_OPPFYLT'
           ELSE 'IKKE_VURDERT'
           END,
       'OPPLYSNINGSPLIKT',
       o.begrunnelse,
       0,
       o.opprettet_av,
       o.opprettet_tid,
       o.endret_av,
       o.endret_tid
FROM OPPLYSNINGSPLIKT o
         INNER JOIN VILKAARSVURDERING v
                    ON v.fk_behandling_id = o.fk_behandling_id
         INNER JOIN PERSON_RESULTAT p ON p.fk_vilkaarsvurdering_id = v.id
WHERE v.aktiv = true
