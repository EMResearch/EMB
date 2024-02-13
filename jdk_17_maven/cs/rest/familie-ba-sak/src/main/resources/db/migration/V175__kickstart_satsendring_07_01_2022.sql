DO
$$
    BEGIN
        INSERT INTO task(payload, type, status, metadata, versjon, opprettet_tid, trigger_tid)
        VALUES ('1654', 'startsatsendringforallebehandlinger', 'UBEHANDLET',
                'callId=startsatsendringforallebehandlinger-07.01.2022', 1, now(),
                now());
    END
$$;