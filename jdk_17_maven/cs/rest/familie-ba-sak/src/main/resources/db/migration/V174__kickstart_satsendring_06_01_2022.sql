DO
$$
    BEGIN
        IF NOT EXISTS
            (SELECT 1 FROM task WHERE type = 'startsatsendringforallebehandlinger')
        THEN
            INSERT INTO task(payload, type, status, metadata, versjon, opprettet_tid, trigger_tid)
            VALUES ('1654', 'startsatsendringforallebehandlinger', 'UBEHANDLET',
                    'callId=startsatsendringforallebehandlinger-06.01.2022', 0, now(),
                    now());
        END IF;
    END
$$;