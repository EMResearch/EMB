package no.nav.familie.ba.sak.config

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ba.sak.integrasjoner.ef.EfSakRestClient
import no.nav.familie.kontrakter.felles.ef.Datakilde
import no.nav.familie.kontrakter.felles.ef.EksternPeriode
import no.nav.familie.kontrakter.felles.ef.EksternePerioderResponse
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import java.time.LocalDate

@TestConfiguration
class EfSakRestClientMock {

    @Bean
    @Primary
    fun mockEfSakRestClient(): EfSakRestClient {
        val efSakRestClient = mockk<EfSakRestClient>()

        clearEfSakRestMocks(efSakRestClient)

        return efSakRestClient
    }

    companion object {
        fun clearEfSakRestMocks(efSakRestClient: EfSakRestClient) {
            clearMocks(efSakRestClient)

            val hentPerioderMedFullOvergangsstønadSlot = slot<String>()
            every { efSakRestClient.hentPerioderMedFullOvergangsstønad(capture(hentPerioderMedFullOvergangsstønadSlot)) } answers {
                EksternePerioderResponse(
                    perioder = listOf(
                        EksternPeriode(
                            personIdent = hentPerioderMedFullOvergangsstønadSlot.captured,
                            fomDato = LocalDate.now().minusYears(2),
                            datakilde = Datakilde.EF,
                            tomDato = LocalDate.now().minusMonths(3),
                        ),
                    ),
                )
            }
        }
    }
}
