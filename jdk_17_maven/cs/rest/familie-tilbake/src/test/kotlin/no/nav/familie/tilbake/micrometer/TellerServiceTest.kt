package no.nav.familie.tilbake.micrometer

import io.kotest.matchers.shouldBe
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.micrometer.domain.MeldingstellingRepository
import no.nav.familie.tilbake.micrometer.domain.Meldingstype
import no.nav.familie.tilbake.micrometer.domain.Mottaksstatus
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class TellerServiceTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var tellerService: TellerService

    @Autowired
    private lateinit var meldingstellingRepository: MeldingstellingRepository

    @Test
    fun `tellKobletKravgrunnlag oppretter ny forekomst ved dagnes første telling`() {
        tellerService.tellKobletKravgrunnlag(fagsystem = Fagsystem.EF)

        val meldingstelling = meldingstellingRepository.findByFagsystemAndTypeAndStatusAndDato(
            Fagsystem.EF,
            Meldingstype.KRAVGRUNNLAG,
            Mottaksstatus.KOBLET,
        )

        meldingstelling!!.antall shouldBe 1
    }

    @Test
    fun `tellUkobletKravgrunnlag oppretter ny forekomst ved dagnes første telling`() {
        tellerService.tellUkobletKravgrunnlag(fagsystem = Fagsystem.EF)

        val meldingstelling = meldingstellingRepository.findByFagsystemAndTypeAndStatusAndDato(
            Fagsystem.EF,
            Meldingstype.KRAVGRUNNLAG,
            Mottaksstatus.UKOBLET,
        )

        meldingstelling!!.antall shouldBe 1
    }

    @Test
    fun `tellKobletStatusmelding oppretter ny forekomst ved dagnes første telling`() {
        tellerService.tellKobletStatusmelding(fagsystem = Fagsystem.EF)

        val meldingstelling = meldingstellingRepository.findByFagsystemAndTypeAndStatusAndDato(
            Fagsystem.EF,
            Meldingstype.STATUSMELDING,
            Mottaksstatus.KOBLET,
        )

        meldingstelling!!.antall shouldBe 1
    }

    @Test
    fun `tellUkobletStatusmelding oppretter ny forekomst ved dagnes første telling`() {
        tellerService.tellUkobletStatusmelding(fagsystem = Fagsystem.EF)

        val meldingstelling = meldingstellingRepository.findByFagsystemAndTypeAndStatusAndDato(
            Fagsystem.EF,
            Meldingstype.STATUSMELDING,
            Mottaksstatus.UKOBLET,
        )

        meldingstelling!!.antall shouldBe 1
    }

    @Test
    fun `tellKobletKravgrunnlag oppdaterer eksisterende teller ved påfølgende tellinger`() {
        tellerService.tellKobletKravgrunnlag(fagsystem = Fagsystem.EF)
        tellerService.tellKobletKravgrunnlag(fagsystem = Fagsystem.EF)

        val meldingstelling = meldingstellingRepository.findByFagsystemAndTypeAndStatusAndDato(
            Fagsystem.EF,
            Meldingstype.KRAVGRUNNLAG,
            Mottaksstatus.KOBLET,
        )

        meldingstelling!!.antall shouldBe 2
    }

    @Test
    fun `tellUkobletKravgrunnlag oppdaterer eksisterende teller ved påfølgende tellinger`() {
        tellerService.tellUkobletKravgrunnlag(fagsystem = Fagsystem.EF)
        tellerService.tellUkobletKravgrunnlag(fagsystem = Fagsystem.EF)

        val meldingstelling = meldingstellingRepository.findByFagsystemAndTypeAndStatusAndDato(
            Fagsystem.EF,
            Meldingstype.KRAVGRUNNLAG,
            Mottaksstatus.UKOBLET,
        )

        meldingstelling!!.antall shouldBe 2
    }

    @Test
    fun `tellKobletStatusmelding oppdaterer eksisterende teller ved påfølgende tellinger`() {
        tellerService.tellKobletStatusmelding(fagsystem = Fagsystem.EF)
        tellerService.tellKobletStatusmelding(fagsystem = Fagsystem.EF)

        val meldingstelling = meldingstellingRepository.findByFagsystemAndTypeAndStatusAndDato(
            Fagsystem.EF,
            Meldingstype.STATUSMELDING,
            Mottaksstatus.KOBLET,
        )

        meldingstelling!!.antall shouldBe 2
    }

    @Test
    fun `tellUkobletStatusmelding oppdaterer eksisterende teller ved påfølgende tellinger`() {
        tellerService.tellUkobletStatusmelding(fagsystem = Fagsystem.EF)
        tellerService.tellUkobletStatusmelding(fagsystem = Fagsystem.EF)

        val meldingstelling = meldingstellingRepository.findByFagsystemAndTypeAndStatusAndDato(
            Fagsystem.EF,
            Meldingstype.STATUSMELDING,
            Mottaksstatus.UKOBLET,
        )

        meldingstelling!!.antall shouldBe 2
    }
}
