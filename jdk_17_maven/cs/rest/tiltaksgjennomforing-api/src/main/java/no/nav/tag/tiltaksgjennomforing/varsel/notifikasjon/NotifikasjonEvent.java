package no.nav.tag.tiltaksgjennomforing.varsel.notifikasjon;

import lombok.Data;

@Data
public class NotifikasjonEvent {
    ArbeidsgiverNotifikasjon notifikasjon;
    boolean notifikasjonFerdigBehandlet;
}
