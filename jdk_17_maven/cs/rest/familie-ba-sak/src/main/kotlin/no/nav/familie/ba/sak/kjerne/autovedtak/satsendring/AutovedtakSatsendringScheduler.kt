package no.nav.familie.ba.sak.kjerne.autovedtak.satsendring

import no.nav.familie.leader.LeaderClient
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class AutovedtakSatsendringScheduler(
    private val startSatsendring: StartSatsendring,
) {
    @Scheduled(cron = CRON_HVERT_10_MIN_UKEDAG)
    fun triggSatsendringJuli2023() {
        startSatsendring(1200)
    }

    private fun startSatsendring(antallFagsaker: Int) {
        if (LeaderClient.isLeader() == true) {
            logger.info("Starter schedulert jobb for satsendring juli 2023")
            startSatsendring.startSatsendring(
                antallFagsaker = antallFagsaker,
            )
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(AutovedtakSatsendringScheduler::class.java)
        const val CRON_HVERT_10_MIN_UKEDAG = "0 */10 7-18 * * MON-FRI"
    }
}
