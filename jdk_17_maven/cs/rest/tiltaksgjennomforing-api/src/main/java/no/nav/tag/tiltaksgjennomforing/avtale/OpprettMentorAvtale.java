package no.nav.tag.tiltaksgjennomforing.avtale;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OpprettMentorAvtale {
    private Fnr deltakerFnr;
    private Fnr mentorFnr;
    private BedriftNr bedriftNr;
    private Tiltakstype tiltakstype;
    private Avtalerolle avtalerolle;
}
