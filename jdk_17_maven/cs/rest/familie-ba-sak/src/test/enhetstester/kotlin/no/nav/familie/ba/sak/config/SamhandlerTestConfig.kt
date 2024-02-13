package no.nav.familie.ba.sak.config

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.integrasjoner.samhandler.SamhandlerKlient
import no.nav.familie.kontrakter.ba.tss.SamhandlerAdresse
import no.nav.familie.kontrakter.ba.tss.SamhandlerInfo
import no.nav.familie.kontrakter.ba.tss.SøkSamhandlerInfo
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@TestConfiguration
class SamhandlerTestConfig {

    @Bean
    @Profile("mock-økonomi")
    @Primary
    fun mockSamhandlerKlient(): SamhandlerKlient {
        val mockSamhandlerKlient: SamhandlerKlient = mockk()

        clearSamhandlerKlient(mockSamhandlerKlient)

        return mockSamhandlerKlient
    }

    companion object {
        fun clearSamhandlerKlient(samhandlerKlient: SamhandlerKlient) {
            clearMocks(samhandlerKlient)
            every { samhandlerKlient.hentSamhandler(any()) } returns samhandlereInfoMock.first()
            every { samhandlerKlient.søkSamhandlere(any(), any(), any(), any()) } returns SøkSamhandlerInfo(
                false,
                samhandlereInfoMock,
            )
        }
    }
}

val samhandlereInfoMock = listOf(
    SamhandlerInfo(
        "80000999999",
        "INSTUTISJON 1",
        listOf(
            SamhandlerAdresse(listOf("Instutisjonsnsveien 1"), "0110", "Oslo", "Arbeidsadresse"),
            SamhandlerAdresse(listOf("Postboks 123"), "0110", "Oslo", "Postadresse"),
        ),
        orgNummer = "974652269",
    ),
    SamhandlerInfo(
        "80000888888",
        "INSTUTISJON 2",
        listOf(SamhandlerAdresse(listOf("Instutisjonsnsveien 2"), "1892", "Degernes", "Arbeidsadresse")),
        orgNummer = "974652269",
    ),
)
