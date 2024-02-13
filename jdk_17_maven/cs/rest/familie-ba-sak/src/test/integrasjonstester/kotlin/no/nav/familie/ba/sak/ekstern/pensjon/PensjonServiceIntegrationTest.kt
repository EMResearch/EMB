package no.nav.familie.ba.sak.ekstern.pensjon

import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagInitiellTilkjentYtelse
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.common.årMnd
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.DatabaseCleanupService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class PensjonServiceIntegrationTest : AbstractSpringIntegrationTest() {
    @Autowired
    lateinit var databaseCleanupService: DatabaseCleanupService

    @Autowired
    lateinit var fagsakRepository: FagsakRepository

    @Autowired
    lateinit var personidentService: PersonidentService

    @Autowired
    lateinit var behandlingService: BehandlingService

    @Autowired
    lateinit var fagsakService: FagsakService

    @Autowired
    lateinit var pensjonService: PensjonService

    @Autowired
    lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @Autowired
    lateinit var behandlingHentOgPersisterService: BehandlingHentOgPersisterService

    @Test
    fun `skal finne en relaterte fagsaker per barn`() {
        val søker = tilfeldigPerson()
        val barn1 = tilfeldigPerson()
        val søkerAktør = personidentService.hentOgLagreAktør(søker.aktør.aktivFødselsnummer(), true)
        val barnAktør = personidentService.hentOgLagreAktør(barn1.aktør.aktivFødselsnummer(), true)

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søker.aktør.aktivFødselsnummer())
        with(behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))) {
            val behandling = this
            with(lagInitiellTilkjentYtelse(behandling, "utbetalingsoppdrag")) {
                val andel = lagAndelTilkjentYtelse(
                    årMnd("2019-04"),
                    årMnd("2023-03"),
                    no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType.ORDINÆR_BARNETRYGD,
                    660,
                    behandling,
                    person = barn1,
                    aktør = barnAktør,
                    tilkjentYtelse = this,
                )
                andelerTilkjentYtelse.add(andel)
                tilkjentYtelseRepository.save(this)
            }
            avsluttOgLagreBehandling(behandling)
        }

        val fagsak2 = fagsakService.hentEllerOpprettFagsakForPersonIdent(barn1.aktør.aktivFødselsnummer())
        with(behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak2))) {
            val behandling = this
            with(lagInitiellTilkjentYtelse(behandling, "utbetalingsoppdrag")) {
                val andel = lagAndelTilkjentYtelse(
                    årMnd("2019-04"),
                    årMnd("2023-03"),
                    no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType.ORDINÆR_BARNETRYGD,
                    660,
                    behandling,
                    person = barn1,
                    aktør = barnAktør,
                    tilkjentYtelse = this,
                )
                andelerTilkjentYtelse.add(andel)
                tilkjentYtelseRepository.save(this)
            }
            avsluttOgLagreBehandling(behandling)
        }

        val barnetrygdTilPensjon = pensjonService.hentBarnetrygd(søkerAktør.aktivFødselsnummer(), LocalDate.of(2023, 1, 1))
        assertThat(barnetrygdTilPensjon).hasSize(2)
    }

    private fun avsluttOgLagreBehandling(behandling: Behandling) {
        behandling.status = BehandlingStatus.AVSLUTTET
        behandling.leggTilBehandlingStegTilstand(StegType.BEHANDLING_AVSLUTTET)
        behandlingHentOgPersisterService.lagreEllerOppdater(behandling, false)
    }
}
