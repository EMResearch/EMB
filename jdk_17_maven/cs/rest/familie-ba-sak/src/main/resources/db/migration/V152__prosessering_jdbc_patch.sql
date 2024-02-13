ALTER TABLE task ALTER COLUMN id SET DEFAULT nextval('task_seq');
ALTER SEQUENCE task_seq OWNED BY task.id;

ALTER TABLE task_logg ALTER COLUMN id SET DEFAULT nextval('task_logg_seq');
ALTER SEQUENCE task_logg_seq OWNED BY task_logg.id;