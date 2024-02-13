package no.nav.familie.ba.sak.integrasjoner.sanity

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityEØSBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.EØSStandardbegrunnelse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class SanityServiceTest {

    @MockK
    private lateinit var sanityKlient: SanityKlient

    @MockK
    private lateinit var featureToggleService: FeatureToggleService

    @InjectMockKs
    private lateinit var sanityService: SanityService

    @Test
    fun `hentSanityEØSBegrunnelser - skal filtrere bort nye begrunnelser tilknyttet EØS praksisendring dersom toggel er av`() {
        every { featureToggleService.isEnabled(FeatureToggleConfig.EØS_PRAKSISENDRING_SEPTEMBER2023) } returns false

        every { sanityKlient.hentEØSBegrunnelser() } returns EØSStandardbegrunnelse.values().map {
            SanityEØSBegrunnelse(
                apiNavn = it.sanityApiNavn,
                navnISystem = it.name,
                fagsakType = null,
                periodeType = null,
                tema = null,
                vilkår = emptySet(),
                annenForeldersAktivitet = emptyList(),
                barnetsBostedsland = emptyList(),
                kompetanseResultat = emptyList(),
                hjemler = emptyList(),
                hjemlerFolketrygdloven = emptyList(),
                hjemlerEØSForordningen883 = emptyList(),
                hjemlerEØSForordningen987 = emptyList(),
                hjemlerSeperasjonsavtalenStorbritannina = emptyList(),
            )
        }

        val eøsBegrunnelser = sanityService.hentSanityEØSBegrunnelser()

        assertThat(eøsBegrunnelser.keys).doesNotContainAnyElementsOf(EØSStandardbegrunnelse.eøsPraksisendringBegrunnelser())
    }

    @Test
    fun `hentSanityEØSBegrunnelser - skal ikke filtrere bort nye begrunnelser tilknyttet EØS praksisendring dersom toggel er på`() {
        every { featureToggleService.isEnabled(FeatureToggleConfig.EØS_PRAKSISENDRING_SEPTEMBER2023) } returns true

        every { sanityKlient.hentEØSBegrunnelser() } returns EØSStandardbegrunnelse.values().map {
            SanityEØSBegrunnelse(
                apiNavn = it.sanityApiNavn,
                navnISystem = it.name,
                fagsakType = null,
                periodeType = null,
                tema = null,
                vilkår = emptySet(),
                annenForeldersAktivitet = emptyList(),
                barnetsBostedsland = emptyList(),
                kompetanseResultat = emptyList(),
                hjemler = emptyList(),
                hjemlerFolketrygdloven = emptyList(),
                hjemlerEØSForordningen883 = emptyList(),
                hjemlerEØSForordningen987 = emptyList(),
                hjemlerSeperasjonsavtalenStorbritannina = emptyList(),
            )
        }

        val eøsBegrunnelser = sanityService.hentSanityEØSBegrunnelser()

        assertThat(eøsBegrunnelser.keys).isEqualTo(EØSStandardbegrunnelse.values().toSet())
    }
}
