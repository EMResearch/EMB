package no.nav.familie.tilbake.avstemming

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.tilbake.avstemming.marshaller.ØkonomiKvitteringTolk
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Behandlingstype
import no.nav.familie.tilbake.behandling.domain.Institusjon
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.IntegrasjonerConfig
import no.nav.familie.tilbake.iverksettvedtak.TilbakekrevingsvedtakMarshaller
import no.nav.familie.tilbake.iverksettvedtak.domain.ØkonomiXmlSendt
import no.nav.familie.tilbake.iverksettvedtak.ØkonomiXmlSendtRepository
import no.nav.okonomi.tilbakekrevingservice.TilbakekrevingsvedtakRequest
import no.nav.tilbakekreving.typer.v1.MmelDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class AvstemmingService(
    private val behandlingRepository: BehandlingRepository,
    private val sendtXmlRepository: ØkonomiXmlSendtRepository,
    private val fagsakRepository: FagsakRepository,
    private val integrasjonerConfig: IntegrasjonerConfig,
) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    fun oppsummer(dato: LocalDate): ByteArray? {
        val sendteVedtak = sendtXmlRepository.findByOpprettetPåDato(dato)
        var antallFeilet = 0
        var antallFørstegangsvedtakUtenTilbakekreving = 0
        val rader = sendteVedtak.mapNotNull { sendtVedtak ->
            if (!erSendtOK(sendtVedtak)) {
                antallFeilet++
                return@mapNotNull null
            }
            val behandling = behandlingRepository.findByIdOrThrow(sendtVedtak.behandlingId)
            val oppsummering: TilbakekrevingsvedtakOppsummering = oppsummer(sendtVedtak)
            if (erFørstegangsvedtakUtenTilbakekreving(behandling, oppsummering)) {
                antallFørstegangsvedtakUtenTilbakekreving++
                return@mapNotNull null
            }
            lagAvstemmingsradForVedtaket(behandling, oppsummering)
        }
        if (antallFeilet == 0) {
            logger.info(
                "Avstemmer {}. Sender {} vedtak til avstemming. Totalt ble {} vedtak sendt til OS dette døgnet. " +
                    "{} førstegangsvedtak uten tilbakekreving sendes ikke til avstemming",
                dato,
                rader.size,
                sendteVedtak.size,
                antallFørstegangsvedtakUtenTilbakekreving,
            )
        } else {
            logger.warn(
                "Avstemmer {}. Sender {} vedtak til avstemming. Totalt ble {} vedtak sendt til OS dette døgnet. " +
                    "{} førstegangsvedtak uten tilbakekreving sendes ikke til avstemming. " +
                    "{} vedtak fikk negativ kvittering fra OS og sendes ikke til avstemming",
                dato,
                rader.size,
                sendteVedtak.size,
                antallFørstegangsvedtakUtenTilbakekreving,
                antallFeilet,
            )
        }
        return if (rader.isEmpty()) {
            null
        } else {
            FilMapper(rader).tilFlatfil()
        }
    }

    private fun erFørstegangsvedtakUtenTilbakekreving(
        behandling: Behandling,
        oppsummering: TilbakekrevingsvedtakOppsummering,
    ): Boolean {
        return behandling.type == Behandlingstype.TILBAKEKREVING && oppsummering.harIngenTilbakekreving()
    }

    private fun lagAvstemmingsradForVedtaket(
        behandling: Behandling,
        oppsummering: TilbakekrevingsvedtakOppsummering,
    ): Rad {
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        val vedtaksdato = behandling.sisteResultat?.behandlingsvedtak?.vedtaksdato ?: error("Vedtaksdato mangler")
        return Rad(
            avsender = integrasjonerConfig.applicationName,
            vedtakId = oppsummering.økonomivedtakId,
            fnr = if (fagsak.institusjon != null) padOrganisasjonsnummer(fagsak.institusjon) else fagsak.bruker.ident,
            vedtaksdato = vedtaksdato,
            fagsakYtelseType = fagsak.ytelsestype,
            tilbakekrevesBruttoUtenRenter = oppsummering.tilbakekrevesBruttoUtenRenter,
            tilbakekrevesNettoUtenRenter = oppsummering.tilbakekrevesNettoUtenRenter,
            skatt = oppsummering.skatt,
            renter = oppsummering.renter,
            erOmgjøringTilIngenTilbakekreving = erOmgjøringTilIngenTilbakekreving(oppsummering, behandling),
        )
    }

    private fun padOrganisasjonsnummer(institusjon: Institusjon): String {
        return "00" + institusjon.organisasjonsnummer
    }

    private fun erOmgjøringTilIngenTilbakekreving(
        oppsummering: TilbakekrevingsvedtakOppsummering,
        behandling: Behandling,
    ): Boolean {
        return behandling.type == Behandlingstype.REVURDERING_TILBAKEKREVING && oppsummering.harIngenTilbakekreving()
    }

    private fun oppsummer(sendtMelding: ØkonomiXmlSendt): TilbakekrevingsvedtakOppsummering {
        val xml: String = sendtMelding.melding
        val melding: TilbakekrevingsvedtakRequest =
            TilbakekrevingsvedtakMarshaller.unmarshall(xml, sendtMelding.behandlingId, sendtMelding.id)
        return TilbakekrevingsvedtakOppsummering.oppsummer(melding.tilbakekrevingsvedtak)
    }

    companion object {

        private fun erSendtOK(melding: ØkonomiXmlSendt): Boolean {
            val kvittering: MmelDto = melding.kvittering?.let { objectMapper.readValue(it) } ?: return false
            return ØkonomiKvitteringTolk.erKvitteringOK(kvittering)
        }
    }
}
