package no.nav.familie.ba.sak.kjerne.fagsak

import no.nav.familie.ba.sak.common.DbContainerInitializer
import no.nav.familie.ba.sak.ekstern.restDomene.RestFagsakDeltager
import no.nav.familie.ba.sak.ekstern.restDomene.RestSøkParam
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonException
import no.nav.familie.kontrakter.felles.Ressurs
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration

@SpringBootTest
@ActiveProfiles(
    "postgres",
    "integrasjonstest",
    "mock-oauth",
    "mock-pdl-test-søk",
    "mock-ident-client",
    "mock-infotrygd-barnetrygd",
    "mock-brev-klient",
)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("integration")
class SøkFagsakNegativeTest {

    @Autowired
    lateinit var fagsakService: FagsakService

    @Autowired
    lateinit var fagsakController: FagsakController

    @Test
    fun `test å søke fagsak deltager med ugyldig fnr`() {
        val feilId = "41235678910"
        assertThrows<IntegrasjonException> {
            fagsakService.hentFagsakDeltager(feilId)
        }
    }

    @Test
    fun `test generer riktig ressur ved feil`() {
        val ukjentId = "43125678910"
        val feilId = "41235678910"

        val resEntity1 = fagsakController.søkFagsak(RestSøkParam(ukjentId))
        assertThat(HttpStatus.OK).isEqualTo(resEntity1.statusCode)
        val ress = resEntity1.body as Ressurs<List<RestFagsakDeltager>>
        assertThat(Ressurs.Status.SUKSESS).isEqualTo(ress.status)
        assertThat(ress.data).isEqualTo(emptyList<RestFagsakDeltager>())

        assertThrows<IntegrasjonException> {
            fagsakController.søkFagsak(RestSøkParam(feilId))
        }
    }
}
