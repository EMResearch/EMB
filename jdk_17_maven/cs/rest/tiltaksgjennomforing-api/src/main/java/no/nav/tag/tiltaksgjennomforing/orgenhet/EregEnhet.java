package no.nav.tag.tiltaksgjennomforing.orgenhet;

import lombok.Data;
import no.nav.tag.tiltaksgjennomforing.avtale.BedriftNr;

@Data
public class EregEnhet {
    private String organisasjonsnummer;
    private EregNavn navn;
    private String type;

    public Organisasjon konverterTilDomeneObjekt() {
        return new Organisasjon(new BedriftNr(organisasjonsnummer), navn.getSammensattnavn());
    }
}
