package no.nav.familie.ba.sak.kjerne.verdikjedetester

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.lagSøknadDTO
import no.nav.familie.ba.sak.ekstern.restDomene.RestPersonResultat
import no.nav.familie.ba.sak.ekstern.restDomene.RestPutVedtaksperiodeMedStandardbegrunnelser
import no.nav.familie.ba.sak.ekstern.restDomene.RestRegistrerSøknad
import no.nav.familie.ba.sak.ekstern.restDomene.RestTilbakekreving
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.autovedtak.omregning.Autobrev6og18ÅrService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.brev.BrevmalService
import no.nav.familie.ba.sak.kjerne.fagsak.Beslutning
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.RestBeslutningPåVedtak
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenario
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenarioPerson
import no.nav.familie.ba.sak.task.dto.Autobrev6og18ÅrDTO
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import java.time.LocalDate
import java.time.YearMonth

class TriggingAvAutobrev6og18ÅrTest(
    @Autowired private val fagsakService: FagsakService,
    @Autowired private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    @Autowired private val vedtakService: VedtakService,
    @Autowired private val stegService: StegService,
    @Autowired private val autobrev6og18ÅrService: Autobrev6og18ÅrService,
    @Autowired private val brevmalService: BrevmalService,
    @Autowired private val vedtaksperiodeService: VedtaksperiodeService,
) : AbstractVerdikjedetest() {

    @Test
    fun `Omregning og autobrev skal kjøres for 18 år og ikke 6 år`() {
        kjørFørstegangsbehandlingOgTriggAutobrev(6)
    }

    @Test
    fun `Omregning og autobrev skal kjøres for 6 år og ikke 18 år`() {
        kjørFørstegangsbehandlingOgTriggAutobrev(18)
    }

    fun kjørFørstegangsbehandlingOgTriggAutobrev(årMedReduksjonsbegrunnelse: Int) {
        val reduksjonsbegrunnelse = if (årMedReduksjonsbegrunnelse == 6) {
            Standardbegrunnelse.REDUKSJON_UNDER_6_ÅR
        } else {
            Standardbegrunnelse.REDUKSJON_UNDER_18_ÅR
        }

        val scenario = mockServerKlient().lagScenario(
            RestScenario(
                søker = RestScenarioPerson(fødselsdato = "1996-11-12", fornavn = "Mor", etternavn = "Søker"),
                barna = listOf(
                    RestScenarioPerson(
                        fødselsdato = LocalDate.now().minusYears(2).toString(),
                        fornavn = "Toåringen",
                        etternavn = "Barnesen",
                        bostedsadresser = emptyList(),
                    ),
                    RestScenarioPerson(
                        fødselsdato = LocalDate.now().minusYears(6).toString(),
                        fornavn = "Seksåringen",
                        etternavn = "Barnesen",
                        bostedsadresser = emptyList(),
                    ),
                    RestScenarioPerson(
                        fødselsdato = LocalDate.now().minusYears(18).toString(),
                        fornavn = "Attenåringen",
                        etternavn = "Barnesen",
                        bostedsadresser = emptyList(),
                    ),
                ),
            ),
        )

        val fagsakId = familieBaSakKlient().opprettFagsak(søkersIdent = scenario.søker.ident!!).data?.id!!
        familieBaSakKlient().opprettBehandling(søkersIdent = scenario.søker.ident, fagsakId = fagsakId)

        val restFagsakEtterOpprettelse = familieBaSakKlient().hentFagsak(fagsakId = fagsakId)

        val aktivBehandling = hentAktivBehandling(restFagsak = restFagsakEtterOpprettelse.data!!)
        val restRegistrerSøknad =
            RestRegistrerSøknad(
                søknad = lagSøknadDTO(
                    søkerIdent = scenario.søker.ident,
                    barnasIdenter = scenario.barna.map { it.ident!! },
                ),
                bekreftEndringerViaFrontend = false,
            )
        val restUtvidetBehandling: Ressurs<RestUtvidetBehandling> =
            familieBaSakKlient().registrererSøknad(
                behandlingId = aktivBehandling.behandlingId,
                restRegistrerSøknad = restRegistrerSøknad,
            )

        // Godkjenner alle vilkår på førstegangsbehandling.
        restUtvidetBehandling.data!!.personResultater.forEach { restPersonResultat ->
            restPersonResultat.vilkårResultater.filter { it.resultat == Resultat.IKKE_VURDERT }.forEach {
                familieBaSakKlient().putVilkår(
                    behandlingId = restUtvidetBehandling.data!!.behandlingId,
                    vilkårId = it.id,
                    restPersonResultat = RestPersonResultat(
                        personIdent = restPersonResultat.personIdent,
                        vilkårResultater = listOf(
                            it.copy(
                                resultat = Resultat.OPPFYLT,
                                periodeFom = LocalDate.now().minusMonths(2),
                            ),
                        ),
                    ),
                )
            }
        }

        familieBaSakKlient().validerVilkårsvurdering(
            behandlingId = restUtvidetBehandling.data!!.behandlingId,
        )

        val restUtvidetBehandlingEtterBehandlingsresultat =
            familieBaSakKlient().behandlingsresultatStegOgGåVidereTilNesteSteg(
                behandlingId = restUtvidetBehandling.data!!.behandlingId,
            )

        val restUtvidetBehandlingEtterVurderTilbakekreving =
            familieBaSakKlient().lagreTilbakekrevingOgGåVidereTilNesteSteg(
                restUtvidetBehandlingEtterBehandlingsresultat.data!!.behandlingId,
                RestTilbakekreving(Tilbakekrevingsvalg.IGNORER_TILBAKEKREVING, begrunnelse = "begrunnelse"),
            )

        val vedtaksperioderMedBegrunnelser = vedtaksperiodeService.hentRestUtvidetVedtaksperiodeMedBegrunnelser(
            restUtvidetBehandlingEtterVurderTilbakekreving.data!!.behandlingId,
        )

        val førsteVedtaksperiode = vedtaksperioderMedBegrunnelser.sortedBy { it.fom }.first()
        familieBaSakKlient().oppdaterVedtaksperiodeMedStandardbegrunnelser(
            vedtaksperiodeId = førsteVedtaksperiode.id,
            restPutVedtaksperiodeMedStandardbegrunnelser = RestPutVedtaksperiodeMedStandardbegrunnelser(
                standardbegrunnelser = listOf(
                    Standardbegrunnelse.INNVILGET_BOR_HOS_SØKER.enumnavnTilString(),
                ),
            ),
        )
        val reduksjonVedtaksperiodeId =
            vedtaksperioderMedBegrunnelser.single {
                it.fom!!.isEqual(
                    LocalDate.now().førsteDagIInneværendeMåned(),
                ) && it.type == Vedtaksperiodetype.UTBETALING
            }
        familieBaSakKlient().oppdaterVedtaksperiodeMedStandardbegrunnelser(
            vedtaksperiodeId = reduksjonVedtaksperiodeId.id,
            restPutVedtaksperiodeMedStandardbegrunnelser = RestPutVedtaksperiodeMedStandardbegrunnelser(
                standardbegrunnelser = listOf(reduksjonsbegrunnelse.enumnavnTilString()),
            ),
        )

        val restUtvidetBehandlingEtterSendTilBeslutter =
            familieBaSakKlient().sendTilBeslutter(behandlingId = restUtvidetBehandlingEtterVurderTilbakekreving.data!!.behandlingId)

        familieBaSakKlient().iverksettVedtak(
            behandlingId = restUtvidetBehandlingEtterSendTilBeslutter.data!!.behandlingId,
            restBeslutningPåVedtak = RestBeslutningPåVedtak(
                Beslutning.GODKJENT,
            ),
            beslutterHeaders = HttpHeaders().apply {
                setBearerAuth(
                    token(
                        mapOf(
                            "groups" to listOf("SAKSBEHANDLER", "BESLUTTER"),
                            "azp" to "azp-test",
                            "name" to "Mock McMockface Beslutter",
                            "NAVident" to "Z0000",
                        ),
                    ),
                )
            },
        )

        håndterIverksettingAvBehandling(
            behandlingEtterVurdering = behandlingHentOgPersisterService.finnAktivForFagsak(fagsakId = fagsakId)!!,
            søkerFnr = scenario.søker.ident,
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            stegService = stegService,
            brevmalService = brevmalService,

        )

        autobrev6og18ÅrService.opprettOmregningsoppgaveForBarnIBrytingsalder(
            autobrev6og18ÅrDTO = Autobrev6og18ÅrDTO(
                fagsakId = fagsakId,
                alder = årMedReduksjonsbegrunnelse,
                årMåned = YearMonth.now(),
            ),
        )

        var behandlinger = behandlingHentOgPersisterService.hentBehandlinger(fagsakId)
        // Her forventer vi ikke at autobrev skal trigges pga vedtaksbegrunnelsen som er satt på FGB.
        assertEquals(1, behandlinger.size)

        autobrev6og18ÅrService.opprettOmregningsoppgaveForBarnIBrytingsalder(
            autobrev6og18ÅrDTO = Autobrev6og18ÅrDTO(
                fagsakId = fagsakId,
                alder = if (årMedReduksjonsbegrunnelse == 6) 18 else 6,
                årMåned = YearMonth.now(),
            ),
        )

        behandlinger = behandlingHentOgPersisterService.hentBehandlinger(fagsakId)
        // Her forventer vi at autobrev skal trigges pga manglende vedtaksbegrunnelse for denne alderen på FGB.
        assertEquals(2, behandlinger.size)

        // Skal nye autobrev trigges må aktiv behandling være avsluttet. Gjør dette eksplisitt (og utenfor normal
        // flyt) ettersom dette skjer via en task når autobrev-koden kjøres.
        val revurderingMedAutobrev = behandlingHentOgPersisterService.finnAktivForFagsak(fagsakId)!!
        revurderingMedAutobrev.status = BehandlingStatus.AVSLUTTET
        behandlingHentOgPersisterService.lagreEllerOppdater(revurderingMedAutobrev)

        autobrev6og18ÅrService.opprettOmregningsoppgaveForBarnIBrytingsalder(
            autobrev6og18ÅrDTO = Autobrev6og18ÅrDTO(
                fagsakId = fagsakId,
                alder = if (årMedReduksjonsbegrunnelse == 6) 18 else 6,
                årMåned = YearMonth.now(),
            ),
        )

        behandlinger = behandlingHentOgPersisterService.hentBehandlinger(fagsakId)
        // Her forventer vi ikke at autobrev skal trigges fordi det har blitt kjørt.
        assertEquals(2, behandlinger.size)
    }
}
