package no.nav.familie.ba.sak.ekstern.tilbakekreving

import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.tilbakekreving.TilbakekrevingService
import no.nav.familie.ba.sak.kjerne.tilbakekreving.hentTilbakekrevingInstitusjon
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.statistikk.producer.KafkaProducer
import no.nav.familie.kontrakter.felles.tilbakekreving.Faktainfo
import no.nav.familie.kontrakter.felles.tilbakekreving.HentFagsystemsbehandling
import no.nav.familie.kontrakter.felles.tilbakekreving.HentFagsystemsbehandlingRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.HentFagsystemsbehandlingRespons
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class FagsystemsbehandlingService(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val persongrunnlagService: PersongrunnlagService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val vedtakService: VedtakService,
    private val tilbakekrevingService: TilbakekrevingService,
    private val kafkaProducer: KafkaProducer,
) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    fun hentFagsystemsbehandling(request: HentFagsystemsbehandlingRequest): HentFagsystemsbehandlingRespons {
        logger.info("Henter behandling for behandlingId=${request.eksternId}")
        val behandling = behandlingHentOgPersisterService.hent(request.eksternId.toLong())

        return lagRespons(request, behandling)
    }

    fun sendFagsystemsbehandling(
        respons: HentFagsystemsbehandlingRespons,
        key: String,
        behandlingId: String,
    ) {
        kafkaProducer.sendFagsystemsbehandlingResponsForTopicTilbakekreving(respons, key, behandlingId)
    }

    private fun lagRespons(
        request: HentFagsystemsbehandlingRequest,
        behandling: Behandling,
    ): HentFagsystemsbehandlingRespons {
        val behandlingId = behandling.id
        val persongrunnlag = persongrunnlagService.hentAktivThrows(behandlingId = behandlingId)
        val arbeidsfordeling = arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId)
        val vedtaksdato = vedtakService.hentVedtaksdatoForBehandlingThrows(behandlingId)

        val faktainfo = Faktainfo(
            revurderingsårsak = behandling.opprettetÅrsak.visningsnavn,
            revurderingsresultat = behandling.resultat.displayName,
            tilbakekrevingsvalg = tilbakekrevingService.hentTilbakekrevingsvalg(behandlingId),
        )

        val hentFagsystemsbehandling = HentFagsystemsbehandling(
            eksternFagsakId = request.eksternFagsakId,
            eksternId = request.eksternId,
            ytelsestype = request.ytelsestype,
            regelverk = behandling.kategori.tilRegelverk(),
            personIdent = behandling.fagsak.aktør.aktivFødselsnummer(),
            språkkode = persongrunnlag.søker.målform.tilSpråkkode(),
            enhetId = arbeidsfordeling.behandlendeEnhetId,
            enhetsnavn = arbeidsfordeling.behandlendeEnhetNavn,
            revurderingsvedtaksdato = vedtaksdato.toLocalDate(),
            faktainfo = faktainfo,
            institusjon = hentTilbakekrevingInstitusjon(behandling.fagsak),
        )

        return HentFagsystemsbehandlingRespons(hentFagsystemsbehandling = hentFagsystemsbehandling)
    }
}
