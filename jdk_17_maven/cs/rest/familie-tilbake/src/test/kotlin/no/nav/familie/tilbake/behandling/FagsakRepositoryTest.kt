package no.nav.familie.tilbake.behandling

import io.kotest.matchers.equality.shouldBeEqualToComparingFieldsExcept
import io.kotest.matchers.shouldBe
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class FagsakRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    private val fagsak = Testdata.fagsak

    @Test
    fun `insert med gyldige verdier skal persistere en forekomst av Fagsak til basen`() {
        fagsakRepository.insert(fagsak)

        val lagretFagsak = fagsakRepository.findByIdOrThrow(fagsak.id)
        lagretFagsak.shouldBeEqualToComparingFieldsExcept(fagsak, Fagsak::sporbar, Fagsak::versjon)
        lagretFagsak.versjon shouldBe 1
    }

    @Test
    fun `update med gyldige verdier skal oppdatere en forekomst av Fagsak i basen`() {
        fagsakRepository.insert(fagsak)
        var lagretFagsak = fagsakRepository.findByIdOrThrow(fagsak.id)
        val oppdatertFagsak = lagretFagsak.copy(eksternFagsakId = "1")

        fagsakRepository.update(oppdatertFagsak)

        lagretFagsak = fagsakRepository.findByIdOrThrow(fagsak.id)
        lagretFagsak.shouldBeEqualToComparingFieldsExcept(oppdatertFagsak, Fagsak::sporbar, Fagsak::versjon)
        lagretFagsak.versjon shouldBe 2
    }
}
