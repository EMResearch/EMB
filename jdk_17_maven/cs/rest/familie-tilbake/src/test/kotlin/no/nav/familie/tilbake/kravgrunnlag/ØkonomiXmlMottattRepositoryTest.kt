package no.nav.familie.tilbake.kravgrunnlag

import io.kotest.matchers.equality.shouldBeEqualToComparingFieldsExcept
import io.kotest.matchers.shouldBe
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.kravgrunnlag.domain.ØkonomiXmlMottatt
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class ØkonomiXmlMottattRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var økonomiXmlMottattRepository: ØkonomiXmlMottattRepository

    private val økonomiXmlMottatt = Testdata.økonomiXmlMottatt

    @Test
    fun `insert med gyldige verdier skal persistere en forekomst av ØkonomiXmlMottatt til basen`() {
        økonomiXmlMottattRepository.insert(økonomiXmlMottatt)

        val lagretØkonomiXmlMottatt = økonomiXmlMottattRepository.findByIdOrThrow(økonomiXmlMottatt.id)

        lagretØkonomiXmlMottatt.shouldBeEqualToComparingFieldsExcept(
            økonomiXmlMottatt,
            ØkonomiXmlMottatt::sporbar,
            ØkonomiXmlMottatt::versjon,
        )
        lagretØkonomiXmlMottatt.versjon shouldBe 1
    }

    @Test
    fun `update med gyldige verdier skal oppdatere en forekomst av ØkonomiXmlMottatt i basen`() {
        økonomiXmlMottattRepository.insert(økonomiXmlMottatt)
        var lagretØkonomiXmlMottatt = økonomiXmlMottattRepository.findByIdOrThrow(økonomiXmlMottatt.id)
        val oppdatertØkonomiXmlMottatt = lagretØkonomiXmlMottatt.copy(eksternFagsakId = "bob")

        økonomiXmlMottattRepository.update(oppdatertØkonomiXmlMottatt)

        lagretØkonomiXmlMottatt = økonomiXmlMottattRepository.findByIdOrThrow(økonomiXmlMottatt.id)
        lagretØkonomiXmlMottatt.shouldBeEqualToComparingFieldsExcept(
            oppdatertØkonomiXmlMottatt,
            ØkonomiXmlMottatt::sporbar,
            ØkonomiXmlMottatt::versjon,
        )
        lagretØkonomiXmlMottatt.versjon shouldBe 2
    }
}
