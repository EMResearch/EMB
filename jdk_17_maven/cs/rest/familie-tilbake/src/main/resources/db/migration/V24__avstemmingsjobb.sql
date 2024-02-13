DO
$$
    BEGIN
        IF NOT EXISTS
            (SELECT 1 FROM task WHERE type = 'task.avstemming')
        THEN
            INSERT INTO task(payload, type, status, metadata, versjon, opprettet_tid, trigger_tid)
            VALUES ('2020-09-20', 'task.avstemming', 'UBEHANDLET', 'callId=e5ac23de-21b7-497a-9be3-dbe1d8110088
', 0, now(), now());
        END IF;
    END
$$;
