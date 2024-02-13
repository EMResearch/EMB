package no.nav.familie.tilbake.kravgrunnlag

import no.nav.familie.kontrakter.felles.historikkinnslag.Aktør
import no.nav.familie.tilbake.historikkinnslag.HistorikkService
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import no.nav.familie.tilbake.integration.økonomi.OppdragClient
import no.nav.familie.tilbake.kravgrunnlag.domain.KodeAksjon
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlag431
import no.nav.okonomi.tilbakekrevingservice.KravgrunnlagHentDetaljRequest
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagDto
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.HentKravgrunnlagDetaljDto
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigInteger
import java.time.LocalDateTime
import java.util.UUID

@Service
class HentKravgrunnlagService(
    private val kravgrunnlagRepository: KravgrunnlagRepository,
    private val oppdragClient: OppdragClient,
    private val historikkService: HistorikkService,
) {

    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    fun hentKravgrunnlagFraØkonomi(kravgrunnlagId: BigInteger, kodeAksjon: KodeAksjon): DetaljertKravgrunnlagDto {
        logger.info("Henter kravgrunnlag for kravgrunnlagId=$kravgrunnlagId for kodeAksjon=$kodeAksjon")
        return oppdragClient.hentKravgrunnlag(kravgrunnlagId, lagRequest(kravgrunnlagId, kodeAksjon))
    }

    fun hentTilbakekrevingskravgrunnlag(behandlingId: UUID): Kravgrunnlag431 {
        return kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
    }

    @Transactional
    fun lagreHentetKravgrunnlag(behandlingId: UUID, kravgrunnlag: DetaljertKravgrunnlagDto) {
        logger.info("Lagrer hentet kravgrunnlag for behandling $behandlingId")
        val kravgrunnlag431 = KravgrunnlagMapper.tilKravgrunnlag431(kravgrunnlag, behandlingId)
        kravgrunnlagRepository.insert(kravgrunnlag431)
    }

    @Transactional
    fun opprettHistorikkinnslag(behandlingId: UUID) {
        logger.info(
            "Oppretter historikkinnslag ${TilbakekrevingHistorikkinnslagstype.KRAVGRUNNLAG_HENT} " +
                "for behandling $behandlingId",
        )
        historikkService.lagHistorikkinnslag(
            behandlingId = behandlingId,
            historikkinnslagstype = TilbakekrevingHistorikkinnslagstype.KRAVGRUNNLAG_HENT,
            aktør = Aktør.VEDTAKSLØSNING,
            opprettetTidspunkt = LocalDateTime.now(),
        )
    }

    private fun lagRequest(
        kravgrunnlagId: BigInteger,
        kodeAksjon: KodeAksjon,
    ): KravgrunnlagHentDetaljRequest {
        val hentkravgrunnlag = HentKravgrunnlagDetaljDto()
        hentkravgrunnlag.kravgrunnlagId = kravgrunnlagId
        hentkravgrunnlag.kodeAksjon = kodeAksjon.kode
        hentkravgrunnlag.enhetAnsvarlig = "8020" // fast verdi
        hentkravgrunnlag.saksbehId = "K231B433" // fast verdi

        val request = KravgrunnlagHentDetaljRequest()
        request.hentkravgrunnlag = hentkravgrunnlag

        return request
    }
}
