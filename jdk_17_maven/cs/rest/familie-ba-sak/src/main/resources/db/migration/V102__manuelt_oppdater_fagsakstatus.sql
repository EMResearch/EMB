/* Kjøring av script 01.12.20 feilet pga manglende leader election på poder */
/* Kjører derfor scriptet manuelt her og oppdaterer status */

UPDATE fagsak
SET status = 'AVSLUTTET'
WHERE fagsak.id in (select id from fagsak
                    where fagsak.id in (
                        with sisteIverksatte as (
                            select b.fk_fagsak_id as fagsakId, max(b.id) as behandlingId
                            from behandling b
                                     inner join tilkjent_ytelse ty on b.id = ty.fk_behandling_id
                                     inner join fagsak f on f.id = b.fk_fagsak_id
                            where ty.utbetalingsoppdrag IS NOT NULL
                              and f.status = 'LØPENDE'
                            group by b.id)
                        select sisteIverksatte.fagsakId
                        from sisteIverksatte
                                 inner join tilkjent_ytelse ty on sisteIverksatte.behandlingId = ty.fk_behandling_id
                        where ty.stonad_tom < now()))