package no.nav.familie.tilbake.config

import no.nav.familie.tilbake.integration.økonomi.MockOppdragClient
import no.nav.familie.tilbake.integration.økonomi.OppdragClient
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.kravgrunnlag.ØkonomiXmlMottattRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
@ComponentScan(value = ["no.nav.familie.tilbake.kravgrunnlag"])
@Profile("mock-økonomi")
class OppdragClientLokalConfig(
    private val kravgrunnlagRepository: KravgrunnlagRepository,
    private val økonomiXmlMottattRepository: ØkonomiXmlMottattRepository,
) {

    @Bean
    @Primary
    fun oppdragClient(): OppdragClient {
        return MockOppdragClient(kravgrunnlagRepository, økonomiXmlMottattRepository)
    }
}
