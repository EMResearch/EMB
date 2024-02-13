package no.nav.familie.ba.sak.kjerne.verdikjedetester

import no.nav.familie.ba.sak.ekstern.restDomene.NavnOgIdent
import no.nav.familie.ba.sak.ekstern.restDomene.RestJournalføring
import no.nav.familie.ba.sak.ekstern.restDomene.RestJournalpostDokument
import no.nav.familie.ba.sak.ekstern.restDomene.RestMinimalFagsak
import no.nav.familie.ba.sak.ekstern.restDomene.RestPersonResultat
import no.nav.familie.ba.sak.ekstern.restDomene.RestPutVedtaksperiodeMedStandardbegrunnelser
import no.nav.familie.ba.sak.ekstern.restDomene.RestTilbakekreving
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.brev.BrevmalService
import no.nav.familie.ba.sak.kjerne.fagsak.Beslutning
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.fagsak.RestBeslutningPåVedtak
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenario
import no.nav.familie.kontrakter.ba.infotrygd.Barn
import no.nav.familie.kontrakter.ba.infotrygd.Delytelse
import no.nav.familie.kontrakter.ba.infotrygd.Sak
import no.nav.familie.kontrakter.ba.infotrygd.Stønad
import no.nav.familie.kontrakter.felles.journalpost.LogiskVedlegg
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import org.springframework.http.HttpHeaders
import java.time.LocalDate
import java.time.LocalDateTime

fun lagMockRestJournalføring(bruker: NavnOgIdent): RestJournalføring = RestJournalføring(
    avsender = bruker,
    bruker = bruker,
    datoMottatt = LocalDateTime.now().minusDays(10),
    journalpostTittel = "Søknad om ordinær barnetrygd",
    kategori = BehandlingKategori.NASJONAL,
    underkategori = BehandlingUnderkategori.ORDINÆR,
    knyttTilFagsak = true,
    opprettOgKnyttTilNyBehandling = true,
    tilknyttedeBehandlingIder = emptyList(),
    dokumenter = listOf(
        RestJournalpostDokument(
            dokumentTittel = "Søknad om barnetrygd",
            brevkode = "mock",
            dokumentInfoId = "1",
            logiskeVedlegg = listOf(LogiskVedlegg("123", "Oppholdstillatelse")),
            eksisterendeLogiskeVedlegg = emptyList(),
        ),
        RestJournalpostDokument(
            dokumentTittel = "Ekstra vedlegg",
            brevkode = "mock",
            dokumentInfoId = "2",
            logiskeVedlegg = listOf(LogiskVedlegg("123", "Pass")),
            eksisterendeLogiskeVedlegg = emptyList(),
        ),
    ),
    navIdent = "09123",
    nyBehandlingstype = BehandlingType.FØRSTEGANGSBEHANDLING,
    nyBehandlingsårsak = BehandlingÅrsak.SØKNAD,
    fagsakType = FagsakType.NORMAL,
)

fun lagInfotrygdSak(beløp: Double, identBarn: List<String>, valg: String? = "OR", undervalg: String? = "OS"): Sak {
    return Sak(
        stønad = Stønad(
            barn = identBarn.map { Barn(it, barnetrygdTom = "000000") },
            delytelse = listOf(
                Delytelse(
                    fom = LocalDate.now(),
                    tom = null,
                    beløp = beløp,
                    typeDelytelse = "MS",
                    typeUtbetaling = "J",
                ),
            ),
            opphørsgrunn = "0",
            antallBarn = identBarn.size,
            mottakerNummer = 80000123456,
            status = "04",
            virkningFom = "797790",
        ),
        status = "FB",
        valg = valg,
        undervalg = undervalg,
    )
}

fun fullførBehandlingFraVilkårsvurderingAlleVilkårOppfylt(
    restUtvidetBehandling: RestUtvidetBehandling,
    personScenario: RestScenario,
    fagsak: RestMinimalFagsak,
    familieBaSakKlient: FamilieBaSakKlient,
    lagToken: (Map<String, Any>) -> String,
    behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    fagsakService: FagsakService,
    vedtakService: VedtakService,
    stegService: StegService,
    brevmalService: BrevmalService,
    vedtaksperiodeService: VedtaksperiodeService,
): Behandling {
    settAlleVilkårTilOppfylt(
        restUtvidetBehandling = restUtvidetBehandling,
        barnFødselsdato = personScenario.barna.maxOf { LocalDate.parse(it.fødselsdato) },
        familieBaSakKlient = familieBaSakKlient,
    )

    familieBaSakKlient.validerVilkårsvurdering(
        behandlingId = restUtvidetBehandling.behandlingId,
    )

    val restUtvidetBehandlingEtterBehandlingsResultat =
        familieBaSakKlient.behandlingsresultatStegOgGåVidereTilNesteSteg(
            behandlingId = restUtvidetBehandling.behandlingId,
        )

    val restUtvidetBehandlingEtterVurderTilbakekreving =
        familieBaSakKlient.lagreTilbakekrevingOgGåVidereTilNesteSteg(
            restUtvidetBehandlingEtterBehandlingsResultat.data!!.behandlingId,
            RestTilbakekreving(Tilbakekrevingsvalg.IGNORER_TILBAKEKREVING, begrunnelse = "begrunnelse"),
        )

    val vedtaksperioderMedBegrunnelser = vedtaksperiodeService.hentRestUtvidetVedtaksperiodeMedBegrunnelser(
        restUtvidetBehandlingEtterVurderTilbakekreving.data!!.behandlingId,
    )

    val utvidetVedtaksperiodeMedBegrunnelser =
        vedtaksperioderMedBegrunnelser.sortedBy { it.fom }.first()

    familieBaSakKlient.oppdaterVedtaksperiodeMedStandardbegrunnelser(
        vedtaksperiodeId = utvidetVedtaksperiodeMedBegrunnelser.id,
        restPutVedtaksperiodeMedStandardbegrunnelser = RestPutVedtaksperiodeMedStandardbegrunnelser(
            standardbegrunnelser = utvidetVedtaksperiodeMedBegrunnelser.gyldigeBegrunnelser.filter(String::isNotEmpty),
        ),
    )
    val restUtvidetBehandlingEtterSendTilBeslutter =
        familieBaSakKlient.sendTilBeslutter(behandlingId = restUtvidetBehandlingEtterVurderTilbakekreving.data!!.behandlingId)

    familieBaSakKlient.iverksettVedtak(
        behandlingId = restUtvidetBehandlingEtterSendTilBeslutter.data!!.behandlingId,
        restBeslutningPåVedtak = RestBeslutningPåVedtak(
            Beslutning.GODKJENT,
        ),
        beslutterHeaders = HttpHeaders().apply {
            setBearerAuth(
                lagToken(
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
    return håndterIverksettingAvBehandling(
        behandlingEtterVurdering = behandlingHentOgPersisterService.finnAktivForFagsak(fagsakId = fagsak.id)!!,
        søkerFnr = personScenario.søker.ident!!,
        fagsakService = fagsakService,
        vedtakService = vedtakService,
        stegService = stegService,
        brevmalService = brevmalService,
    )
}

fun settAlleVilkårTilOppfylt(
    restUtvidetBehandling: RestUtvidetBehandling,
    barnFødselsdato: LocalDate,
    familieBaSakKlient: FamilieBaSakKlient,
) {
    restUtvidetBehandling.personResultater.forEach { restPersonResultat ->
        restPersonResultat.vilkårResultater.filter { it.resultat == Resultat.IKKE_VURDERT }.forEach {
            familieBaSakKlient.putVilkår(
                behandlingId = restUtvidetBehandling.behandlingId,
                vilkårId = it.id,
                restPersonResultat =
                RestPersonResultat(
                    personIdent = restPersonResultat.personIdent,
                    vilkårResultater = listOf(
                        it.copy(
                            resultat = Resultat.OPPFYLT,
                            periodeFom = barnFødselsdato,
                        ),
                    ),
                ),
            )
        }
    }
}
