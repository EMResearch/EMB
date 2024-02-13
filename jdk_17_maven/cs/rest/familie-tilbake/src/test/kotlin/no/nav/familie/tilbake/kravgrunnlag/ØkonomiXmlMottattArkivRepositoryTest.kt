package no.nav.familie.tilbake.kravgrunnlag

import io.kotest.matchers.equality.shouldBeEqualToComparingFieldsExcept
import io.kotest.matchers.shouldBe
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.kravgrunnlag.domain.ØkonomiXmlMottattArkiv
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class ØkonomiXmlMottattArkivRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var økonomiXmlMottattArkivRepository: ØkonomiXmlMottattArkivRepository

    private val økonomiXmlMottattArkiv = Testdata.økonomiXmlMottattArkiv

    @Test
    fun `insert med gyldige verdier skal persistere en forekomst av ØkonomiXmlMottattArkiv til basen`() {
        økonomiXmlMottattArkivRepository.insert(økonomiXmlMottattArkiv)

        val lagretØkonomiXmlMottattArkiv = økonomiXmlMottattArkivRepository.findByIdOrThrow(økonomiXmlMottattArkiv.id)

        lagretØkonomiXmlMottattArkiv.shouldBeEqualToComparingFieldsExcept(
            økonomiXmlMottattArkiv,
            ØkonomiXmlMottattArkiv::sporbar,
            ØkonomiXmlMottattArkiv::versjon,
        )
        lagretØkonomiXmlMottattArkiv.versjon shouldBe 1
    }

    @Test
    fun `update med gyldige verdier skal oppdatere en forekomst av ØkonomiXmlMottattArkiv i basen`() {
        økonomiXmlMottattArkivRepository.insert(økonomiXmlMottattArkiv)
        var lagretØkonomiXmlMottattArkiv = økonomiXmlMottattArkivRepository.findByIdOrThrow(økonomiXmlMottattArkiv.id)
        val oppdatertØkonomiXmlMottattArkiv = lagretØkonomiXmlMottattArkiv.copy(melding = "bob")

        økonomiXmlMottattArkivRepository.update(oppdatertØkonomiXmlMottattArkiv)

        lagretØkonomiXmlMottattArkiv = økonomiXmlMottattArkivRepository.findByIdOrThrow(økonomiXmlMottattArkiv.id)
        lagretØkonomiXmlMottattArkiv.shouldBeEqualToComparingFieldsExcept(
            oppdatertØkonomiXmlMottattArkiv,
            ØkonomiXmlMottattArkiv::sporbar,
            ØkonomiXmlMottattArkiv::versjon,
        )
        lagretØkonomiXmlMottattArkiv.versjon shouldBe 2
    }
}
