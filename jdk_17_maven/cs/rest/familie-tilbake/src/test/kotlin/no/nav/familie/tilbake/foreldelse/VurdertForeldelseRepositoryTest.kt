package no.nav.familie.tilbake.foreldelse

import io.kotest.matchers.equality.shouldBeEqualToComparingFieldsExcept
import io.kotest.matchers.shouldBe
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.foreldelse.domain.VurdertForeldelse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class VurdertForeldelseRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var vurdertForeldelseRepository: VurdertForeldelseRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    private val vurdertForeldelse = Testdata.vurdertForeldelse

    @BeforeEach
    fun init() {
        fagsakRepository.insert(Testdata.fagsak)
        behandlingRepository.insert(Testdata.behandling)
    }

    @Test
    fun `insert med gyldige verdier skal persistere en forekomst av VurdertForeldelse til basen`() {
        vurdertForeldelseRepository.insert(vurdertForeldelse)

        val lagretVurdertForeldelse = vurdertForeldelseRepository.findByIdOrThrow(vurdertForeldelse.id)

        lagretVurdertForeldelse.shouldBeEqualToComparingFieldsExcept(
            vurdertForeldelse,
            VurdertForeldelse::sporbar,
            VurdertForeldelse::versjon,
        )
        lagretVurdertForeldelse.versjon shouldBe 1
    }

    @Test
    fun `update med gyldige verdier skal oppdatere en forekomst av VurdertForeldelse i basen`() {
        vurdertForeldelseRepository.insert(vurdertForeldelse)
        var lagretVurdertForeldelse = vurdertForeldelseRepository.findByIdOrThrow(vurdertForeldelse.id)
        val oppdatertVurdertForeldelse = lagretVurdertForeldelse.copy(aktiv = false)

        vurdertForeldelseRepository.update(oppdatertVurdertForeldelse)

        lagretVurdertForeldelse = vurdertForeldelseRepository.findByIdOrThrow(vurdertForeldelse.id)
        lagretVurdertForeldelse.shouldBeEqualToComparingFieldsExcept(
            oppdatertVurdertForeldelse,
            VurdertForeldelse::sporbar,
            VurdertForeldelse::versjon,
        )
        lagretVurdertForeldelse.versjon shouldBe 2
    }
}
