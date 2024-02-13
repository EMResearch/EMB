package no.nav.tag.tiltaksgjennomforing.varsel;

import static no.nav.tag.tiltaksgjennomforing.avtale.Avtalerolle.ARBEIDSGIVER;
import static no.nav.tag.tiltaksgjennomforing.avtale.Avtalerolle.BESLUTTER;
import static no.nav.tag.tiltaksgjennomforing.avtale.Avtalerolle.DELTAKER;
import static no.nav.tag.tiltaksgjennomforing.avtale.Avtalerolle.MENTOR;
import static no.nav.tag.tiltaksgjennomforing.avtale.Avtalerolle.VEILEDER;
import static no.nav.tag.tiltaksgjennomforing.avtale.HendelseType.*;
import static no.nav.tag.tiltaksgjennomforing.avtale.TestData.avtalerMedTilskuddsperioder;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;

import no.nav.tag.tiltaksgjennomforing.Miljø;
import no.nav.tag.tiltaksgjennomforing.avtale.Arbeidsgiver;
import no.nav.tag.tiltaksgjennomforing.avtale.Avslagsårsak;
import no.nav.tag.tiltaksgjennomforing.avtale.Avtale;
import no.nav.tag.tiltaksgjennomforing.avtale.AvtaleInnholdRepository;
import no.nav.tag.tiltaksgjennomforing.avtale.AvtaleRepository;
import no.nav.tag.tiltaksgjennomforing.avtale.Avtalerolle;
import no.nav.tag.tiltaksgjennomforing.avtale.BedriftNr;
import no.nav.tag.tiltaksgjennomforing.avtale.Beslutter;
import no.nav.tag.tiltaksgjennomforing.avtale.Deltaker;
import no.nav.tag.tiltaksgjennomforing.avtale.EndreStillingsbeskrivelse;
import no.nav.tag.tiltaksgjennomforing.avtale.Fnr;
import no.nav.tag.tiltaksgjennomforing.avtale.HendelseType;
import no.nav.tag.tiltaksgjennomforing.avtale.NavIdent;
import no.nav.tag.tiltaksgjennomforing.avtale.OpprettAvtale;
import no.nav.tag.tiltaksgjennomforing.avtale.OpprettMentorAvtale;
import no.nav.tag.tiltaksgjennomforing.avtale.TestData;
import no.nav.tag.tiltaksgjennomforing.avtale.Tiltakstype;
import no.nav.tag.tiltaksgjennomforing.avtale.Veileder;
import no.nav.tag.tiltaksgjennomforing.datadeling.AvtaleMeldingEntitetRepository;
import no.nav.tag.tiltaksgjennomforing.datavarehus.DvhMeldingEntitetRepository;
import no.nav.tag.tiltaksgjennomforing.enhet.Kvalifiseringsgruppe;
import no.nav.tag.tiltaksgjennomforing.enhet.VeilarbArenaClient;
import no.nav.tag.tiltaksgjennomforing.utils.Now;
import no.nav.tag.tiltaksgjennomforing.varsel.notifikasjon.ArbeidsgiverNotifikasjonRepository;
import no.nav.tag.tiltaksgjennomforing.varsel.oppgave.LagGosysVarselLytter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles(Miljø.LOCAL)
@DirtiesContext
class LagVarselFraAvtaleHendelserTest {
    @Autowired
    AvtaleRepository avtaleRepository;
    @Autowired
    AvtaleInnholdRepository avtaleInnholdRepository;
    @Autowired
    VarselRepository varselRepository;
    @Autowired
    ArbeidsgiverNotifikasjonRepository arbeidsgiverNotifikasjonRepository;
    @Autowired
    DvhMeldingEntitetRepository dvhMeldingEntitetRepository;
    @Autowired
    AvtaleMeldingEntitetRepository avtaleMeldingEntitetRepository;
    @MockBean
    LagGosysVarselLytter lagGosysVarselLytter;
    @Autowired
    VeilarbArenaClient veilarbArenaClient;
    @Autowired
    SmsRepository smsRepository;

    @BeforeEach
    void setUp() {
        smsRepository.deleteAll();
        varselRepository.deleteAll();
        arbeidsgiverNotifikasjonRepository.deleteAll();
        avtaleInnholdRepository.deleteAll();
        dvhMeldingEntitetRepository.deleteAll();
        avtaleMeldingEntitetRepository.deleteAll();
        avtaleRepository.deleteAll();
    }

    @Test
    void test_alt() {
        Avtale avtale = avtaleRepository.save(Avtale.veilederOppretterAvtale(new OpprettAvtale(new Fnr("00000000000"), new BedriftNr("999999999"), Tiltakstype.MIDLERTIDIG_LONNSTILSKUDD), TestData.enNavIdent()));
        avtale.setKvalifiseringsgruppe(Kvalifiseringsgruppe.VARIG_TILPASSET_INNSATS);

        assertHendelse(OPPRETTET, VEILEDER, VEILEDER, false);
        assertHendelse(OPPRETTET, VEILEDER, ARBEIDSGIVER, true);
        assertHendelse(OPPRETTET, VEILEDER, DELTAKER, true);

        avtale.endreAvtale(Now.instant(), TestData.endringPåAlleLønnstilskuddFelter(), ARBEIDSGIVER, avtalerMedTilskuddsperioder);
        avtale = avtaleRepository.save(avtale);
        assertHendelse(ENDRET, ARBEIDSGIVER, VEILEDER, true);
        assertHendelse(ENDRET, ARBEIDSGIVER, ARBEIDSGIVER, false);
        assertHendelse(ENDRET, ARBEIDSGIVER, DELTAKER, true);

        avtale.togglegodkjennEtterregistrering(TestData.enNavIdent());
        avtale = avtaleRepository.save(avtale);
        assertHendelse(GODKJENT_FOR_ETTERREGISTRERING, BESLUTTER, VEILEDER, true);

        avtale.togglegodkjennEtterregistrering(TestData.enNavIdent());
        avtale = avtaleRepository.save(avtale);
        assertHendelse(FJERNET_ETTERREGISTRERING, BESLUTTER, VEILEDER, true);

        avtale.delMedAvtalepart(DELTAKER);
        avtale = avtaleRepository.save(avtale);
        assertHendelse(DELT_MED_DELTAKER, VEILEDER, VEILEDER, false);
        assertHendelse(DELT_MED_DELTAKER, VEILEDER, DELTAKER, true);
        assertIngenHendelse(DELT_MED_DELTAKER, ARBEIDSGIVER);

        avtale.delMedAvtalepart(ARBEIDSGIVER);
        avtale = avtaleRepository.save(avtale);
        assertHendelse(DELT_MED_ARBEIDSGIVER, VEILEDER, VEILEDER, false);
        assertHendelse(DELT_MED_ARBEIDSGIVER, VEILEDER, ARBEIDSGIVER, true);
        assertIngenHendelse(DELT_MED_ARBEIDSGIVER, DELTAKER);

        Deltaker deltaker = TestData.enDeltaker(avtale);
        deltaker.godkjennAvtale(Now.instant(), avtale);
        avtale = avtaleRepository.save(avtale);
        assertHendelse(GODKJENT_AV_DELTAKER, DELTAKER, VEILEDER, true);
        assertHendelse(GODKJENT_AV_DELTAKER, DELTAKER, ARBEIDSGIVER, true);
        assertHendelse(GODKJENT_AV_DELTAKER, DELTAKER, DELTAKER, false);

        Veileder veileder = TestData.enVeileder(avtale);
        veileder.opphevGodkjenninger(avtale);
        avtale = avtaleRepository.save(avtale);
        assertHendelse(GODKJENNINGER_OPPHEVET_AV_VEILEDER, VEILEDER, VEILEDER, false);
        assertHendelse(GODKJENNINGER_OPPHEVET_AV_VEILEDER, VEILEDER, ARBEIDSGIVER, true);
        assertHendelse(GODKJENNINGER_OPPHEVET_AV_VEILEDER, VEILEDER, DELTAKER, true);

        Arbeidsgiver arbeidsgiver = TestData.enArbeidsgiver(avtale);
        arbeidsgiver.godkjennAvtale(Now.instant(), avtale);
        avtale = avtaleRepository.save(avtale);
        assertHendelse(GODKJENT_AV_ARBEIDSGIVER, ARBEIDSGIVER, VEILEDER, true);
        assertHendelse(GODKJENT_AV_ARBEIDSGIVER, ARBEIDSGIVER, ARBEIDSGIVER, false);
        assertHendelse(GODKJENT_AV_ARBEIDSGIVER, ARBEIDSGIVER, DELTAKER, true);

        arbeidsgiver.opphevGodkjenninger(avtale);
        avtale = avtaleRepository.save(avtale);
        assertHendelse(GODKJENNINGER_OPPHEVET_AV_ARBEIDSGIVER, ARBEIDSGIVER, VEILEDER, true);
        assertHendelse(GODKJENNINGER_OPPHEVET_AV_ARBEIDSGIVER, ARBEIDSGIVER, ARBEIDSGIVER, false);
        assertHendelse(GODKJENNINGER_OPPHEVET_AV_ARBEIDSGIVER, ARBEIDSGIVER, DELTAKER, true);

        arbeidsgiver.godkjennAvtale(Now.instant(), avtale);
        veileder.godkjennForVeilederOgDeltaker(TestData.enGodkjentPaVegneGrunn(), avtale);
        avtale = avtaleRepository.save(avtale);
        assertHendelse(GODKJENT_PAA_VEGNE_AV, VEILEDER, VEILEDER, false);
        assertIngenHendelse(GODKJENT_PAA_VEGNE_AV, ARBEIDSGIVER);
        assertIngenHendelse(GODKJENT_PAA_VEGNE_AV, DELTAKER);

        Beslutter beslutter = TestData.enBeslutter(avtale);
        beslutter.avslåTilskuddsperiode(avtale, EnumSet.of(Avslagsårsak.FEIL_I_REGELFORSTÅELSE), "Forklaring");
        avtale = avtaleRepository.save(avtale);
        assertHendelse(TILSKUDDSPERIODE_AVSLATT, BESLUTTER, VEILEDER, true);
        assertIngenHendelse(TILSKUDDSPERIODE_AVSLATT, ARBEIDSGIVER);
        assertIngenHendelse(TILSKUDDSPERIODE_AVSLATT, DELTAKER);

        veileder.sendTilbakeTilBeslutter(avtale);
        beslutter.godkjennTilskuddsperiode(avtale, TestData.ENHET_OPPFØLGING.getVerdi());
        avtale = avtaleRepository.save(avtale);
        assertHendelse(TILSKUDDSPERIODE_GODKJENT, BESLUTTER, VEILEDER, true);
        assertIngenHendelse(TILSKUDDSPERIODE_GODKJENT, ARBEIDSGIVER);
        assertIngenHendelse(TILSKUDDSPERIODE_GODKJENT, DELTAKER);

        veileder.endreStillingbeskrivelse(EndreStillingsbeskrivelse.builder().stillingstittel("Tittel").arbeidsoppgaver("Oppgaver").stillingprosent(100).stillingKonseptId(1).stillingStyrk08(1).antallDagerPerUke(5).build(), avtale);
        avtale = avtaleRepository.save(avtale);
        assertHendelse(STILLINGSBESKRIVELSE_ENDRET, VEILEDER, VEILEDER, false);
        assertHendelse(STILLINGSBESKRIVELSE_ENDRET, VEILEDER, ARBEIDSGIVER, true);
        assertHendelse(STILLINGSBESKRIVELSE_ENDRET, VEILEDER, DELTAKER, true);

        Veileder nyVeileder = TestData.enVeileder(new NavIdent("I000000"));
        nyVeileder.overtaAvtale(avtale);
        avtale = avtaleRepository.save(avtale);
        assertHendelse(NY_VEILEDER, VEILEDER, VEILEDER, false);
        assertHendelse(NY_VEILEDER, VEILEDER, ARBEIDSGIVER, true);
        assertHendelse(NY_VEILEDER, VEILEDER, DELTAKER, true);
    }

    @Test
    void test_for_arbeidsgiver_oppretter() {
        Avtale avtale = avtaleRepository.save(Avtale.arbeidsgiverOppretterAvtale(new OpprettAvtale(new Fnr("00000000000"), new BedriftNr("999999999"), Tiltakstype.MIDLERTIDIG_LONNSTILSKUDD)));

        assertHendelse(OPPRETTET_AV_ARBEIDSGIVER, ARBEIDSGIVER, VEILEDER, true);
        assertHendelse(OPPRETTET_AV_ARBEIDSGIVER, ARBEIDSGIVER, ARBEIDSGIVER, false);
        assertHendelse(OPPRETTET_AV_ARBEIDSGIVER, ARBEIDSGIVER, DELTAKER, true);

        Veileder veileder = TestData.enVeileder(TestData.enNavIdent());
        veileder.overtaAvtale(avtale);
        avtale = avtaleRepository.save(avtale);
        assertHendelse(AVTALE_FORDELT, VEILEDER, VEILEDER, false);
        assertHendelse(AVTALE_FORDELT, VEILEDER, ARBEIDSGIVER, true);
        assertHendelse(AVTALE_FORDELT, VEILEDER, DELTAKER, true);
    }

    @Test
    void test_for_arbeidsgiver_oppretter_mentor_avtale() {
        Avtale avtale = avtaleRepository.save(Avtale.arbeidsgiverOppretterAvtale(new OpprettMentorAvtale(new Fnr("00000000000"),new Fnr("00000000000"), new BedriftNr("999999999"), Tiltakstype.MENTOR, ARBEIDSGIVER)));

        assertHendelse(OPPRETTET_AV_ARBEIDSGIVER, ARBEIDSGIVER, VEILEDER, true);
        assertHendelse(OPPRETTET_AV_ARBEIDSGIVER, ARBEIDSGIVER, ARBEIDSGIVER, false);
        assertHendelse(OPPRETTET_AV_ARBEIDSGIVER, ARBEIDSGIVER, DELTAKER, true);

        Veileder veileder = TestData.enVeileder(TestData.enNavIdent());
        veileder.overtaAvtale(avtale);
        avtale = avtaleRepository.save(avtale);
        assertHendelse(AVTALE_FORDELT, VEILEDER, VEILEDER, false);
        assertHendelse(AVTALE_FORDELT, VEILEDER, ARBEIDSGIVER, true);
        assertHendelse(AVTALE_FORDELT, VEILEDER, DELTAKER, true);
        assertHendelse(AVTALE_FORDELT, VEILEDER, MENTOR, true);
    }

    @Test
    void test_for_delt_med_mentor() {
        Avtale avtale = avtaleRepository.save(Avtale.veilederOppretterAvtale(new OpprettMentorAvtale(new Fnr("00000000000") , new Fnr("00000000000"), new BedriftNr("999999999"), Tiltakstype.MENTOR, VEILEDER), TestData.enNavIdent()));
        avtale.setKvalifiseringsgruppe(Kvalifiseringsgruppe.VARIG_TILPASSET_INNSATS);
        avtale.endreAvtale(Now.instant(), TestData.endringPåAlleMentorFelter(), VEILEDER, avtalerMedTilskuddsperioder);
        avtale = avtaleRepository.save(avtale);

        avtale.delMedAvtalepart(DELTAKER);
        avtale = avtaleRepository.save(avtale);
        assertHendelse(DELT_MED_DELTAKER, VEILEDER, VEILEDER, false);
        assertHendelse(DELT_MED_DELTAKER, VEILEDER, DELTAKER, true);
        assertIngenHendelse(DELT_MED_DELTAKER, ARBEIDSGIVER);

        avtale.delMedAvtalepart(MENTOR);
        avtale = avtaleRepository.save(avtale);
        assertHendelse(DELT_MED_MENTOR, VEILEDER, VEILEDER, false);
        assertHendelse(DELT_MED_MENTOR, VEILEDER, MENTOR, true);
        assertIngenHendelse(DELT_MED_MENTOR, ARBEIDSGIVER);

    }

    @Test
    void forleng_avtale() {
        Avtale avtale = TestData.enLonnstilskuddAvtaleGodkjentAvVeileder();
        avtale.setKvalifiseringsgruppe(Kvalifiseringsgruppe.VARIG_TILPASSET_INNSATS);
        avtale = avtaleRepository.save(avtale);
        Veileder veileder = TestData.enVeileder(avtale);

        veileder.forlengAvtale(avtale.getGjeldendeInnhold().getSluttDato().plusMonths(1), avtale);
        avtaleRepository.save(avtale);

        assertHendelse(AVTALE_FORLENGET, VEILEDER, VEILEDER, false);
        assertHendelse(AVTALE_FORLENGET, VEILEDER, ARBEIDSGIVER, true);
        assertHendelse(AVTALE_FORLENGET, VEILEDER, DELTAKER, true);
    }

    @Test
    void endre_tilskuddsberegning() {
        Avtale avtale = avtaleRepository.save(TestData.enLonnstilskuddAvtaleGodkjentAvVeileder());
        Veileder veileder = TestData.enVeileder(avtale);

        veileder.endreTilskuddsberegning(TestData.enEndreTilskuddsberegning(), avtale);
        avtaleRepository.save(avtale);

        assertHendelse(TILSKUDDSBEREGNING_ENDRET, VEILEDER, VEILEDER, false);
        assertHendelse(TILSKUDDSBEREGNING_ENDRET, VEILEDER, ARBEIDSGIVER, true);
        assertHendelse(TILSKUDDSBEREGNING_ENDRET, VEILEDER, DELTAKER, true);
    }

    private void assertHendelse(HendelseType hendelseType, Avtalerolle utførtAv, Avtalerolle mottaker, boolean bjelle) {
        assertThat(varselRepository.findAll())
                .filteredOn(varsel -> varsel.getMottaker() == mottaker && varsel.getUtførtAv() == utførtAv && varsel.getHendelseType() == hendelseType && varsel.isBjelle() == bjelle)
                .hasSize(1);
    }

    private void assertIngenHendelse(HendelseType hendelseType, Avtalerolle mottaker) {
        assertThat(varselRepository.findAll())
                .filteredOn(varsel -> varsel.getMottaker() == mottaker && varsel.getHendelseType() == hendelseType)
                .isEmpty();
    }
}