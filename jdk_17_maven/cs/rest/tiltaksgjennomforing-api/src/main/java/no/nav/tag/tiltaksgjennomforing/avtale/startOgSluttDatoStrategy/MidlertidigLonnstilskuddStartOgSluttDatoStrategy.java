package no.nav.tag.tiltaksgjennomforing.avtale.startOgSluttDatoStrategy;

import no.nav.tag.tiltaksgjennomforing.enhet.Kvalifiseringsgruppe;
import no.nav.tag.tiltaksgjennomforing.exceptions.Feilkode;
import no.nav.tag.tiltaksgjennomforing.exceptions.FeilkodeException;

import java.time.LocalDate;

public class MidlertidigLonnstilskuddStartOgSluttDatoStrategy implements StartOgSluttDatoStrategy {
    private static final int TJUEFIRE_MND_MAKS_LENGDE = 24;
    private static final int TOLV_MND_MAKS_LENGDE = 12;
    private final Kvalifiseringsgruppe kvalifiseringsgruppe;

    MidlertidigLonnstilskuddStartOgSluttDatoStrategy(Kvalifiseringsgruppe kvalifiseringsgruppe) {
        this.kvalifiseringsgruppe = kvalifiseringsgruppe;
    }

    @Override
    public void sjekkStartOgSluttDato(LocalDate startDato, LocalDate sluttDato, boolean erGodkjentForEtterregistrering, boolean erAvtaleInngått) {
        StartOgSluttDatoStrategy.super.sjekkStartOgSluttDato(startDato, sluttDato, erGodkjentForEtterregistrering, erAvtaleInngått);

        if (startDato != null && sluttDato != null) {
            if ((kvalifiseringsgruppe == Kvalifiseringsgruppe.SPESIELT_TILPASSET_INNSATS || kvalifiseringsgruppe == Kvalifiseringsgruppe.VARIG_TILPASSET_INNSATS)
                    && startDato.plusMonths(TJUEFIRE_MND_MAKS_LENGDE).minusDays(1).isBefore(sluttDato)) {
                throw new FeilkodeException(Feilkode.VARIGHET_FOR_LANG_MIDLERTIDIG_LONNSTILSKUDD_24_MND);
            }

            if (kvalifiseringsgruppe == Kvalifiseringsgruppe.SITUASJONSBESTEMT_INNSATS && startDato.plusMonths(TOLV_MND_MAKS_LENGDE).minusDays(1).isBefore(sluttDato)) {
                throw new FeilkodeException(Feilkode.VARIGHET_FOR_LANG_MIDLERTIDIG_LONNSTILSKUDD_12_MND);
            }

            // Ikke funnet kvalifiseringsgruppe, default 12 mnd
            if (kvalifiseringsgruppe == null && startDato.plusMonths(TOLV_MND_MAKS_LENGDE).minusDays(1).isBefore(sluttDato)) {
                throw new FeilkodeException(Feilkode.VARIGHET_FOR_LANG_MIDLERTIDIG_LONNSTILSKUDD_12_MND);
            }
        }
    }
}
