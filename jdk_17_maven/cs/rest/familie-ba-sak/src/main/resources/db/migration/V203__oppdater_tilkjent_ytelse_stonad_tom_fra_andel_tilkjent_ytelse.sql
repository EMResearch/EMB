with ny_stonad_tom (tilkjent_ytelse_id, ny_stonad_tom) as (
    select ty.id,
           grouped_aty.max_stonad_tom
    from (select tilkjent_ytelse_id, MAX(stonad_tom) as max_stonad_tom
          from andel_tilkjent_ytelse
          group by tilkjent_ytelse_id) grouped_aty
             join tilkjent_ytelse ty on grouped_aty.tilkjent_ytelse_id = ty.id
    where grouped_aty.max_stonad_tom != ty.stonad_tom
)

update tilkjent_ytelse ty
set stonad_tom = st.ny_stonad_tom
from ny_stonad_tom st
where st.tilkjent_ytelse_id = ty.id;