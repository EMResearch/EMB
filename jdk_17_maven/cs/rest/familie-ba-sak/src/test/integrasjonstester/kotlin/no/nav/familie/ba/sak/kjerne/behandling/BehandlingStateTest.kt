package no.nav.familie.ba.sak.kjerne.behandling

import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.DatabaseCleanupService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class BehandlingStateTest(
    @Autowired private val fagsakService: FagsakService,
    @Autowired private val behandlingRepository: BehandlingRepository,
    @Autowired private val databaseCleanupService: DatabaseCleanupService,
) : AbstractSpringIntegrationTest() {

    private lateinit var fagsak: Fagsak

    @BeforeEach
    fun setUp() {
        databaseCleanupService.truncate()
        fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(tilfeldigPerson().aktør.aktivFødselsnummer())
    }

    @Nested
    inner class AktivBehandling {
        @Test
        fun `kan ikke ha flere behandlinger med aktiv true`() {
            opprettBehandling(status = BehandlingStatus.AVSLUTTET, aktiv = true)
            assertThatThrownBy {
                opprettBehandling(status = BehandlingStatus.AVSLUTTET, aktiv = true)
            }.hasMessageContaining("uidx_behandling_01")
        }

        @Test
        fun `skal kunne ha aktiv tvers ulike fagsaker`() {
            opprettBehandling(status = BehandlingStatus.AVSLUTTET, aktiv = true)
            val annenFagsak =
                fagsakService.hentEllerOpprettFagsakForPersonIdent(tilfeldigPerson().aktør.aktivFødselsnummer())
            opprettBehandling(annenFagsak, status = BehandlingStatus.AVSLUTTET, aktiv = true)
        }
    }

    @Nested
    inner class BehandlingStatuser {

        @Test
        fun `kan ha flere behandlinger som er avsluttet`() {
            opprettBehandling(status = BehandlingStatus.AVSLUTTET, aktiv = false)
            opprettBehandling(status = BehandlingStatus.AVSLUTTET, aktiv = true)
        }

        @Test
        fun `kan ha en behandling på maskinell vent og en med status utredes`() {
            opprettBehandling(status = BehandlingStatus.AVSLUTTET, aktiv = false)
            opprettBehandling(status = BehandlingStatus.SATT_PÅ_MASKINELL_VENT, aktiv = false)
            opprettBehandling(status = BehandlingStatus.UTREDES, aktiv = true)
        }

        @Test
        fun `kan ikke ha 2 behandlinger med status SATT_PÅ_VENTSATT_PÅ_MASKINELL_VENT`() {
            opprettBehandling(status = BehandlingStatus.SATT_PÅ_MASKINELL_VENT, aktiv = false)
            assertThatThrownBy {
                opprettBehandling(status = BehandlingStatus.SATT_PÅ_MASKINELL_VENT, aktiv = true)
            }.hasMessageContaining("uidx_behandling_03")
        }

        @Test
        fun `kan maks ha en parallell behandling i arbeid`() {
            BehandlingStatus.values().filter { it != BehandlingStatus.AVSLUTTET && it != BehandlingStatus.SATT_PÅ_MASKINELL_VENT }
                .forEach {
                    behandlingRepository.deleteAll()
                    opprettBehandling(status = it, aktiv = false)
                    assertThatThrownBy {
                        opprettBehandling(status = it, aktiv = true)
                    }.hasMessageContaining("uidx_behandling_02")
                }
        }
    }

    private fun opprettBehandling(status: BehandlingStatus, aktiv: Boolean): Behandling {
        return opprettBehandling(fagsak, status, aktiv)
    }

    private fun opprettBehandling(
        fagsak: Fagsak,
        status: BehandlingStatus,
        aktiv: Boolean,
    ): Behandling {
        val behandling = Behandling(
            fagsak = fagsak,
            opprettetÅrsak = BehandlingÅrsak.NYE_OPPLYSNINGER,
            type = BehandlingType.REVURDERING,
            kategori = BehandlingKategori.NASJONAL,
            underkategori = BehandlingUnderkategori.ORDINÆR,
            status = status,
            aktiv = aktiv,
        ).initBehandlingStegTilstand()
        return behandlingRepository.saveAndFlush(behandling)
    }
}
