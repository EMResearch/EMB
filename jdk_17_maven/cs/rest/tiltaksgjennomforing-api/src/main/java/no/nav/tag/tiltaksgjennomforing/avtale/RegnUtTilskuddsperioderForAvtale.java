package no.nav.tag.tiltaksgjennomforing.avtale;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import no.nav.tag.tiltaksgjennomforing.exceptions.Feilkode;
import no.nav.tag.tiltaksgjennomforing.exceptions.FeilkodeException;
import no.nav.tag.tiltaksgjennomforing.utils.Periode;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static no.nav.tag.tiltaksgjennomforing.utils.DatoUtils.sisteDatoIMnd;

@Slf4j
@UtilityClass
public class RegnUtTilskuddsperioderForAvtale {

    private final static BigDecimal DAGER_I_MÅNED = new BigDecimal("30.4375");
    private final static int ANTALL_MÅNEDER_I_EN_PERIODE = 1;

    /**
        TODO: TODO: Kalkulering av redusert prosent og redusert dato bør kun skje i {@link no.nav.tag.tiltaksgjennomforing.avtale.VarigLonnstilskuddStrategy} og heller ikke i frontend
     */
    public static List<TilskuddPeriode> beregnTilskuddsperioderForAvtale(UUID id, Tiltakstype tiltakstype, Integer sumLønnstilskuddPerMåned, LocalDate datoFraOgMed, LocalDate datoTilOgMed, Integer lonnstilskuddprosent, LocalDate datoForRedusertProsent, Integer sumLønnstilskuddPerMånedRedusert) {
        if (datoForRedusertProsent == null) {
            // Ingen reduserte peridoder   -----60-----60------60------ |
            return lagPeriode(datoFraOgMed, datoTilOgMed).stream().map(datoPar -> {
                Integer beløp = beløpForPeriode(datoPar.getStart(), datoPar.getSlutt(), sumLønnstilskuddPerMåned);
                return new TilskuddPeriode(beløp, datoPar.getStart(), datoPar.getSlutt(), lonnstilskuddprosent);
            }).collect(Collectors.toList());
        } else {
            if (datoFraOgMed.isBefore(datoForRedusertProsent.plusDays(1)) && datoTilOgMed.isAfter(datoForRedusertProsent.minusDays(1))) {
                // Både ikke reduserte og reduserte   ---60---60-----50----|--50----50-----
                List<TilskuddPeriode> tilskuddperioderFørRedusering = lagPeriode(datoFraOgMed, datoForRedusertProsent.minusDays(1)).stream().map(datoPar -> {
                    Integer beløp = beløpForPeriode(datoPar.getStart(), datoPar.getSlutt(), sumLønnstilskuddPerMåned);
                    return new TilskuddPeriode(beløp, datoPar.getStart(), datoPar.getSlutt(), lonnstilskuddprosent);
                }).collect(Collectors.toList());

                List<TilskuddPeriode> tilskuddperioderEtterRedusering = lagPeriode(datoForRedusertProsent, datoTilOgMed).stream().map(datoPar -> {
                    Integer beløp = beløpForPeriode(datoPar.getStart(), datoPar.getSlutt(), sumLønnstilskuddPerMånedRedusert);
                    return new TilskuddPeriode(beløp, datoPar.getStart(), datoPar.getSlutt(), getLonnstilskuddProsent(tiltakstype, lonnstilskuddprosent));
                }).collect(Collectors.toList());

                ArrayList<TilskuddPeriode> tilskuddsperioder = new ArrayList<>();
                tilskuddsperioder.addAll(tilskuddperioderFørRedusering);
                tilskuddsperioder.addAll(tilskuddperioderEtterRedusering);
                return tilskuddsperioder;
            } else if (datoFraOgMed.isAfter(datoForRedusertProsent)) {
                // Kun redusete peridoer      ---60----60----60---50---|--50----50---50--50--
                List<TilskuddPeriode> tilskuddperioderEtterRedusering = lagPeriode(datoFraOgMed, datoTilOgMed).stream().map(datoPar -> {
                    Integer beløp = beløpForPeriode(datoPar.getStart(), datoPar.getSlutt(), sumLønnstilskuddPerMånedRedusert);
                    return new TilskuddPeriode(beløp, datoPar.getStart(), datoPar.getSlutt(), getLonnstilskuddProsent(tiltakstype, lonnstilskuddprosent));
                }).collect(Collectors.toList());
                ArrayList<TilskuddPeriode> tilskuddsperioder = new ArrayList<>();
                tilskuddsperioder.addAll(tilskuddperioderEtterRedusering);
                return tilskuddsperioder;
            } else {
                log.error("Uventet feil i utregning av tilskuddsperioder med startdato: {}, sluttdato: {}, datoForRedusertProsent: {}, avtaleId: {}", datoFraOgMed, datoTilOgMed, datoForRedusertProsent, id);
                throw new FeilkodeException(Feilkode.FORLENG_MIDLERTIDIG_IKKE_TILGJENGELIG);
            }
        }

    }

    private static int getLonnstilskuddProsent(Tiltakstype tiltakstype, Integer lonnstilskuddprosent) {
        if(tiltakstype == Tiltakstype.VARIG_LONNSTILSKUDD){
            if(lonnstilskuddprosent >= 68) return 67;
            return lonnstilskuddprosent;
        }
        return lonnstilskuddprosent - 10;
    }

    public static Integer beløpForPeriode(LocalDate datoFraOgMed, LocalDate datoTilOgMed, LocalDate datoForRedusertProsent, Integer sumLønnstilskuddPerMåned, Integer sumLønnstilskuddPerMånedRedusert) {
        if (datoForRedusertProsent == null || datoTilOgMed.isBefore(datoForRedusertProsent)) {
            return beløpForPeriode(datoFraOgMed, datoTilOgMed, sumLønnstilskuddPerMåned);
        } else {
            return beløpForPeriode(datoFraOgMed, datoTilOgMed, sumLønnstilskuddPerMånedRedusert);
        }
    }

    public static Integer beløpForPeriode(LocalDate fra, LocalDate til, Integer sumLønnstilskuddPerMåned) {
        Period period = Period.between(fra, til.plusDays(1));
        Integer sumHeleMåneder = period.getMonths() * sumLønnstilskuddPerMåned;
        BigDecimal dagsats = new BigDecimal(sumLønnstilskuddPerMåned).divide(DAGER_I_MÅNED, 10, RoundingMode.HALF_UP);
        Integer sumEnkeltdager = dagsats.multiply(BigDecimal.valueOf(period.getDays()), MathContext.UNLIMITED).setScale(0, RoundingMode.HALF_UP).intValue();
        return sumHeleMåneder + sumEnkeltdager;
    }

    private static List<Periode> lagPeriode(LocalDate datoFraOgMed, LocalDate datoTilOgMed) {
        if (datoFraOgMed.isAfter(datoTilOgMed)) {
            return List.of();
        }
        List<LocalDate> startDatoer = datoFraOgMed.datesUntil(datoTilOgMed.plusDays(1), Period.ofMonths(ANTALL_MÅNEDER_I_EN_PERIODE)).collect(Collectors.toList());
        ArrayList<Periode> datoPar = new ArrayList<>();
        for (int i = 0; i < startDatoer.size(); i++) {
            // fra: Hvis startdato er lik datoFraOgMed, bruk denne, hvis ikke, bruk første datoen i mnd.
            LocalDate fra = startDatoer.get(i).equals(datoFraOgMed) ? startDatoer.get(i) : førsteDatoIMnd(startDatoer.get(i));
            // til: Hvis siste dag i mnd. er mindre enn datoTilOgMed, bruk siste dag i mnd, ellers bruk datoTilOgMed
            LocalDate til = sisteDatoIMnd(startDatoer.get(i)).isBefore(datoTilOgMed) ? sisteDatoIMnd(startDatoer.get(i)) : datoTilOgMed;

            datoPar.addAll(splittHvisNyttÅr(fra, til));
        }
        // Legg til siste periode hvis den ikke kom med i loopen
        if (datoPar.get(datoPar.size() - 1).getSlutt() != datoTilOgMed) {
            datoPar.addAll(splittHvisNyttÅr(førsteDatoIMnd(datoTilOgMed), datoTilOgMed));
        }
        return datoPar;
    }

    private LocalDate førsteDatoIMnd(LocalDate dato) {
        return LocalDate.of(dato.getYear(), dato.getMonth(), 01);
    }

    private static List<Periode> splittHvisNyttÅr (LocalDate fraDato, LocalDate tilDato) {
        if (fraDato.getYear() != tilDato.getYear()) {
            Periode datoPar1 = new Periode(fraDato, fraDato.withMonth(12).withDayOfMonth(31));
            Periode datoPar2 = new Periode(tilDato.withMonth(1).withDayOfMonth(1), tilDato);
            return List.of(datoPar1, datoPar2);
        } else {
            return List.of(new Periode(fraDato, tilDato));
        }
    }
}