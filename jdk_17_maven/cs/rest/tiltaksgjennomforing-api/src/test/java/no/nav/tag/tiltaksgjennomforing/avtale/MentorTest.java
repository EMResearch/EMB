package no.nav.tag.tiltaksgjennomforing.avtale;

import static no.nav.tag.tiltaksgjennomforing.AssertFeilkode.assertFeilkode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import no.nav.tag.tiltaksgjennomforing.exceptions.Feilkode;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public class MentorTest {

    private AvtaleRepository avtaleRepository = mock(AvtaleRepository.class);

    private Pageable pageable = PageRequest.of(0, 100);

    @Test
    public void hentAlleAvtalerMedMuligTilgang__mentor_en_avtale() {

        // GITT
        Avtale avtaleUsignert = TestData.enMentorAvtaleUsignert();
        Avtale avtaleSignert = TestData.enMentorAvtaleSignert();
        Mentor mentor = TestData.enMentor(avtaleSignert);
        AvtalePredicate avtalePredicate = new AvtalePredicate();

        // NÅR
        when(avtaleRepository.findAllByMentorFnr(any(), eq(pageable))).thenReturn(new PageImpl<Avtale>(List.of(avtaleUsignert, avtaleSignert)));
        Page<Avtale> avtaler = mentor.hentAlleAvtalerMedMuligTilgang(avtaleRepository, avtalePredicate, pageable);

        assertThat(avtaler.getTotalElements()).isEqualTo(2);
        assertThat(avtaler.getContent().get(0)).isEqualTo(avtaleUsignert);
        assertThat(avtaler.getContent().get(1)).isEqualTo(avtaleSignert);
        assertThat(avtaler.getContent().get(0).getDeltakerFnr()).isNull();
        assertThat(avtaler.getContent().get(1).getDeltakerFnr()).isNull();
     }

    @Test
    public void deltakerFNR_skal_være_null_selv_om_mentor_har_signert(){
        // GITT
        Avtale avtaleSignert = TestData.enMentorAvtaleSignert();
        Mentor mentor = TestData.enMentor(avtaleSignert);
        // NÅR
        when(avtaleRepository.findById(any())).thenReturn(Optional.of(avtaleSignert));
        Avtale avtale = mentor.hentAvtale(avtaleRepository, avtaleSignert.getId());

        assertThat(avtale).isEqualTo(avtaleSignert);
        assertThat(avtale.getDeltakerFnr()).isNull();
    }

    @Test
    public void om_mentor_har_tilgang_til_en_annen_mentors_avtale() {

        // GITT
        Avtale avtale = TestData.enMentorAvtaleUsignert();
        Mentor mentor = new Mentor(new Fnr("77665521872"));
        // NÅR
        boolean hartilgang = mentor.harTilgangTilAvtale(avtale);
        assertFalse(hartilgang);
    }

    @Test
    public void om_mentor_har_tilgang_til_en_annen_mentors_avtale_TestDataTest() {

        // GITT
        Avtale avtale = TestData.enMentorAvtaleUsignert();
        Mentor mentor = new Mentor(new Fnr("77665521872"));
        // NÅR
        boolean hartilgang = mentor.harTilgangTilAvtale(avtale);
        assertFalse(hartilgang);
    }

    @Test
    public void hentAlleAvtalerMedMuligTilgang__mentor_en_ikke_signert_avtale_skal_returnere_avtale_med_kun_bedrift_navn() {

        // GITT
        Avtale avtale = TestData.enMentorAvtaleUsignert();
        Mentor mentor = TestData.enMentor(avtale);
        AvtalePredicate avtalePredicate = new AvtalePredicate();
        // NÅR
        when(avtaleRepository.findAllByMentorFnr(any(), eq(pageable))).thenReturn(new PageImpl<Avtale>(List.of(avtale)));
        Page<Avtale> avtaler = mentor.hentAlleAvtalerMedMuligTilgang(avtaleRepository, avtalePredicate, pageable);

        assertThat(avtaler).isNotEmpty();
        assertThat(avtaler.getContent().get(0).getDeltakerFnr()).isNull();
        assertThat(avtaler.getContent().get(0).getVeilederNavIdent()).isNull();
        assertThat(avtaler.getContent().get(0).getGjeldendeInnhold().getDeltakerFornavn()).isNull();
        assertThat(avtaler.getContent().get(0).getGjeldendeInnhold().getDeltakerEtternavn()).isNull();
        assertThat(avtaler.getContent().get(0).getGjeldendeInnhold().getVeilederTlf()).isNull();
        assertThat(avtaler.getContent().get(0).getGjeldendeInnhold().getArbeidsgiverKontonummer()).isNull();
        assertThat(avtaler.getContent().get(0).getBedriftNr()).isNotNull();
    }

    @Test
    public void endreOmMentor__må_være_en_mentor_avtale() {
        Avtale avtale = TestData.enMidlertidigLonnstilskuddAvtaleMedAltUtfylt();
        Arbeidsgiver arbeidsgiver = TestData.enArbeidsgiver(avtale);
        Veileder veileder = TestData.enVeileder(avtale);
        arbeidsgiver.godkjennAvtale(Instant.now(), avtale);
        veileder.godkjennForVeilederOgDeltaker(TestData.enGodkjentPaVegneGrunn(), avtale);
        EndreOmMentor endreOmMentor = new EndreOmMentor("Per", "Persen", "12345678", "litt mentorering", 5.0, 500);
        assertFeilkode(Feilkode.KAN_IKKE_ENDRE_FEIL_TILTAKSTYPE, () -> veileder.endreOmMentor(endreOmMentor, avtale));
    }

    @Test
    public void endreOmMentor__setter_riktige_felter() {
        Avtale avtale = TestData.enMentorAvtaleSignert();
        Arbeidsgiver arbeidsgiver = TestData.enArbeidsgiver(avtale);
        Veileder veileder = TestData.enVeileder(avtale);
        arbeidsgiver.godkjennAvtale(Instant.now(), avtale);
        veileder.godkjennForVeilederOgDeltaker(TestData.enGodkjentPaVegneGrunn(),avtale);
        //veileder.godkjennAvtale(Instant.now(), avtale);
        assertThat(avtale.getGjeldendeInnhold().getInnholdType()).isEqualTo(AvtaleInnholdType.INNGÅ);
        EndreOmMentor endreOmMentor = new EndreOmMentor("Per", "Persen", "12345678", "litt mentorering", 5.0, 500);
        veileder.endreOmMentor(endreOmMentor, avtale);
        assertThat(avtale.getGjeldendeInnhold().getMentorFornavn()).isEqualTo("Per");
        assertThat(avtale.getGjeldendeInnhold().getMentorEtternavn()).isEqualTo("Persen");
        assertThat(avtale.getGjeldendeInnhold().getMentorTlf()).isEqualTo("12345678");
        assertThat(avtale.getGjeldendeInnhold().getMentorOppgaver()).isEqualTo("litt mentorering");
        assertThat(avtale.getGjeldendeInnhold().getMentorAntallTimer()).isEqualTo(5);
        assertThat(avtale.getGjeldendeInnhold().getMentorTimelonn()).isEqualTo(500);
        assertThat(avtale.getGjeldendeInnhold().getInnholdType()).isEqualTo(AvtaleInnholdType.ENDRE_OM_MENTOR);
    }

    @Test
    public void endreOmMentor__avtale_må_være_inngått() {
        Avtale avtale = TestData.enMentorAvtaleSignert();
        Arbeidsgiver arbeidsgiver = TestData.enArbeidsgiver(avtale);
        Veileder veileder = TestData.enVeileder(avtale);
        arbeidsgiver.godkjennAvtale(Instant.now(), avtale);
        assertThat(avtale.erAvtaleInngått()).isFalse();
        assertFeilkode(
                Feilkode.KAN_IKKE_ENDRE_OM_MENTOR_IKKE_INNGAATT_AVTALE,
                () -> veileder.endreOmMentor(new EndreOmMentor("Per", "Persen", "12345678", "litt mentorering", 5.0, 500), avtale)
        );
    }

}
