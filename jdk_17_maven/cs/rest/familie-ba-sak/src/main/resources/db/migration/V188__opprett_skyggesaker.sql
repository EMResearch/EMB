INSERT INTO skyggesak(id, fk_fagsak_id)
select nextval('skyggesak_seq'), id
from fagsak
where arkivert = false
  AND status in ('LØPENDE', 'OPPRETTET');