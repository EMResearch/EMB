package no.nav.tag.tiltaksgjennomforing.avtale.startOgSluttDatoStrategy;


import no.nav.tag.tiltaksgjennomforing.enhet.Kvalifiseringsgruppe;
import no.nav.tag.tiltaksgjennomforing.exceptions.Feilkode;
import no.nav.tag.tiltaksgjennomforing.exceptions.FeilkodeException;

import java.time.LocalDate;

public class MentorStartOgSluttDatoStrategy implements StartOgSluttDatoStrategy {
    private final Kvalifiseringsgruppe kvalifiseringsgruppe;

    public MentorStartOgSluttDatoStrategy(Kvalifiseringsgruppe kvalifiseringsgruppe) {
        this.kvalifiseringsgruppe = kvalifiseringsgruppe;
    }

    @Override
    public void sjekkStartOgSluttDato(LocalDate startDato, LocalDate sluttDato, boolean erGodkjentForEtterregistrering, boolean erAvtaleInngått) {
        StartOgSluttDatoStrategy.super.sjekkStartOgSluttDato(startDato, sluttDato, erGodkjentForEtterregistrering, erAvtaleInngått);

        if (startDato != null && sluttDato != null) {
            if ((kvalifiseringsgruppe == Kvalifiseringsgruppe.SPESIELT_TILPASSET_INNSATS || kvalifiseringsgruppe == Kvalifiseringsgruppe.VARIG_TILPASSET_INNSATS)
                    && startDato.plusMonths(36).minusDays(1).isBefore(sluttDato)) {
                throw new FeilkodeException(Feilkode.VARIGHET_FOR_LANG_MENTOR_36_MND);
            }
        }

        if (kvalifiseringsgruppe != Kvalifiseringsgruppe.SPESIELT_TILPASSET_INNSATS && kvalifiseringsgruppe != Kvalifiseringsgruppe.VARIG_TILPASSET_INNSATS && startDato != null && sluttDato != null && startDato.plusMonths(6).minusDays(1).isBefore(sluttDato)) {
            throw new FeilkodeException(Feilkode.VARIGHET_FOR_LANG_MENTOR_6_MND);
        }
    }
}
