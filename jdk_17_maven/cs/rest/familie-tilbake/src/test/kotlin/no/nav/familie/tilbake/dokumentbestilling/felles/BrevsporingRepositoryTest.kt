package no.nav.familie.tilbake.dokumentbestilling.felles

import io.kotest.matchers.equality.shouldBeEqualToComparingFieldsExcept
import io.kotest.matchers.shouldBe
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.common.repository.Sporbar
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevsporing
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevtype
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import java.util.UUID

internal class BrevsporingRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var brevsporingRepository: BrevsporingRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    private val brevsporing = Testdata.brevsporing

    @BeforeEach
    fun init() {
        fagsakRepository.insert(Testdata.fagsak)
        behandlingRepository.insert(Testdata.behandling)
    }

    @Test
    fun `insert med gyldige verdier skal persistere en forekomst av Brevsporing til basen`() {
        brevsporingRepository.insert(brevsporing)

        val lagretBrevsporing = brevsporingRepository.findByIdOrThrow(brevsporing.id)

        lagretBrevsporing.shouldBeEqualToComparingFieldsExcept(brevsporing, Brevsporing::sporbar, Brevsporing::versjon)
        lagretBrevsporing.versjon shouldBe 1
    }

    @Test
    fun `update med gyldige verdier skal oppdatere en forekomst av Brevsporing i basen`() {
        brevsporingRepository.insert(brevsporing)
        var lagretBrevsporing = brevsporingRepository.findByIdOrThrow(brevsporing.id)
        val oppdatertBrevsporing = lagretBrevsporing.copy(brevtype = Brevtype.HENLEGGELSE)

        brevsporingRepository.update(oppdatertBrevsporing)

        lagretBrevsporing = brevsporingRepository.findByIdOrThrow(brevsporing.id)
        lagretBrevsporing.shouldBeEqualToComparingFieldsExcept(oppdatertBrevsporing, Brevsporing::sporbar, Brevsporing::versjon)
        lagretBrevsporing.versjon shouldBe 2
    }

    @Test
    fun `findFirstByBehandlingIdAndBrevtypeOrderBySporbarOpprettetTidDesc rerturnerer siste brevsporing`() {
        brevsporingRepository.insert(brevsporing)
        val nyesteBrevsporing = brevsporingRepository
            .insert(
                brevsporing.copy(
                    id = UUID.randomUUID(),
                    sporbar = Sporbar(opprettetTid = LocalDateTime.now().plusSeconds(1)),
                ),
            )

        val funnetBrevsporing =
            brevsporingRepository.findFirstByBehandlingIdAndBrevtypeOrderBySporbarOpprettetTidDesc(
                Testdata.behandling.id,
                Brevtype.VARSEL,
            )

        funnetBrevsporing?.shouldBeEqualToComparingFieldsExcept(nyesteBrevsporing, Brevsporing::sporbar, Brevsporing::versjon)
    }
}
