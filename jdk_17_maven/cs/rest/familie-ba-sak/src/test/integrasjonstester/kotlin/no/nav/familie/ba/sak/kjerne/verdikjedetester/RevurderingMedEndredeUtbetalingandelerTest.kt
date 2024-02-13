package no.nav.familie.ba.sak.kjerne.verdikjedetester

import no.nav.familie.ba.sak.common.lagSøknadDTO
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.nyOrdinærBehandling
import no.nav.familie.ba.sak.common.nyRevurdering
import no.nav.familie.ba.sak.ekstern.restDomene.RestEndretUtbetalingAndel
import no.nav.familie.ba.sak.ekstern.restDomene.RestPersonResultat
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestPersonResultat
import no.nav.familie.ba.sak.ekstern.restDomene.writeValueAsString
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.tilstand.BehandlingStegTilstand
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelService
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.søknad.SøknadGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.søknad.SøknadGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.steg.grunnlagForNyBehandling.VilkårsvurderingForNyBehandlingService
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenario
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenarioPerson
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class RevurderingMedEndredeUtbetalingandelerTest(
    @Autowired
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,

    @Autowired
    private val fagsakService: FagsakService,

    @Autowired
    private val persongrunnlagService: PersongrunnlagService,

    @Autowired
    private val vilkårService: VilkårService,

    @Autowired
    private val stegService: StegService,

    @Autowired
    private val endretUtbetalingAndelHentOgPersisterService: EndretUtbetalingAndelHentOgPersisterService,

    @Autowired
    private val endretUtbetalingAndelService: EndretUtbetalingAndelService,

    @Autowired
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService,

    @Autowired
    private val personidentService: PersonidentService,

    @Autowired
    private val vilkårsvurderingForNyBehandlingService: VilkårsvurderingForNyBehandlingService,

    @Autowired
    private val søknadGrunnlagRepository: SøknadGrunnlagRepository,

) : AbstractVerdikjedetest() {
    @Test
    fun `Endrede utbetalingsandeler fra forrige behandling kopieres riktig og oppdaterer andel med riktig beløp`() {
        val scenario = mockServerKlient().lagScenario(
            RestScenario(
                søker = RestScenarioPerson(
                    fødselsdato = "${LocalDate.now().minusYears(28)}",
                    fornavn = "Mor",
                    etternavn = "Søker",
                ),
                barna = listOf(
                    RestScenarioPerson(
                        fødselsdato = LocalDate.now().minusYears(4).toString(),
                        fornavn = "Barn",
                        etternavn = "Barnesen",
                    ),
                ),
            ),
        )
        val fnr = scenario.søker.ident!!
        val barnFnr = scenario.barna[0].ident!!
        val barnetsFødselsdato = LocalDate.parse(scenario.barna[0].fødselsdato)

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)

        val endretAndelFom = YearMonth.now().minusYears(3).minusMonths(4)
        val endretAndelTom = YearMonth.now().minusYears(3)

        // Behandling 1 - førstegangsbehandling
        val iverksattFørstegangsbehandling =
            lagFørstegangsbehandlingMedEndretUtbetalingAndel(
                endretAndelFom = endretAndelFom,
                endretAndelTom = endretAndelTom,
                søkersIdent = fnr,
                barnFnr = barnFnr,
                fagsak = fagsak,
                barnetsFødselsdato = barnetsFødselsdato,
            )

        // Behandling 2 - revurdering
        val behandlingRevurdering =
            stegService.håndterNyBehandling(nyRevurdering(søkersIdent = fnr, fagsakId = fagsak.id))

        persongrunnlagService.lagreOgDeaktiverGammel(
            lagTestPersonopplysningGrunnlag(
                behandlingId = behandlingRevurdering.id,
                søkerPersonIdent = fnr,
                barnasIdenter = listOf(barnFnr),
                søkerAktør = personidentService.hentOgLagreAktør(fnr, true),
                barnAktør = personidentService.hentOgLagreAktørIder(listOf(barnFnr), true),
                barnasFødselsdatoer = listOf(barnetsFødselsdato),
            ),
        )

        val vilkårsvurderingRevurdering = vilkårsvurderingForNyBehandlingService.initierVilkårsvurderingForBehandling(
            behandling = behandlingRevurdering,
            bekreftEndringerViaFrontend = true,
            forrigeBehandlingSomErVedtatt = iverksattFørstegangsbehandling,
        )

        gjennomførVilkårsvurdering(
            vilkårsvurdering = vilkårsvurderingRevurdering,
            behandling = behandlingRevurdering,
            barnetsFødselsdato = barnetsFødselsdato,
        )

        val kopierteEndredeUtbetalingAndeler =
            endretUtbetalingAndelHentOgPersisterService.hentForBehandling(behandlingRevurdering.id)
        val andelerTilkjentYtelse =
            andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(
                behandlingRevurdering.id,
            )
        val andelPåvirketAvEndringer = andelerTilkjentYtelse.first()

        assertEquals(1, kopierteEndredeUtbetalingAndeler.size)

        assertEquals(BigDecimal.ZERO, andelPåvirketAvEndringer.prosent)
        assertEquals(endretAndelFom, andelPåvirketAvEndringer.stønadFom)
        assertEquals(endretAndelTom, andelPåvirketAvEndringer.stønadTom)
        assertTrue(andelPåvirketAvEndringer.endreteUtbetalinger.any { it.id == kopierteEndredeUtbetalingAndeler.single().id })
    }

    private fun gjennomførVilkårsvurdering(
        vilkårsvurdering: Vilkårsvurdering,
        behandling: Behandling,
        barnetsFødselsdato: LocalDate,
    ) {
        vilkårsvurdering.personResultater.map { personResultat ->
            personResultat.tilRestPersonResultat().vilkårResultater.map {
                vilkårService.endreVilkår(
                    behandlingId = behandling.id,
                    vilkårId = it.id,
                    restPersonResultat =
                    RestPersonResultat(
                        personIdent = personResultat.aktør.aktivFødselsnummer(),
                        vilkårResultater = listOf(
                            it.copy(
                                resultat = Resultat.OPPFYLT,
                                periodeFom = if (it.vilkårType == Vilkår.UNDER_18_ÅR) {
                                    barnetsFødselsdato
                                } else {
                                    LocalDate.now().minusYears(3).minusMonths(5).withDayOfMonth(8)
                                },
                                utdypendeVilkårsvurderinger = listOfNotNull(
                                    if (it.vilkårType == Vilkår.BOR_MED_SØKER) UtdypendeVilkårsvurdering.DELT_BOSTED else null,
                                ),
                            ),
                        ),
                    ),
                )
            }
        }
        behandling.behandlingStegTilstand.add(
            BehandlingStegTilstand(behandling = behandling, behandlingSteg = StegType.VILKÅRSVURDERING),
        )

        stegService.håndterVilkårsvurdering(behandling)
    }

    private fun lagFørstegangsbehandlingMedEndretUtbetalingAndel(
        endretAndelFom: YearMonth,
        endretAndelTom: YearMonth,
        søkersIdent: String,
        barnFnr: String,
        fagsak: Fagsak,
        barnetsFødselsdato: LocalDate,
    ): Behandling {
        val førstegangsbehandling =
            stegService.håndterNyBehandling(nyOrdinærBehandling(søkersIdent = søkersIdent, fagsakId = fagsak.id))

        val søknadGrunnlag = SøknadGrunnlag(
            behandlingId = førstegangsbehandling.id,
            aktiv = true,
            søknad = lagSøknadDTO(
                søkersIdent,
                barnasIdenter = listOf(barnFnr),
                underkategori = BehandlingUnderkategori.ORDINÆR,
            ).writeValueAsString(),
        )

        søknadGrunnlagRepository.save(søknadGrunnlag)

        persongrunnlagService.lagreOgDeaktiverGammel(
            lagTestPersonopplysningGrunnlag(
                behandlingId = førstegangsbehandling.id,
                søkerPersonIdent = søkersIdent,
                barnasIdenter = listOf(barnFnr),
                søkerAktør = personidentService.hentOgLagreAktør(søkersIdent, true),
                barnAktør = personidentService.hentOgLagreAktørIder(listOf(barnFnr), true),
                barnasFødselsdatoer = listOf(barnetsFødselsdato),
            ),
        )

        val vilkårsvurdering = vilkårsvurderingForNyBehandlingService.initierVilkårsvurderingForBehandling(
            behandling = førstegangsbehandling,
            bekreftEndringerViaFrontend = true,
            forrigeBehandlingSomErVedtatt = null,
        )

        gjennomførVilkårsvurdering(
            vilkårsvurdering = vilkårsvurdering,
            behandling = førstegangsbehandling,
            barnetsFødselsdato = barnetsFødselsdato,
        )

        val endretUtbetalingAndel =
            endretUtbetalingAndelService.opprettTomEndretUtbetalingAndelOgOppdaterTilkjentYtelse(førstegangsbehandling)

        val restEndretUtbetalingAndel = RestEndretUtbetalingAndel(
            id = endretUtbetalingAndel.id,
            fom = endretAndelFom,
            tom = endretAndelTom,
            avtaletidspunktDeltBosted = LocalDate.now().minusYears(3).withDayOfMonth(8),
            søknadstidspunkt = LocalDate.now().minusYears(3).withDayOfMonth(8),
            begrunnelse = "begrunnelse",
            personIdent = barnFnr,
            årsak = Årsak.DELT_BOSTED,
            prosent = BigDecimal.ZERO,
            erTilknyttetAndeler = false,
        )

        endretUtbetalingAndelService.oppdaterEndretUtbetalingAndelOgOppdaterTilkjentYtelse(
            førstegangsbehandling,
            endretUtbetalingAndel.id,
            restEndretUtbetalingAndel,
        )

        førstegangsbehandling.behandlingStegTilstand.add(
            BehandlingStegTilstand(behandling = førstegangsbehandling, behandlingSteg = StegType.BEHANDLINGSRESULTAT),
        )
        val behandlingEtterHåndterBehandlingsresultat = stegService.håndterBehandlingsresultat(førstegangsbehandling)

        behandlingEtterHåndterBehandlingsresultat.behandlingStegTilstand.add(
            BehandlingStegTilstand(
                behandling = førstegangsbehandling,
                behandlingSteg = StegType.BEHANDLING_AVSLUTTET,
            ),
        )
        behandlingEtterHåndterBehandlingsresultat.status = BehandlingStatus.AVSLUTTET

        val iverksattBehandling =
            behandlingHentOgPersisterService.lagreEllerOppdater(behandlingEtterHåndterBehandlingsresultat)

        return iverksattBehandling
    }
}
