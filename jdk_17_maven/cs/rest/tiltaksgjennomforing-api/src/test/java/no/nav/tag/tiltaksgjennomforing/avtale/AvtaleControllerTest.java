package no.nav.tag.tiltaksgjennomforing.avtale;

import no.nav.tag.tiltaksgjennomforing.autorisasjon.InnloggingService;
import no.nav.tag.tiltaksgjennomforing.autorisasjon.SlettemerkeProperties;
import no.nav.tag.tiltaksgjennomforing.autorisasjon.abac.TilgangskontrollService;
import no.nav.tag.tiltaksgjennomforing.enhet.Formidlingsgruppe;
import no.nav.tag.tiltaksgjennomforing.enhet.Kvalifiseringsgruppe;
import no.nav.tag.tiltaksgjennomforing.enhet.Norg2Client;
import no.nav.tag.tiltaksgjennomforing.enhet.Norg2GeoResponse;
import no.nav.tag.tiltaksgjennomforing.enhet.Oppfølgingsstatus;
import no.nav.tag.tiltaksgjennomforing.enhet.VeilarbArenaClient;
import no.nav.tag.tiltaksgjennomforing.exceptions.IkkeTilgangTilDeltakerException;
import no.nav.tag.tiltaksgjennomforing.exceptions.KanIkkeOppretteAvtalePåKode6Exception;
import no.nav.tag.tiltaksgjennomforing.exceptions.KontoregisterFeilException;
import no.nav.tag.tiltaksgjennomforing.exceptions.RessursFinnesIkkeException;
import no.nav.tag.tiltaksgjennomforing.exceptions.TilgangskontrollException;
import no.nav.tag.tiltaksgjennomforing.exceptions.TiltaksgjennomforingException;
import no.nav.tag.tiltaksgjennomforing.featuretoggles.enhet.NavEnhet;
import no.nav.tag.tiltaksgjennomforing.okonomi.KontoregisterService;
import no.nav.tag.tiltaksgjennomforing.orgenhet.EregService;
import no.nav.tag.tiltaksgjennomforing.orgenhet.Organisasjon;
import no.nav.tag.tiltaksgjennomforing.persondata.PdlRespons;
import no.nav.tag.tiltaksgjennomforing.persondata.PersondataService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static no.nav.tag.tiltaksgjennomforing.avtale.TestData.enArbeidstreningAvtale;
import static no.nav.tag.tiltaksgjennomforing.avtale.TestData.enNavIdent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@SuppressWarnings("rawtypes")
@ExtendWith(MockitoExtension.class)
public class AvtaleControllerTest {

    @Mock
    VeilarbArenaClient veilarbArenaClient;
    @Mock
    Norg2Client norg2Client;
    @Spy
    TilskuddsperiodeConfig tilskuddsperiodeConfig = new TilskuddsperiodeConfig();
    @InjectMocks
    private AvtaleController avtaleController;
    @Mock
    private AvtaleRepository avtaleRepository;
    @Mock
    private TilgangskontrollService tilgangskontrollService;
    @Mock
    private InnloggingService innloggingService;
    @Mock
    private EregService eregService;
    @Mock
    private PersondataService persondataService;
    @Mock
    private KontoregisterService kontoregisterService;

    private Pageable pageable = PageRequest.of(0, 100);

    private static List<Avtale> lagListeMedAvtaler(Avtale avtale, int antall) {
        List<Avtale> avtaler = new ArrayList<>();
        for (int i = 0; i <= antall; i++) {
            avtaler.add(avtale);
        }
        return avtaler;
    }

    private static OpprettAvtale lagOpprettAvtale() {
        Fnr deltakerFnr = new Fnr("00000000000");
        BedriftNr bedriftNr = new BedriftNr("12345678");
        return new OpprettAvtale(deltakerFnr, bedriftNr, Tiltakstype.ARBEIDSTRENING);
    }

    @Test
    public void hentSkalKasteResourceNotFoundExceptionHvisAvtaleIkkeFins() {
        Avtale avtale = TestData.enArbeidstreningAvtale();
        Veileder veileder = TestData.enVeileder(avtale);
        værInnloggetSom(veileder);
        when(avtaleRepository.findById(avtale.getId())).thenReturn(Optional.empty());
        assertThatThrownBy(
                () -> avtaleController.hent(avtale.getId(), Avtalerolle.VEILEDER)
        ).isExactlyInstanceOf(RessursFinnesIkkeException.class);
    }

    @Test
    public void hentSkalKastTilgangskontrollExceptionHvisInnloggetNavAnsattIkkeHarTilgang() {
        Avtale avtale = TestData.enArbeidstreningAvtale();
        værInnloggetSom(
                new Veileder(
                        new NavIdent("Z333333"),
                        tilgangskontrollService,
                        persondataService,
                        norg2Client,
                        Collections.emptySet(),
                        new SlettemerkeProperties(),
                        false,
                        veilarbArenaClient
                )
        );
        when(avtaleRepository.findById(avtale.getId())).thenReturn(Optional.of(avtale));
        assertThatThrownBy(
                () -> avtaleController.hent(avtale.getId(), Avtalerolle.VEILEDER)
        ).isExactlyInstanceOf(TilgangskontrollException.class);
    }

    @Disabled("må skrives om")
    @Test
    public void hentAvtalerOpprettetAvVeileder_skal_returnere_tom_liste_dersom_veileder_ikke_har_tilgang() {
        NavIdent veilederNavIdent = new NavIdent("Z222222");
        Avtale avtaleForVeilederSomSøkesEtter = Avtale.veilederOppretterAvtale(lagOpprettAvtale(), veilederNavIdent);
        NavIdent identTilInnloggetVeileder = new NavIdent("Z333333");
        Veileder veileder = new Veileder(
                identTilInnloggetVeileder,
                tilgangskontrollService,
                persondataService,
                norg2Client,
                Collections.emptySet(),
                new SlettemerkeProperties(),
                false,
                veilarbArenaClient
        );
        værInnloggetSom(veileder);
        Avtale exampleAvtale = Avtale.builder()
                .veilederNavIdent(new NavIdent("Z222222"))
                .build();
        when(
                avtaleRepository.findAll(eq(Example.of(exampleAvtale)), eq(pageable))
        ).thenReturn(new PageImpl<Avtale>(List.of(avtaleForVeilederSomSøkesEtter)));;
        when(tilgangskontrollService.harSkrivetilgangTilKandidat(
                eq(veileder),
                any(Fnr.class)
        )).thenReturn(false);
        AvtalePredicate avtalePredicate = new AvtalePredicate();

        Map<String, Object> avtalerPageResponse = veileder.hentAlleAvtalerMedLesetilgang(
                avtaleRepository,
                avtalePredicate.setVeilederNavIdent(veilederNavIdent),
                Avtale.Fields.sistEndret,
                pageable
        );

        List<Avtale> avtaler = (List<Avtale>)avtalerPageResponse.get("avtaler");
        assertThat(avtaler).doesNotContain(avtaleForVeilederSomSøkesEtter);
    }

    public void hentAvtaleOpprettetAvInnloggetVeileder_fordelt_oppfolgingsEnhet_og_geoEnhet() {
        NavIdent navIdent = new NavIdent("Z123456");
        String navEnhet = "0904";
        Veileder veileder = new Veileder(
                navIdent,
                tilgangskontrollService,
                persondataService,
                norg2Client,
                Collections.emptySet(),
                new SlettemerkeProperties(),
                false,
                veilarbArenaClient
        );
        værInnloggetSom(veileder);
        Avtale nyAvtaleMedGeografiskEnhet = TestData.enArbeidstreningAvtaleOpprettetAvArbeidsgiverOgErUfordeltMedOppfølgningsEnhetOgGeografiskEnhet();
        Avtale nyAvtaleMedOppfølgningsEnhet = TestData.enArbeidstreningAvtaleOpprettetAvArbeidsgiverOgErUfordeltMedOppfølgningsEnhet();

        when(
                avtaleRepository.findAllByEnhetGeografiskOrEnhetOppfolging(eq(navEnhet), eq(navEnhet), eq(pageable))
        ).thenReturn(new PageImpl<Avtale>(List.of(nyAvtaleMedGeografiskEnhet, nyAvtaleMedOppfølgningsEnhet)));
        when(tilgangskontrollService.harSkrivetilgangTilKandidat(eq(veileder), any(Fnr.class))).thenReturn(true);

        Map<String, Object> avtalerPageResponse = veileder.hentAlleAvtalerMedLesetilgang(
                avtaleRepository,
                new AvtalePredicate().setNavEnhet(navEnhet),
                Avtale.Fields.sistEndret,
                pageable
        );

        List<AvtaleMinimalListevisning> avtaler = (List<AvtaleMinimalListevisning>)avtalerPageResponse.get("avtaler");
        assertThat(avtaler).isNotNull();
    }

    @Disabled("må skrives om")
    @Test
    public void hentAvtaleOpprettetAvInnloggetVeileder_pa_avtaleNr() {
        NavIdent navIdent = new NavIdent("Z123456");
        Veileder veileder = new Veileder(
                navIdent,
                tilgangskontrollService,
                persondataService,
                norg2Client,
                Collections.emptySet(),
                new SlettemerkeProperties(),
                false,
                veilarbArenaClient
        );
        værInnloggetSom(veileder);

        Avtale enArbeidstreningsAvtale = TestData.enArbeidstreningAvtale();
        enArbeidstreningsAvtale.setAvtaleNr(TestData.ET_AVTALENR);

        Avtale exampleAvtale = Avtale.builder()
                .avtaleNr(TestData.ET_AVTALENR)
                .build();
        when(
                avtaleRepository.findAll(eq(Example.of(exampleAvtale)), eq(pageable))
        ).thenReturn(new PageImpl<Avtale>(List.of(enArbeidstreningsAvtale)));
        when(tilgangskontrollService.harSkrivetilgangTilKandidat(eq(veileder), any(Fnr.class))).thenReturn(true);

        Map<String, Object> avtalerPageResponse = veileder.hentAlleAvtalerMedLesetilgang(
                avtaleRepository,
                new AvtalePredicate().setAvtaleNr(TestData.ET_AVTALENR),
                Avtale.Fields.sistEndret,
                pageable
        );

        List<AvtaleMinimalListevisning> avtaler = (List<AvtaleMinimalListevisning>)avtalerPageResponse.get("avtaler");
        assertThat(avtaler).isNotNull();
        assertThat(avtaler.stream().filter(avtaleMinimalListevisning-> avtaleMinimalListevisning.getTiltakstype() == Tiltakstype.ARBEIDSTRENING).toList()).isNotNull();
    }

    @Test
    public void mentorGodkjennTaushetserklæring_når_innlogget_er_ikke_Mentor(){
        Avtale enMentorAvtale = TestData.enMentorAvtaleUsignert();
        NavIdent navIdent = new NavIdent("Z123456");
        Veileder veileder = new Veileder(
                navIdent,
                tilgangskontrollService,
                persondataService,
                norg2Client,
                Collections.emptySet(),
                new SlettemerkeProperties(),
                false,
                veilarbArenaClient
        );
        værInnloggetSom(veileder);

        assertThatThrownBy(() -> {
            avtaleController.mentorGodkjennTaushetserklæring(enMentorAvtale.getId(), Instant.now(),Avtalerolle.DELTAKER);
        }).isExactlyInstanceOf(TiltaksgjennomforingException.class);
    }

    @Test
    public void mentorGodkjennTaushetserklæring_når_innlogget_er__Mentor(){
        Avtale enMentorAvtale = TestData.enMentorAvtaleUsignert();
        Mentor mentor = new Mentor(enMentorAvtale.getMentorFnr());
        værInnloggetSom(mentor);

        when(avtaleRepository.findById(enMentorAvtale.getId())).thenReturn(Optional.of(enMentorAvtale));

        avtaleController.mentorGodkjennTaushetserklæring(enMentorAvtale.getId(), Instant.now(),Avtalerolle.DELTAKER);
    }

    @Test
    public void hentSkalKastTilgangskontrollExceptionHvisInnloggetSelvbetjeningBrukerIkkeHarTilgang() {
        Avtale avtale = TestData.enArbeidstreningAvtale();
        værInnloggetSom(
                new Arbeidsgiver(
                        new Fnr("55555566666"),
                        Set.of(),
                        Map.of(),
                        null,
                        null
                )
        );
        when(avtaleRepository.findById(avtale.getId())).thenReturn(Optional.of(avtale));
        assertThatThrownBy(
                () -> avtaleController.hent(avtale.getId(), Avtalerolle.ARBEIDSGIVER)
        ).isExactlyInstanceOf(TilgangskontrollException.class);
    }

    @Test
    public void opprettAvtaleSkalReturnereCreatedOgOpprettetLokasjon() {
        Avtale avtale = TestData.enArbeidstreningAvtale();
        Fnr fnr = avtale.getDeltakerFnr();

        final NavIdent navIdent = new NavIdent("Z123456");
        final NavEnhet navEnhet = TestData.ENHET_OPPFØLGING;
        final PdlRespons pdlRespons = TestData.enPdlrespons(false);
        final OpprettAvtale opprettAvtale = new OpprettAvtale(
                avtale.getDeltakerFnr(),
                avtale.getBedriftNr(),
                Tiltakstype.ARBEIDSTRENING
        );
        var veileder = new Veileder(
                navIdent,
                tilgangskontrollService,
                persondataService,
                norg2Client,
                Set.of(navEnhet),
                new SlettemerkeProperties(),
                false,
                veilarbArenaClient
        );
        værInnloggetSom(veileder);
        when(avtaleRepository.save(any(Avtale.class))).thenReturn(avtale);
        when(
                eregService.hentVirksomhet(avtale.getBedriftNr())).thenReturn(
                        new Organisasjon(
                                avtale.getBedriftNr(),
                                avtale.getGjeldendeInnhold().getBedriftNavn()
                        )
        );
        when(tilgangskontrollService.harSkrivetilgangTilKandidat(eq(veileder), any())).thenReturn(true);
        when(persondataService.hentPersondata(fnr)).thenReturn(pdlRespons);
        when(norg2Client.hentGeografiskEnhet(pdlRespons.getData().getHentGeografiskTilknytning().getGtBydel()))
                .thenReturn(
                        new Norg2GeoResponse(
                                TestData.ENHET_GEOGRAFISK.getNavn(),
                                TestData.ENHET_GEOGRAFISK.getVerdi()
                        )
                );
        when(veilarbArenaClient.sjekkOgHentOppfølgingStatus(any()))
                .thenReturn(
                        new Oppfølgingsstatus(
                                Formidlingsgruppe.ARBEIDSSOKER,
                                Kvalifiseringsgruppe.SITUASJONSBESTEMT_INNSATS,
                                "0906"
                        )
                );

        ResponseEntity svar = avtaleController.opprettAvtaleSomVeileder(opprettAvtale, null);
        assertThat(svar.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(svar.getHeaders().getLocation().getPath()).isEqualTo("/avtaler/" + avtale.getId());
    }

    @Test
    public void endreAvtaleSkalReturnereNotFoundHvisDenIkkeFins() {
        Avtale avtale = TestData.enArbeidstreningAvtale();
        værInnloggetSom(TestData.enVeileder(avtale));
        when(avtaleRepository.findById(avtale.getId())).thenReturn(Optional.empty());
        assertThatThrownBy(
                () -> avtaleController.endreAvtale(
                        avtale.getId(),
                        avtale.getSistEndret(),
                        TestData.ingenEndring(),
                        Avtalerolle.VEILEDER
                )
        ).isExactlyInstanceOf(RessursFinnesIkkeException.class);
    }

    @Test
    public void endreAvtaleSkalReturnereOkHvisInnloggetPersonErVeileder() {
        Avtale avtale = TestData.enArbeidstreningAvtale();
        Veileder veileder = new Veileder(
                enNavIdent(),
                tilgangskontrollService,
                persondataService,
                norg2Client,
                Collections.emptySet(),
                new SlettemerkeProperties(),
                false,
                veilarbArenaClient
        );
        værInnloggetSom(veileder);
        when(tilgangskontrollService.harSkrivetilgangTilKandidat(
                any(Veileder.class),
                any(Fnr.class)
        )).thenReturn(true);
        when(avtaleRepository.findById(avtale.getId())).thenReturn(Optional.of(avtale));
        when(avtaleRepository.save(avtale)).thenReturn(avtale);
        ResponseEntity svar = avtaleController.endreAvtale(
                avtale.getId(),
                avtale.getSistEndret(),
                TestData.ingenEndring(),
                Avtalerolle.VEILEDER
        );
        assertThat(svar.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void endreAvtaleSkalReturnereForbiddenHvisInnloggetPersonIkkeHarTilgang() {
        Avtale avtale = TestData.enArbeidstreningAvtale();
        værInnloggetSom(TestData.enArbeidsgiver());
        when(avtaleRepository.findById(avtale.getId())).thenReturn(Optional.of(avtale));
        assertThatThrownBy(
                () -> avtaleController.endreAvtale(
                        avtale.getId(),
                        avtale.getSistEndret(),
                        TestData.ingenEndring(),
                        Avtalerolle.ARBEIDSGIVER
                )
        ).isInstanceOf(TilgangskontrollException.class);
    }

    @Test
    public void hentAlleAvtalerInnloggetBrukerHarTilgangTilSkalIkkeReturnereAvtalerManIkkeHarTilgangTil() {
        Avtale avtaleMedTilgang = TestData.enArbeidstreningAvtale();
        Avtale avtaleUtenTilgang = Avtale.veilederOppretterAvtale(
                new OpprettAvtale(new Fnr("01039513753"), new BedriftNr("111222333"), Tiltakstype.ARBEIDSTRENING),
                new NavIdent("X643564")
        );
        Deltaker deltaker = TestData.enDeltaker(avtaleMedTilgang);
        værInnloggetSom(deltaker);
        List<Avtale> avtalerBrukerHarTilgangTil = lagListeMedAvtaler(avtaleMedTilgang, 5);
        List<Avtale> alleAvtaler = new ArrayList<>();
        alleAvtaler.addAll(avtalerBrukerHarTilgangTil);
        alleAvtaler.addAll(lagListeMedAvtaler(avtaleUtenTilgang, 4));
        when(avtaleRepository.findAllByDeltakerFnr(eq(deltaker.getIdentifikator()), eq(pageable))).thenReturn(new PageImpl<Avtale>(alleAvtaler));

        Map<String, Object> avtalerPageResponse = deltaker.hentAlleAvtalerMedLesetilgang(
                avtaleRepository,
                new AvtalePredicate(),
                Avtale.Fields.sistEndret,
                pageable
        );

        List<AvtaleMinimalListevisning> avtaler = (List<AvtaleMinimalListevisning>)avtalerPageResponse.get("avtaler");
        assertThat(avtaler)
                .hasSize(avtalerBrukerHarTilgangTil.size());
    }

    @Test
    public void opprettAvtaleSomVeileder__skal_feile_hvis_veileder_ikke_har_tilgang_til_bruker() {
        PersondataService persondataServiceIMetode = mock(PersondataService.class);
        Veileder enNavAnsatt = new Veileder(
                new NavIdent("T000000"),
                tilgangskontrollService,
                persondataServiceIMetode,
                norg2Client,
                Collections.emptySet(),
                new SlettemerkeProperties(),
                false,
                veilarbArenaClient
        );
        værInnloggetSom(enNavAnsatt);
        Fnr deltakerFnr = new Fnr("11111100000");
        when(
                tilgangskontrollService.harSkrivetilgangTilKandidat(enNavAnsatt, deltakerFnr)
        ).thenReturn(false);
        assertThatThrownBy(
                () -> avtaleController.opprettAvtaleSomVeileder(
                        new OpprettAvtale(deltakerFnr, new BedriftNr("111222333"), Tiltakstype.ARBEIDSTRENING),
                        null
                )
        ).isInstanceOf(IkkeTilgangTilDeltakerException.class);
    }

    @Test
    public void opprettAvtaleSomVeileder__skal_feile_hvis_kode6() {
        PersondataService persondataServiceIMetode = mock(PersondataService.class);
        Veileder enNavAnsatt = new Veileder(
                new NavIdent("T000000"),
                tilgangskontrollService,
                persondataServiceIMetode,
                norg2Client,
                Collections.emptySet(),
                new SlettemerkeProperties(),
                false,
                veilarbArenaClient
        );
        værInnloggetSom(enNavAnsatt);
        Fnr deltakerFnr = new Fnr("11111100000");
        when(
                tilgangskontrollService.harSkrivetilgangTilKandidat(enNavAnsatt, deltakerFnr)
        ).thenReturn(true);
        PdlRespons pdlRespons = TestData.enPdlrespons(true);
        when(persondataServiceIMetode.hentPersondata(deltakerFnr)).thenReturn(pdlRespons);
        when(persondataServiceIMetode.erKode6(pdlRespons)).thenCallRealMethod();
        assertThatThrownBy(
                () -> avtaleController.opprettAvtaleSomVeileder(
                        new OpprettAvtale(deltakerFnr, new BedriftNr("111222333"), Tiltakstype.ARBEIDSTRENING),
                        null
                )
        ).isInstanceOf(KanIkkeOppretteAvtalePåKode6Exception.class);
    }

    @Test
    public void opprettAvtaleSomArbeidsgiver__skal_feile_hvis_ag_ikke_har_tilgang_til_bedrift() {
        Arbeidsgiver arbeidsgiver = new Arbeidsgiver(
                TestData.etFodselsnummer(),
                Set.of(),
                Map.of(),
                null,
                null
        );
        værInnloggetSom(arbeidsgiver);
        assertThatThrownBy(
                () -> avtaleController.opprettAvtaleSomArbeidsgiver(
                        new OpprettAvtale(new Fnr("99887765432"), new BedriftNr("111222333"),
                                Tiltakstype.ARBEIDSTRENING)
                )
        ).isInstanceOf(TilgangskontrollException.class);
    }

    private void værInnloggetSom(Avtalepart avtalepart) {
        lenient().when(innloggingService.hentAvtalepart(any())).thenReturn(avtalepart);
        if (avtalepart instanceof Veileder) {
            lenient().when(innloggingService.hentVeileder()).thenReturn((Veileder) avtalepart);
        }
        if (avtalepart instanceof Arbeidsgiver) {
            lenient().when(innloggingService.hentArbeidsgiver()).thenReturn((Arbeidsgiver) avtalepart);
        }
    }

    @Test
    public void viser_ikke_avbruttGrunn_til_arbeidsgiver() {
        Avtale avtale = enArbeidstreningAvtale();
        avtale.setAvbruttGrunn("Hemmelig");
        var arbeidsgiver = TestData.enArbeidsgiver(avtale);
        værInnloggetSom(arbeidsgiver);
        when(avtaleRepository.findById(avtale.getId())).thenReturn(Optional.of(avtale));
        Avtale hentetAvtale = avtaleController.hent(avtale.getId(), Avtalerolle.VEILEDER);
        assertThat(hentetAvtale.getAvbruttGrunn()).isNull();
    }

    @Test
    public void viser_ikke_navenheter_til_arbeidsgiver() {
        Avtale avtale = enArbeidstreningAvtale();
        var arbeidsgiver = TestData.enArbeidsgiver(avtale);
        værInnloggetSom(arbeidsgiver);
        when(avtaleRepository.findById(avtale.getId())).thenReturn(Optional.of(avtale));
        Avtale hentetAvtale = avtaleController.hent(avtale.getId(), Avtalerolle.VEILEDER);
        assertThat(hentetAvtale.getEnhetGeografisk()).isNull();
        assertThat(hentetAvtale.getEnhetOppfolging()).isNull();
    }


    @Test
    public void hentBedriftKontonummer_skal_returnere_nytt_bedriftKontonummer() {
        NavIdent veilederNavIdent = new NavIdent("Z222222");
        Avtale avtale = Avtale.veilederOppretterAvtale(lagOpprettAvtale(), veilederNavIdent);
        NavIdent identTilInnloggetVeileder = new NavIdent("Z333333");
        Veileder veileder = new Veileder(
                identTilInnloggetVeileder,
                tilgangskontrollService,
                persondataService,
                norg2Client,
                Collections.emptySet(),
                new SlettemerkeProperties(),
                false,
                veilarbArenaClient
        );
        værInnloggetSom(veileder);
        when(kontoregisterService.hentKontonummer(anyString())).thenReturn("990983666");
        when(
                tilgangskontrollService.harSkrivetilgangTilKandidat(
                        eq(veileder),
                        any(Fnr.class)
                )
        ).thenReturn(true);
        when(avtaleRepository.findById(avtale.getId())).thenReturn(Optional.of(avtale));
        String kontonummer = avtaleController.hentBedriftKontonummer(avtale.getId(), Avtalerolle.VEILEDER);
        assertThat(kontonummer).isEqualTo("990983666");
    }

    @Test
    public void hentBedriftKontonummer_skal_kaste_en_feil_når_innlogget_part_ikke_har_tilgang_til_Avtale() throws TilgangskontrollException {
        NavIdent veilederNavIdent = new NavIdent("Z222222");
        Avtale avtale = Avtale.veilederOppretterAvtale(lagOpprettAvtale(), veilederNavIdent);
        NavIdent identTilInnloggetVeileder = new NavIdent("Z333333");
        Veileder veileder = new Veileder(
                identTilInnloggetVeileder,
                tilgangskontrollService,
                persondataService,
                norg2Client,
                Collections.emptySet(),
                new SlettemerkeProperties(),
                false,
                veilarbArenaClient
        );
        værInnloggetSom(veileder);
        when(tilgangskontrollService.harSkrivetilgangTilKandidat(
                        eq(veileder),
                        any(Fnr.class)
                )).thenReturn(false);
        when(avtaleRepository.findById(avtale.getId())).thenReturn(Optional.of(avtale));
        assertThatThrownBy(
                () -> avtaleController.hentBedriftKontonummer(avtale.getId(), Avtalerolle.VEILEDER)
        ).isInstanceOf(TilgangskontrollException.class);
    }

    @Test
    public void hentBedriftKontonummer_skal_kaste_en_feil_når_kontonummer_rest_service_svarer_med_feil_response_status_og_kaster_en_exception() {
        NavIdent veilederNavIdent = new NavIdent("Z222222");
        Avtale avtale = Avtale.veilederOppretterAvtale(lagOpprettAvtale(), veilederNavIdent);
        NavIdent identTilInnloggetVeileder = new NavIdent("Z333333");
        Veileder veileder = new Veileder(
                identTilInnloggetVeileder,
                tilgangskontrollService,
                persondataService,
                norg2Client,
                Collections.emptySet(),
                new SlettemerkeProperties(),
                false,
                veilarbArenaClient
        );
        værInnloggetSom(veileder);
        when(kontoregisterService.hentKontonummer(anyString())).thenThrow(KontoregisterFeilException.class);
        when(tilgangskontrollService.harSkrivetilgangTilKandidat(
                eq(veileder),
                any(Fnr.class)
        )).thenReturn(true);
        when(avtaleRepository.findById(avtale.getId())).thenReturn(Optional.of(avtale));
        assertThatThrownBy(
                () -> avtaleController.hentBedriftKontonummer(avtale.getId(), Avtalerolle.VEILEDER)
        ).isInstanceOf(KontoregisterFeilException.class);
    }
}
