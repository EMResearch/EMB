update behandling
set resultat = behandling_med_resultat.eksisterende_resultat
from (select b.id              as behandling_id,
             v.samlet_resultat as eksisterende_resultat
      from behandling b
               inner join vilkaarsvurdering v on b.id = v.fk_behandling_id and v.aktiv = true) as behandling_med_resultat
where behandling.id = behandling_med_resultat.behandling_id;