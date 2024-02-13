package no.nav.tag.tiltaksgjennomforing.avtale;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OpprettAvtale {
    private Fnr deltakerFnr;
    private BedriftNr bedriftNr;
    private Tiltakstype tiltakstype;

    boolean erLønnstilskudd() {
        return tiltakstype.equals(Tiltakstype.MIDLERTIDIG_LONNSTILSKUDD) || tiltakstype.equals(Tiltakstype.VARIG_LONNSTILSKUDD);
    }
}