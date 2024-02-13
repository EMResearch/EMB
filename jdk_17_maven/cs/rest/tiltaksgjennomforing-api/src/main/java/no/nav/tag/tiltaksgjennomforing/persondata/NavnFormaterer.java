package no.nav.tag.tiltaksgjennomforing.persondata;

import org.apache.commons.text.WordUtils;
import org.springframework.util.StringUtils;

public class NavnFormaterer {
    private final Navn navn;

    public NavnFormaterer(Navn navn) {
        this.navn = navn;
    }

    public String getEtternavn() {
        return storeForbokstaver(navn.getEtternavn());
    }

    public String getFornavn() {
        String fornavnOgMellomnavn = navn.getFornavn();
        if (StringUtils.hasLength(navn.getMellomnavn())) {
            fornavnOgMellomnavn += " " + navn.getMellomnavn();
        }
        return storeForbokstaver(fornavnOgMellomnavn);
    }

    private static String storeForbokstaver(String navn) {
        return WordUtils.capitalizeFully(navn, '-', ' ');
    }
}
