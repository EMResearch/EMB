package no.nav.familie.ba.sak.kjerne.behandling

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdService
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.behandling.behandlingstema.BehandlingstemaService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingMigreringsinfoRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingSøknadsinfoService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.simulering.lagBehandling
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakRepository
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.statistikk.saksstatistikk.SaksstatistikkEventPublisher
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.YearMonth

@ExtendWith(MockKExtension::class)
class BehandlingServiceEnhetstest {

    @MockK
    private lateinit var behandlingHentOgPersisterService: BehandlingHentOgPersisterService

    @MockK
    private lateinit var behandlingstemaService: BehandlingstemaService

    @MockK
    private lateinit var behandlingSøknadsinfoService: BehandlingSøknadsinfoService

    @MockK
    private lateinit var behandlingMigreringsinfoRepository: BehandlingMigreringsinfoRepository

    @MockK
    private lateinit var behandlingMetrikker: BehandlingMetrikker

    @MockK
    private lateinit var saksstatistikkEventPublisher: SaksstatistikkEventPublisher

    @MockK
    private lateinit var fagsakRepository: FagsakRepository

    @MockK
    private lateinit var vedtakRepository: VedtakRepository

    @MockK
    private lateinit var andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository

    @MockK
    private lateinit var loggService: LoggService

    @MockK
    private lateinit var arbeidsfordelingService: ArbeidsfordelingService

    @MockK
    private lateinit var infotrygdService: InfotrygdService

    @MockK
    private lateinit var vedtaksperiodeService: VedtaksperiodeService

    @MockK
    private lateinit var taskRepository: TaskRepositoryWrapper

    @MockK
    private lateinit var vilkårsvurderingService: VilkårsvurderingService

    @InjectMockKs
    private lateinit var behandlingService: BehandlingService

    @Test
    fun `erLøpende - skal returnere true dersom det finnes andeler i en behandling hvor tom er etter YearMonth now`() {
        val behandling = lagBehandling()

        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(any()) } returns listOf(
            lagAndelTilkjentYtelse(YearMonth.now().minusYears(1), YearMonth.now().minusMonths(6)),
            lagAndelTilkjentYtelse(YearMonth.now().minusMonths(6), YearMonth.now().minusMonths(3)),
            lagAndelTilkjentYtelse(YearMonth.now().minusMonths(3), YearMonth.now().plusMonths(3)),
        )
        assertThat(behandlingService.erLøpende(behandling)).isTrue
    }

    @Test
    fun `erLøpende - skal returnere false dersom det finnes andeler i en behandling hvor tom er det samme som YearMonth now`() {
        val behandling = lagBehandling()

        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(any()) } returns listOf(
            lagAndelTilkjentYtelse(YearMonth.now().minusYears(1), YearMonth.now().minusMonths(6)),
            lagAndelTilkjentYtelse(YearMonth.now().minusMonths(6), YearMonth.now().minusMonths(3)),
            lagAndelTilkjentYtelse(YearMonth.now().minusMonths(3), YearMonth.now()),
        )
        assertThat(behandlingService.erLøpende(behandling)).isFalse
    }

    @Test
    fun `erLøpende - skal returnere false dersom alle andeler i en behandling har tom før YearMonth now`() {
        val behandling = lagBehandling()

        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(any()) } returns listOf(
            lagAndelTilkjentYtelse(YearMonth.now().minusYears(1), YearMonth.now().minusMonths(6)),
            lagAndelTilkjentYtelse(YearMonth.now().minusMonths(6), YearMonth.now().minusMonths(3)),
            lagAndelTilkjentYtelse(YearMonth.now().minusMonths(3), YearMonth.now().minusMonths(1)),
        )
        assertThat(behandlingService.erLøpende(behandling)).isFalse
    }
}
