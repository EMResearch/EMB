package no.nav.familie.ba.sak.kjerne.autovedtak.satsendring

import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.MultiGauge
import io.micrometer.core.instrument.Tags
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.domene.SatskjøringRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.log.mdc.MDCConstants
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.UUID
import java.util.concurrent.TimeUnit

@Component
class SatsendringStatistikk(
    private val fagsakRepository: FagsakRepository,
    private val satskjøringRepository: SatskjøringRepository,
    private val startSatsendring: StartSatsendring,
) {

    val satsendringGauge =
        MultiGauge.builder("satsendring").register(Metrics.globalRegistry)

    @Scheduled(
        fixedDelay = 60,
        timeUnit = TimeUnit.MINUTES,
        initialDelay = 5,
    )
    fun antallSatsendringerKjørt() {
        try {
            MDC.put(MDCConstants.MDC_CALL_ID, UUID.randomUUID().toString())
            logger.info("Kjører statistikk satsendring")
            val satsTidspunkt = startSatsendring.hentAktivSatsendringstidspunkt()
            val antallKjørt = satskjøringRepository.countByFerdigTidspunktIsNotNullAndSatsTidspunkt(satsTidspunkt)
            val antallTriggetTotalt = satskjøringRepository.countBySatsTidspunkt(satsTidspunkt)
            val antallLøpendeFagsakerTotalt = fagsakRepository.finnAntallFagsakerLøpende()

            val rows = listOf(
                MultiGauge.Row.of(
                    Tags.of(
                        "satsendring",
                        "totalt",
                    ),
                    antallTriggetTotalt,
                ),
                MultiGauge.Row.of(
                    Tags.of(
                        "satsendring",
                        "antallkjort",
                    ),
                    antallKjørt,
                ),
                MultiGauge.Row.of(
                    Tags.of(
                        "satsendring",
                        "antallfagsaker",
                    ),
                    antallLøpendeFagsakerTotalt,
                ),
                MultiGauge.Row.of(
                    Tags.of(
                        "satsendring",
                        "antallgjenstaaende",
                    ),
                    antallLøpendeFagsakerTotalt - antallKjørt,
                ),
            )

            satsendringGauge.register(rows, true)
        } finally {
            MDC.clear()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SatsendringStatistikk::class.java)
    }
}
