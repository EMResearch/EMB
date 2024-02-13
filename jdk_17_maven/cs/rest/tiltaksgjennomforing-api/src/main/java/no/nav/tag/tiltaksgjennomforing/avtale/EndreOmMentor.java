package no.nav.tag.tiltaksgjennomforing.avtale;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class EndreOmMentor {
    String mentorFornavn;
    String mentorEtternavn;
    String mentorTlf;
    String mentorOppgaver;
    Double mentorAntallTimer;
    Integer mentorTimelonn;
}
