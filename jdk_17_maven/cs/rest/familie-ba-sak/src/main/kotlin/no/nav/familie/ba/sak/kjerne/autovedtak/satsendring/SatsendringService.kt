package no.nav.familie.ba.sak.kjerne.autovedtak.satsendring

import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.log.mdc.MDCConstants
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Slice
import org.springframework.stereotype.Service
import java.util.stream.Collectors

@Service
class SatsendringService(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService,
    private val fagsakRepository: FagsakRepository,
) {
    private val logger = LoggerFactory.getLogger(SatsendringService::class.java)
    fun erFagsakOppdatertMedSisteSatser(fagsakId: Long): Boolean {
        val sisteVedtatteBehandling =
            behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsakId)

        return sisteVedtatteBehandling == null ||
            andelerTilkjentYtelseOgEndreteUtbetalingerService
                .finnAndelerTilkjentYtelseMedEndreteUtbetalinger(sisteVedtatteBehandling.id)
                .erOppdatertMedSisteSatser()
    }

    fun finnLøpendeFagsakerUtenSisteSats(callId: String) {
        MDC.put(MDCConstants.MDC_CALL_ID, callId)
        val fagsakerUtenSisteSats = mutableListOf<Long>()
        var slice: Slice<Long> = fagsakRepository.finnLøpendeFagsaker(PageRequest.of(0, 10000))
        val løpendeFagsaker: List<Long> = slice.getContent()
        fagsakerUtenSisteSats.addAll(
            løpendeFagsaker.parallelStream().filter {
                !erFagsakOppdatertMedSisteSatser(it)
            }.collect(
                Collectors.toList(),
            ),
        )

        while (slice.hasNext()) {
            logger.info("Next slice")
            slice = fagsakRepository.finnLøpendeFagsaker(slice.nextPageable())
            fagsakerUtenSisteSats.addAll(
                slice.get().toList().parallelStream().filter {
                    !erFagsakOppdatertMedSisteSatser(it)
                }.collect(
                    Collectors.toList(),
                ),
            )
        }
        logger.warn("Følgende saker mangler satsendring:")
        fagsakerUtenSisteSats.chunked(1000) {
            logger.warn("$it")
        }
    }
}
