DO
$$
    BEGIN
        UPDATE task
        set versjon=1
        WHERE versjon = 0
          AND metadata = 'callId=startsatsendringforallebehandlinger-06.01.2022';
    END
$$;