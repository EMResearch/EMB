package no.nav.tag.tiltaksgjennomforing.datavarehus;

import lombok.Value;

@Value
public class DvhMeldingOpprettet {
    DvhMeldingEntitet entitet;
    AvroTiltakHendelse avroTiltakHendelse;
}
