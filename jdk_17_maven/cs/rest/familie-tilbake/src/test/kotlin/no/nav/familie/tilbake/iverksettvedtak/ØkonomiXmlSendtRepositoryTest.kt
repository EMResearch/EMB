package no.nav.familie.tilbake.iverksettvedtak

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.equality.shouldBeEqualToComparingFieldsExcept
import io.kotest.matchers.shouldBe
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.iverksettvedtak.domain.ØkonomiXmlSendt
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

internal class ØkonomiXmlSendtRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var økonomiXmlSendtRepository: ØkonomiXmlSendtRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    private val økonomiXmlSendt = Testdata.økonomiXmlSendt

    @BeforeEach
    fun init() {
        fagsakRepository.insert(Testdata.fagsak)
        behandlingRepository.insert(Testdata.behandling)
    }

    @Test
    fun `insert med gyldige verdier skal persistere en forekomst av ØkonomiXmlSendt til basen`() {
        økonomiXmlSendtRepository.insert(økonomiXmlSendt)

        val lagretØkonomiXmlSendt = økonomiXmlSendtRepository.findByIdOrThrow(økonomiXmlSendt.id)

        lagretØkonomiXmlSendt.shouldBeEqualToComparingFieldsExcept(
            økonomiXmlSendt,
            ØkonomiXmlSendt::sporbar,
            ØkonomiXmlSendt::versjon,
        )
        lagretØkonomiXmlSendt.versjon shouldBe 1
    }

    @Test
    fun `findByMeldingstypeAndSporbarOpprettetTidAfter skal finne forekomster hvis det finnes for søkekriterier`() {
        økonomiXmlSendtRepository.insert(økonomiXmlSendt)

        val lagretØkonomiXmlSendt =
            økonomiXmlSendtRepository.findByOpprettetPåDato(LocalDate.now())

        lagretØkonomiXmlSendt.shouldNotBeEmpty()
    }

    @Test
    fun `findByMeldingstypeAndSporbarOpprettetTidAfter skal ikke finne forekomster hvis det ikke finnes for søkekriterier`() {
        økonomiXmlSendtRepository.insert(økonomiXmlSendt)

        val lagretØkonomiXmlSendt =
            økonomiXmlSendtRepository.findByOpprettetPåDato(LocalDate.now().plusDays(1))

        lagretØkonomiXmlSendt.shouldBeEmpty()
    }

    @Test
    fun `update med gyldige verdier skal oppdatere en forekomst av ØkonomiXmlSendt i basen`() {
        økonomiXmlSendtRepository.insert(økonomiXmlSendt)
        var lagretØkonomiXmlSendt = økonomiXmlSendtRepository.findByIdOrThrow(økonomiXmlSendt.id)
        val oppdatertØkonomiXmlSendt = lagretØkonomiXmlSendt.copy(melding = "bob")

        økonomiXmlSendtRepository.update(oppdatertØkonomiXmlSendt)

        lagretØkonomiXmlSendt = økonomiXmlSendtRepository.findByIdOrThrow(økonomiXmlSendt.id)
        lagretØkonomiXmlSendt.shouldBeEqualToComparingFieldsExcept(
            oppdatertØkonomiXmlSendt,
            ØkonomiXmlSendt::sporbar,
            ØkonomiXmlSendt::versjon,
        )
        lagretØkonomiXmlSendt.versjon shouldBe 2
    }
}
