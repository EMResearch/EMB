package no.nav.tag.tiltaksgjennomforing.avtale;

import java.time.LocalDate;
import no.nav.tag.tiltaksgjennomforing.exceptions.TiltaksgjennomforingException;
import no.nav.tag.tiltaksgjennomforing.utils.Now;

public class Fnr extends Identifikator {

    public Fnr(String fnr) {
        super(fnr);
        if (fnr != null && !erGyldigFnr(fnr)) {
            throw new TiltaksgjennomforingException("Ugyldig fødselsnummer. Må bestå av 11 tegn.");
        }
    }

    public static boolean erGyldigFnr(String fnr) {
        return fnr.matches("^[0-9]{11}$");
    }

    private LocalDate fødselsdato() {
        int dag = Integer.parseInt(this.getDayInMonth());
        int måned = Integer.parseInt(this.getMonth());
        int år = Integer.parseInt(this.getBirthYear());
        return LocalDate.of(år, måned, dag);
    }

    public boolean erUnder16år() {
        if (this.asString().equals("00000000000")) {
            return false;
        }
        return this.fødselsdato().isAfter(Now.localDate().minusYears(16));
    }

    public boolean erOver30år() {
        if (this.asString().equals("00000000000")) {
            return false;
        }
        return this.fødselsdato().isBefore(Now.localDate().minusYears(30));
    }

    private static LocalDate førsteJanuarIÅr() {
        return Now.localDate()
                .minusMonths(Now.localDate().getMonthValue() - 1).minusDays(Now.localDate().getDayOfMonth() - 1);
    }

    public boolean erOver30årFørsteJanuar() {
        if (this.asString().equals("00000000000")) {
            return false;
        }
        return this.fødselsdato().isBefore(førsteJanuarIÅr().minusYears(30));
    }

    public boolean erOver30årFraOppstartDato(LocalDate opprettetTidspunkt) {
        if (this.asString().equals("00000000000")) {
            return false;
        }
        return this.fødselsdato().isBefore(opprettetTidspunkt.minusYears(30));
    }

    public boolean erOver67ÅrFraSluttDato(LocalDate sluttDato) {
        if (this.asString().equals("00000000000")) {
            return false;
        }
        return this.fødselsdato().isBefore(sluttDato.minusYears(67).plusDays(1));
    }

    public boolean erOver72ÅrFraSluttDato(LocalDate sluttDato) {
        if (this.asString().equals("00000000000")) {
            return false;
        }
        return this.fødselsdato().isBefore(sluttDato.minusYears(72).plusDays(1));
    }

    private String getDayInMonth() {
        return parseSynthenticNumber(parseDNumber(this.asString())).substring(0, 2);
    }

    private String getMonth() {
        return parseSynthenticNumber(parseDNumber(this.asString())).substring(2, 4);
    }

    private String getBirthYear() {
        return getCentury() + get2DigitBirthYear();
    }

    private static String parseSynthenticNumber(String fodselsnummer) {
        if (!isSynthetic(fodselsnummer)) {
            return fodselsnummer;
        } else {
            if (getThirdDigit(fodselsnummer) > 7) {
                return fodselsnummer.substring(0, 2) + (getThirdDigit(fodselsnummer) - 8) + fodselsnummer.substring(3);
            } else {
                return fodselsnummer.substring(0, 2) + (getThirdDigit(fodselsnummer) - 4) + fodselsnummer.substring(3);
            }
        }
    }

    private static boolean isSynthetic(String fodselsnummer) {
        try {
            int thirdDigit = getThirdDigit(fodselsnummer);
            if (thirdDigit > 3) {
                return true;
            }
        } catch (IllegalArgumentException e) {
            // ignore
        }
        return false;
    }

    private static int getThirdDigit(String fodselsnummer) {
        return Integer.parseInt(fodselsnummer.substring(2, 3));
    }

    private static int getFirstDigit(String fodselsnummer) {
        return Integer.parseInt(fodselsnummer.substring(0, 1));
    }

    private static String parseDNumber(String fodselsnummer) {
        if (!isDNumber(fodselsnummer)) {
            return fodselsnummer;
        } else {
            return (getFirstDigit(fodselsnummer) - 4) + fodselsnummer.substring(1);
        }
    }

    private static boolean isDNumber(String fodselsnummer) {
        try {
            int firstDigit = getFirstDigit(fodselsnummer);
            if (firstDigit > 3 && firstDigit < 8) {
                return true;
            }
        } catch (IllegalArgumentException e) {
            // ignore
        }
        return false;
    }

    private String getCentury() {
        String result = null;
        int individnummerInt = Integer.parseInt(getIndividnummer());
        int birthYear = Integer.parseInt(get2DigitBirthYear());
        if (individnummerInt <= 499) {
            result = "19";
        } else if (individnummerInt >= 500 && birthYear < 40) {
            result = "20";
        } else if (individnummerInt >= 500 && individnummerInt <= 749 && birthYear >= 54) {
            result = "18";
        } else if (individnummerInt >= 900 && birthYear > 39) {
            result = "19";
        }
        return result;
    }

    private String getIndividnummer() {
        return this.asString().substring(6, 9);
    }

    private String get2DigitBirthYear() {
        return this.asString().substring(4, 6);
    }
}
