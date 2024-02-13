package no.nav.familie.tilbake.avstemming.domain

import io.kotest.matchers.equality.shouldBeEqualToComparingFieldsExcept
import io.kotest.matchers.shouldBe
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class AvstemmingsfilRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var avstemmingsfilRepository: AvstemmingsfilRepository

    private val avstemmingsfil = Testdata.avstemmingsfil

    @Test
    fun `insert med gyldige verdier skal persistere en forekomst av Avstemmingsfil til basen`() {
        avstemmingsfilRepository.insert(avstemmingsfil)

        val lagretAvstemmingsfil = avstemmingsfilRepository.findByIdOrThrow(avstemmingsfil.id)

        lagretAvstemmingsfil.shouldBeEqualToComparingFieldsExcept(
            avstemmingsfil,
            Avstemmingsfil::fil,
            Avstemmingsfil::sporbar,
            Avstemmingsfil::versjon,
        )
        lagretAvstemmingsfil.versjon shouldBe 1
    }
}
