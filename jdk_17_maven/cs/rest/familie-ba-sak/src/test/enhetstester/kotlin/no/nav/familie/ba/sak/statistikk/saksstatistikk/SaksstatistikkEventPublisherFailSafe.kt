package no.nav.familie.ba.sak.statistikk.saksstatistikk

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@TestConfiguration
class SaksstatistikkEventPublisherFailSafe : SaksstatistikkEventPublisher() {

    @Bean
    @Primary
    fun safeSaksstatistikkEventPublisher(): SaksstatistikkEventPublisher {
        return this
    }

    override fun publiserBehandlingsstatistikk(behandlingId: Long) {
        runCatching {
            super.publiserBehandlingsstatistikk(behandlingId)
        }.onFailure { it.printStackTrace() }
    }

    override fun publiserSaksstatistikk(fagsakId: Long) {
        runCatching {
            super.publiserSaksstatistikk(fagsakId)
        }.onFailure { it.printStackTrace() }
    }
}
