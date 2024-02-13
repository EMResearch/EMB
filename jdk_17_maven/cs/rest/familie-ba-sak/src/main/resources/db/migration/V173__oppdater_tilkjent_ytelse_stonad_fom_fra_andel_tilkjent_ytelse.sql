with ny_stonad_tom (tilkjent_ytelse_id, ny_stonad_tom) as (
	select ty.id, max(aty.stonad_tom)
	from andel_tilkjent_ytelse aty
	join tilkjent_ytelse ty on ty.id = aty.tilkjent_ytelse_id
	where  ty.stonad_tom < aty.stonad_tom
	group by ty.id
)

update tilkjent_ytelse ty
    set stonad_tom = st.ny_stonad_tom
    from ny_stonad_tom st
    where st.tilkjent_ytelse_id = ty.id
;

with ny_stonad_fom (tilkjent_ytelse_id, ny_stonad_fom) as (
	select ty.id, min(aty.stonad_fom)
	from andel_tilkjent_ytelse aty
	join tilkjent_ytelse ty on ty.id = aty.tilkjent_ytelse_id
	where  ty.stonad_fom > aty.stonad_fom
	group by ty.id
)

update tilkjent_ytelse ty
    set stonad_fom = st.ny_stonad_fom
    from ny_stonad_fom st
    where st.tilkjent_ytelse_id = ty.id	
;