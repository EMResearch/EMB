package no.nav.tag.tiltaksgjennomforing.avtale;

import no.nav.tag.tiltaksgjennomforing.enhet.Kvalifiseringsgruppe;
import no.nav.tag.tiltaksgjennomforing.exceptions.Feilkode;
import no.nav.tag.tiltaksgjennomforing.utils.Now;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static no.nav.tag.tiltaksgjennomforing.AssertFeilkode.assertFeilkode;
import static no.nav.tag.tiltaksgjennomforing.avtale.TestData.avtalerMedTilskuddsperioder;
import static no.nav.tag.tiltaksgjennomforing.avtale.Tiltakstype.MIDLERTIDIG_LONNSTILSKUDD;
import static no.nav.tag.tiltaksgjennomforing.avtale.Tiltakstype.VARIG_LONNSTILSKUDD;

class LonnstilskuddStartOgSluttDatoStrategyTest {

    private static Avtale enMidlertidig() {
        return Avtale.veilederOppretterAvtale(new OpprettAvtale(TestData.etFodselsnummer(), TestData.etBedriftNr(), MIDLERTIDIG_LONNSTILSKUDD), TestData.enNavIdent());
    }

    private static Avtale enVarig() {
        return Avtale.veilederOppretterAvtale(new OpprettAvtale(TestData.etFodselsnummer(), TestData.etBedriftNr(), VARIG_LONNSTILSKUDD), TestData.enNavIdent());
    }

    private void endreAvtale(Avtale avtale, EndreAvtale endreAvtale) {
        avtale.endreAvtale(Now.instant(), endreAvtale, Avtalerolle.VEILEDER, avtalerMedTilskuddsperioder);
    }

    @Test
    public void endreMidlertidigLønnstilskudd__startdato_og_sluttdato_satt_24mnd() {
        EndreAvtale endreAvtale = TestData.endringPåAlleLønnstilskuddFelter();
        LocalDate startDato = Now.localDate();
        LocalDate sluttDato = startDato.plusMonths(24).minusDays(1);
        endreAvtale.setStartDato(startDato);
        endreAvtale.setSluttDato(sluttDato);
        Avtale avtale = enMidlertidig();
        avtale.setKvalifiseringsgruppe(Kvalifiseringsgruppe.VARIG_TILPASSET_INNSATS);
        endreAvtale(avtale, endreAvtale);
    }

    @Test
    public void endreMidlertidigLønnstilskudd__startdato_og_sluttdato_satt_over_24mnd__SPESIELT_TILPASSET_INNSATS() {
        EndreAvtale endreAvtale = TestData.endringPåAlleLønnstilskuddFelter();
        LocalDate startDato = Now.localDate();
        LocalDate sluttDato = startDato.plusMonths(24).plusDays(1);
        endreAvtale.setStartDato(startDato);
        endreAvtale.setSluttDato(sluttDato);
        Avtale avtale = enMidlertidig();
        avtale.setKvalifiseringsgruppe(Kvalifiseringsgruppe.SPESIELT_TILPASSET_INNSATS);
        assertFeilkode(Feilkode.VARIGHET_FOR_LANG_MIDLERTIDIG_LONNSTILSKUDD_24_MND, () -> endreAvtale(avtale, endreAvtale));
    }

    @Test
    public void endreMidlertidigLønnstilskudd__startdato_og_sluttdato_satt_over_24mnd__VARIG_TILPASSET_INNSATS() {
        EndreAvtale endreAvtale = TestData.endringPåAlleLønnstilskuddFelter();
        LocalDate startDato = Now.localDate();
        LocalDate sluttDato = startDato.plusMonths(24).plusDays(1);
        endreAvtale.setStartDato(startDato);
        endreAvtale.setSluttDato(sluttDato);
        Avtale avtale = enMidlertidig();
        avtale.setKvalifiseringsgruppe(Kvalifiseringsgruppe.VARIG_TILPASSET_INNSATS);
        assertFeilkode(Feilkode.VARIGHET_FOR_LANG_MIDLERTIDIG_LONNSTILSKUDD_24_MND, () -> endreAvtale(avtale, endreAvtale));
    }

    @Test
    public void endreMidlertidigLønnstilskudd__startdato_og_sluttdato_satt_over_24mnd__SITUASJONSBESTEMT_INNSATS() {
        EndreAvtale endreAvtale = TestData.endringPåAlleLønnstilskuddFelter();
        LocalDate startDato = Now.localDate();
        LocalDate sluttDato = startDato.plusMonths(12).plusDays(1);
        endreAvtale.setStartDato(startDato);
        endreAvtale.setSluttDato(sluttDato);
        Avtale avtale = enMidlertidig();
        avtale.setKvalifiseringsgruppe(Kvalifiseringsgruppe.SITUASJONSBESTEMT_INNSATS);
        assertFeilkode(Feilkode.VARIGHET_FOR_LANG_MIDLERTIDIG_LONNSTILSKUDD_12_MND, () -> endreAvtale(avtale, endreAvtale));
    }

    @Test
    public void endreMidlertidigLønnstilskudd__startdato_og_sluttdato_satt_over_12mnd__ikke_satt() {
        EndreAvtale endreAvtale = TestData.endringPåAlleLønnstilskuddFelter();
        LocalDate startDato = Now.localDate();
        LocalDate sluttDato = startDato.plusMonths(12).plusDays(1);
        endreAvtale.setStartDato(startDato);
        endreAvtale.setSluttDato(sluttDato);
        Avtale avtale = enMidlertidig();
        avtale.setKvalifiseringsgruppe(null);
        assertFeilkode(Feilkode.VARIGHET_FOR_LANG_MIDLERTIDIG_LONNSTILSKUDD_12_MND, () -> endreAvtale(avtale, endreAvtale));
    }

    @Test
    public void endreVarigLønnstilskudd__startdato_og_sluttdato_satt_over_24mnd() {
        EndreAvtale endreAvtale = TestData.endringPåAlleLønnstilskuddFelter();
        LocalDate startDato = Now.localDate();
        LocalDate sluttDato = startDato.plusMonths(24).plusDays(1);
        endreAvtale.setStartDato(startDato);
        endreAvtale.setSluttDato(sluttDato);
        endreAvtale(enVarig(), endreAvtale);
    }
}