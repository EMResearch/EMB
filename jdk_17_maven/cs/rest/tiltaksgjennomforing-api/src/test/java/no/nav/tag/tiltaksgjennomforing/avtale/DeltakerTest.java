package no.nav.tag.tiltaksgjennomforing.avtale;

import no.nav.tag.tiltaksgjennomforing.exceptions.Feilkode;
import no.nav.tag.tiltaksgjennomforing.exceptions.KanIkkeOppheveException;
import no.nav.tag.tiltaksgjennomforing.utils.Now;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static no.nav.tag.tiltaksgjennomforing.AssertFeilkode.assertFeilkode;
import static no.nav.tag.tiltaksgjennomforing.avtale.TestData.avtalerMedTilskuddsperioder;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DeltakerTest {
    private AvtaleRepository avtaleRepository = mock(AvtaleRepository.class);
    @Test
    public void opphevGodkjenninger__kan_aldri_oppheve_godkjenninger() {
        Avtale avtale = TestData.enAvtaleMedAltUtfylt();
        Deltaker deltaker = TestData.enDeltaker(avtale);
        assertThatThrownBy(() -> deltaker.opphevGodkjenninger(avtale)).isInstanceOf(KanIkkeOppheveException.class);
    }

    @Test
    public void mentor_Avtaler__skjul_mentor_fnr_for_deltaker() {
        Pageable pageable = PageRequest.of(0, 100);
        Avtale avtale = TestData.enMentorAvtaleSignert();
        Deltaker deltaker = TestData.enDeltaker(avtale);
        AvtalePredicate avtalePredicate = new AvtalePredicate();
        when(avtaleRepository.findAllByDeltakerFnr(any(), eq(pageable))).thenReturn(new PageImpl<Avtale>(List.of(avtale)));
        Page<Avtale> avtaler = deltaker.hentAlleAvtalerMedMuligTilgang(avtaleRepository, avtalePredicate, pageable);
        assertThat(avtaler.getContent().get(0).getMentorFnr()).isNull();
        assertThat(avtaler.getContent().get(0).getGjeldendeInnhold().getMentorTimelonn()).isNull();

    }

    @Test
    public void mentor_en_Avtale__skjul_mentor_fnr_for_deltaker() {
        Avtale avtale = TestData.enMentorAvtaleSignert();
        Deltaker deltaker = TestData.enDeltaker(avtale);
        AvtalePredicate avtalePredicate = new AvtalePredicate();
        when(avtaleRepository.findById(any())).thenReturn(Optional.of(avtale));

        Avtale avtaler = deltaker.hentAvtale(avtaleRepository,avtale.getId());
        assertThat(avtaler.getMentorFnr()).isNull();
        assertThat(avtaler.getGjeldendeInnhold().getMentorTimelonn()).isNull();

    }

    @Test
    public void deltaker_alder_ikke_eldre_enn_72() {
        Now.fixedDate(LocalDate.of(2021, 01, 20));
        Avtale avtale = Avtale.veilederOppretterAvtale(new OpprettAvtale(new Fnr("30015521534"), TestData.etBedriftNr(), Tiltakstype.VARIG_LONNSTILSKUDD), TestData.enNavIdent());
        EndreAvtale endreAvtale = TestData.endringPåAlleLønnstilskuddFelter();
        endreAvtale.setStartDato(LocalDate.of(2021, 6, 1));
        endreAvtale.setSluttDato(LocalDate.of(2027, 1, 30));
        avtale.endreAvtale(Instant.now(), endreAvtale, Avtalerolle.VEILEDER, avtalerMedTilskuddsperioder);
        avtale.godkjennForArbeidsgiver(TestData.enIdentifikator());
        assertFeilkode(Feilkode.DELTAKER_72_AAR, () ->  avtale.godkjennForVeilederOgDeltaker(TestData.enNavIdent(), TestData.enGodkjentPaVegneGrunn()));

        Now.resetClock();
    }

    @Test
    public void deltaker_alder_ikke_eldre_enn_67() {
        Now.fixedDate(LocalDate.of(2021, 01, 20));
        Avtale avtale = Avtale.veilederOppretterAvtale(new OpprettAvtale(new Fnr("30015521534"), TestData.etBedriftNr(), Tiltakstype.MIDLERTIDIG_LONNSTILSKUDD), TestData.enNavIdent());
        EndreAvtale endreAvtale = TestData.endringPåAlleLønnstilskuddFelter();
        avtale.getGjeldendeInnhold().setLonnstilskuddProsent(60);
        endreAvtale.setStartDato(LocalDate.of(2021, 6, 1));
        endreAvtale.setSluttDato(LocalDate.of(2022, 1, 30));
        avtale.endreAvtale(Instant.now(), endreAvtale, Avtalerolle.VEILEDER, avtalerMedTilskuddsperioder);
        avtale.godkjennForArbeidsgiver(TestData.enIdentifikator());
        assertFeilkode(Feilkode.DELTAKER_67_AAR, () ->  avtale.godkjennForVeilederOgDeltaker(TestData.enNavIdent(), TestData.enGodkjentPaVegneGrunn()));

        Now.resetClock();
    }
}