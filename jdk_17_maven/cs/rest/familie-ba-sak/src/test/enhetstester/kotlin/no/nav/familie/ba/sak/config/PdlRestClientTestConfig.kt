package no.nav.familie.ba.sak.config

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlRestClient
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.ForelderBarnRelasjon
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PersonInfo
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import no.nav.familie.kontrakter.felles.personopplysning.Sivilstand
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import java.time.LocalDate

@TestConfiguration
class PdlRestClientTestConfig {

    @Bean
    @Profile("mock-pdl-client")
    @Primary
    fun pdlRestClientMock(): PdlRestClient {
        val klient = mockk<PdlRestClient>(relaxed = true)

        every {
            klient.hentPerson(any(), any())
        } returns PersonInfo(
            fødselsdato = LocalDate.of(1980, 5, 12),
            navn = "Kari Normann",
            kjønn = Kjønn.KVINNE,
            forelderBarnRelasjon = setOf(
                ForelderBarnRelasjon(
                    aktør = tilAktør("12345678910"),
                    relasjonsrolle = FORELDERBARNRELASJONROLLE.BARN,
                ),
            ),
            adressebeskyttelseGradering = null,
            sivilstander = listOf(Sivilstand(type = SIVILSTAND.UGIFT)),
            dødsfall = null,
            kontaktinformasjonForDoedsbo = null,
        )
        return klient
    }
}
