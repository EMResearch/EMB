package no.nav.familie.ba.sak.config.featureToggle.miljø

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.core.env.Environment

class EnvironmentConfigTest {

    @Test
    fun `aktiv profil skal være aktiv`() {
        val env = mockk<Environment>().also { every { it.activeProfiles } returns arrayOf("dev") }
        assertThat(env.erAktiv(Profil.Dev)).isTrue
    }

    @Test
    fun `profil som ikke er lista som aktiv skal ikke være aktiv`() {
        val env = mockk<Environment>().also { every { it.activeProfiles } returns arrayOf("prod") }
        assertThat(env.erAktiv(Profil.Dev)).isFalse
    }
}
