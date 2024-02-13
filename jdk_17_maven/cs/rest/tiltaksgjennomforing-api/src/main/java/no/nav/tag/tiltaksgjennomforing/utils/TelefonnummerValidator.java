package no.nav.tag.tiltaksgjennomforing.utils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class
TelefonnummerValidator {
    public static boolean erGyldigMobilnummer(String tlf) {
        if (tlf == null) {
            return false;
        }
        return tlf.matches("[4|9][0-9]{7}");
    }
}
