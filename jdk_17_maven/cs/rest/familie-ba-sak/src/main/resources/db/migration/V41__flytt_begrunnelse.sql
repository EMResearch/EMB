alter table vilkar_resultat add column begrunnelse text;

update vilkar_resultat vr
set begrunnelse=periode_resultat_begrunnelse.begrunnelse
from (
         with behandling_resultat_begrunnelse as (
             select br.id, b.begrunnelse
             from behandling_resultat br,
                  behandling b
             where br.FK_BEHANDLING_ID = b.id)
         select brb.begrunnelse, pr.id as periode_resultat_id
         from behandling_resultat_begrunnelse brb
                  inner join periode_resultat pr on pr.fk_behandling_resultat_id = brb.ID) as periode_resultat_begrunnelse
where periode_resultat_begrunnelse.periode_resultat_id = vr.fk_periode_resultat_id;

alter table behandling drop column begrunnelse;