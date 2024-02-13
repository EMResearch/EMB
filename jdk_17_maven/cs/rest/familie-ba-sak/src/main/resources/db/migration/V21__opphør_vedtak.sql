alter table VEDTAK add column fk_forrige_vedtak_id bigint references VEDTAK default null;
alter table VEDTAK add column opphor_dato TIMESTAMP(3) default null;