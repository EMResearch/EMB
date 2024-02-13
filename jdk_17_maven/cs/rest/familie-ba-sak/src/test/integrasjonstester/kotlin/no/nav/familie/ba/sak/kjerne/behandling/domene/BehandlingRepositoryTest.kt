package no.nav.familie.ba.sak.kjerne.behandling.domene

import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagInitiellTilkjentYtelse
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.DatabaseCleanupService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus.AVSLUTTET
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus.IVERKSETTER_VEDTAK
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime

class BehandlingRepositoryTest(
    @Autowired private val fagsakService: FagsakService,
    @Autowired private val behandlingRepository: BehandlingRepository,
    @Autowired private val tilkjentRepository: TilkjentYtelseRepository,
    @Autowired private val databaseCleanupService: DatabaseCleanupService,
) : AbstractSpringIntegrationTest() {

    @BeforeEach
    fun cleanUp() {
        databaseCleanupService.truncate()
    }

    @Nested
    inner class FinnSisteIverksatteBehandling {

        val tilfeldigPerson = tilfeldigPerson()
        val tilfeldigPerson2 = tilfeldigPerson()
        lateinit var fagsak: Fagsak
        lateinit var fagsak2: Fagsak

        @BeforeEach
        fun setUp() {
            fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(tilfeldigPerson.aktør.aktivFødselsnummer())
            fagsak2 = fagsakService.hentEllerOpprettFagsakForPersonIdent(tilfeldigPerson2.aktør.aktivFødselsnummer())
        }

        @Test
        fun `skal finne siste iverksatte behandlingen som har utbetalingsoppdrag, som er avsluttet`() {
            opprettBehandling(fagsak, AVSLUTTET, LocalDateTime.now().minusDays(3))
                .medTilkjentYtelse(true)
            val behandling2 = opprettBehandling(fagsak, AVSLUTTET, LocalDateTime.now().minusDays(2))
                .medTilkjentYtelse(true)
            opprettBehandling(fagsak, IVERKSETTER_VEDTAK, LocalDateTime.now().minusDays(1))
                .medTilkjentYtelse(true)

            val behandling4 = opprettBehandling(fagsak2, AVSLUTTET, LocalDateTime.now())
                .medTilkjentYtelse(true)

            assertThat(behandlingRepository.finnSisteIverksatteBehandling(fagsak.id)!!).isEqualTo(behandling2)
            assertThat(behandlingRepository.finnSisteIverksatteBehandling(fagsak2.id)!!).isEqualTo(behandling4)
        }

        @Test
        fun `skal finne siste iverksatte behandlingen som har utbetalingsoppdrag`() {
            opprettBehandling(fagsak, AVSLUTTET, LocalDateTime.now().minusDays(3))
                .medTilkjentYtelse(true)
            val behandling3 = opprettBehandling(fagsak, AVSLUTTET, LocalDateTime.now().minusDays(1))
                .medTilkjentYtelse(true)

            opprettBehandling(fagsak, AVSLUTTET).medTilkjentYtelse()
            opprettBehandling(fagsak, IVERKSETTER_VEDTAK).medTilkjentYtelse()

            assertThat(behandlingRepository.finnSisteIverksatteBehandling(fagsak.id)!!).isEqualTo(behandling3)
        }
    }

    private fun opprettBehandling(
        fagsak: Fagsak,
        behandlingStatus: BehandlingStatus,
        aktivertTidspunkt: LocalDateTime = LocalDateTime.now(),
        aktiv: Boolean = false,
    ): Behandling {
        val behandling = lagBehandling(fagsak = fagsak, status = behandlingStatus)
            .copy(
                id = 0,
                aktiv = aktiv,
                aktivertTidspunkt = aktivertTidspunkt,
            )
        val oppdaterteSteg = behandling.behandlingStegTilstand.map { it.copy(behandling = behandling) }
        behandling.behandlingStegTilstand.clear()
        behandling.behandlingStegTilstand.addAll(oppdaterteSteg)
        return behandlingRepository.saveAndFlush(behandling).let {
            behandlingRepository.finnBehandling(it.id)
        }
    }

    private fun Behandling.medTilkjentYtelse(medUtbetalingsoppdrag: Boolean = false) =
        this.also {
            val tilkjentYtelse = lagInitiellTilkjentYtelse(
                behandling = it,
                utbetalingsoppdrag = if (medUtbetalingsoppdrag) "~" else null,
            )
            tilkjentRepository.saveAndFlush(tilkjentYtelse)
        }
}
