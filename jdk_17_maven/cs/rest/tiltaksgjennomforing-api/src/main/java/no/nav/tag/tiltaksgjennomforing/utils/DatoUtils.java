package no.nav.tag.tiltaksgjennomforing.utils;

import lombok.experimental.UtilityClass;

import java.time.LocalDate;

@UtilityClass
public class DatoUtils {
    public static LocalDate sisteDatoIMnd(LocalDate dato) {
        return LocalDate.of(dato.getYear(), dato.getMonth(), dato.lengthOfMonth());
    }
}
