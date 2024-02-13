package no.nav.tag.tiltaksgjennomforing.avtale;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NavIdent extends Identifikator {
    public NavIdent(String verdi) {
        super(verdi);
        if (!erNavIdent(verdi)) {
            throw new IllegalArgumentException("Er ikke en nav-ident");
        }
    }

    public static boolean erNavIdent(String verdi) {
        return verdi != null && verdi.matches("\\w\\d{6}");
    }
}
