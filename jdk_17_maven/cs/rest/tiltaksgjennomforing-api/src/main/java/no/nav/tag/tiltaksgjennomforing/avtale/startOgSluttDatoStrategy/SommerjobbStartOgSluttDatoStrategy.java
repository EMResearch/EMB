package no.nav.tag.tiltaksgjennomforing.avtale.startOgSluttDatoStrategy;

import java.time.LocalDate;
import no.nav.tag.tiltaksgjennomforing.exceptions.Feilkode;
import no.nav.tag.tiltaksgjennomforing.exceptions.FeilkodeException;

public class SommerjobbStartOgSluttDatoStrategy implements StartOgSluttDatoStrategy {
    @Override
    public void sjekkStartOgSluttDato(LocalDate startDato, LocalDate sluttDato, boolean erGodkjentForEtterregistrering, boolean erAvtaleInngått) {
        StartOgSluttDatoStrategy.super.sjekkStartOgSluttDato(startDato, sluttDato, erGodkjentForEtterregistrering, erAvtaleInngått);
        if (startDato != null) {
                if (startDato.isBefore(LocalDate.of(startDato.getYear(), 6, 1)) ) {
                    throw new FeilkodeException(Feilkode.SOMMERJOBB_FOR_TIDLIG);
                }
                if (startDato.isAfter(LocalDate.of(startDato.getYear(), 8, 31))) {
                    throw new FeilkodeException(Feilkode.SOMMERJOBB_FOR_SENT);
                }
        }
      if (startDato != null && sluttDato != null) {
        if (startDato.plusWeeks(4).minusDays(1).isBefore(sluttDato)) {
          throw new FeilkodeException(Feilkode.SOMMERJOBB_FOR_LANG_VARIGHET);
        }else{
          if (sluttDato.isBefore(LocalDate.of(sluttDato.getYear(), 6, 1)) ) {
            throw new FeilkodeException(Feilkode.SOMMERJOBB_FOR_TIDLIG);
          }
          if (sluttDato.isAfter(LocalDate.of(sluttDato.getYear(), 9, 27))) {
            throw new FeilkodeException(Feilkode.SOMMERJOBB_FOR_SENT);
          }
        }
      }
    }
}
