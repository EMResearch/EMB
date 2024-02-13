package no.nav.familie.tilbake.faktaomfeilutbetaling

import io.kotest.matchers.equality.shouldBeEqualToComparingFieldsExcept
import io.kotest.matchers.shouldBe
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.FaktaFeilutbetaling
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class FaktaFeilutbetalingRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var faktaFeilutbetalingRepository: FaktaFeilutbetalingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    private val fagsak = Testdata.fagsak
    private val behandling = Testdata.behandling
    private val faktaFeilutbetaling = Testdata.faktaFeilutbetaling

    @BeforeEach
    fun init() {
        fagsakRepository.insert(fagsak)
        behandlingRepository.insert(behandling)
    }

    @Test
    fun `insert med gyldige verdier skal persistere en forekomst av FaktaFeilutbetaling til basen`() {
        faktaFeilutbetalingRepository.insert(faktaFeilutbetaling)

        val lagretFaktaFeilutbetaling = faktaFeilutbetalingRepository.findByIdOrThrow(faktaFeilutbetaling.id)

        lagretFaktaFeilutbetaling.shouldBeEqualToComparingFieldsExcept(
            faktaFeilutbetaling,
            FaktaFeilutbetaling::sporbar,
            FaktaFeilutbetaling::versjon,
        )
        lagretFaktaFeilutbetaling.versjon shouldBe 1
    }

    @Test
    fun `update med gyldige verdier skal oppdatere en forekomst av FaktaFeilutbetaling i basen`() {
        faktaFeilutbetalingRepository.insert(faktaFeilutbetaling)
        var lagretFaktaFeilutbetaling = faktaFeilutbetalingRepository.findByIdOrThrow(faktaFeilutbetaling.id)
        val oppdatertFaktaFeilutbetaling = lagretFaktaFeilutbetaling.copy(begrunnelse = "bob")

        faktaFeilutbetalingRepository.update(oppdatertFaktaFeilutbetaling)

        lagretFaktaFeilutbetaling = faktaFeilutbetalingRepository.findByIdOrThrow(faktaFeilutbetaling.id)
        lagretFaktaFeilutbetaling.shouldBeEqualToComparingFieldsExcept(
            oppdatertFaktaFeilutbetaling,
            FaktaFeilutbetaling::sporbar,
            FaktaFeilutbetaling::versjon,
        )
        lagretFaktaFeilutbetaling.versjon shouldBe 2
    }

    @Test
    fun `findByBehandlingIdAndAktivIsTrue returnerer resultat når det finnes en forekomst`() {
        faktaFeilutbetalingRepository.insert(faktaFeilutbetaling)

        val findByBehandlingId = faktaFeilutbetalingRepository.findByBehandlingIdAndAktivIsTrue(behandling.id)

        findByBehandlingId?.shouldBeEqualToComparingFieldsExcept(
            faktaFeilutbetaling,
            FaktaFeilutbetaling::sporbar,
            FaktaFeilutbetaling::versjon,
        )
    }
}
