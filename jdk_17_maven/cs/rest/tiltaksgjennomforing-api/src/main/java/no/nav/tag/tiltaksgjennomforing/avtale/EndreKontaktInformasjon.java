package no.nav.tag.tiltaksgjennomforing.avtale;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class EndreKontaktInformasjon {
    String deltakerFornavn;
    String deltakerEtternavn;
    String deltakerTlf;
    String veilederFornavn;
    String veilederEtternavn;
    String veilederTlf;
    String arbeidsgiverFornavn;
    String arbeidsgiverEtternavn;
    String arbeidsgiverTlf;
    RefusjonKontaktperson refusjonKontaktperson;
}
