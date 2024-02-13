package no.nav.familie.ba.sak.integrasjoner.økonomi

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagInitiellTilkjentYtelse
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.common.årMnd
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.simulering.KontrollerNyUtbetalingsgeneratorService
import no.nav.familie.ba.sak.kjerne.steg.StatusFraOppdrag
import no.nav.familie.ba.sak.kjerne.steg.StatusFraOppdragMedTask
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.task.StatusFraOppdragTask
import no.nav.familie.ba.sak.task.dto.StatusFraOppdragDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppdrag.OppdragStatus
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.unleash.UnleashService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month

class HentStatusTest {

    private val økonomiKlient = mockk<ØkonomiKlient>()

    private val beregningService: BeregningService = mockk()

    private val kontrollerNyUtbetalingsgeneratorService: KontrollerNyUtbetalingsgeneratorService = mockk()

    lateinit var statusFraOppdrag: StatusFraOppdrag

    private val tilkjentYtelseRepository = mockk<TilkjentYtelseRepository>()

    private val utbetalingsoppdragGeneratorService: UtbetalingsoppdragGeneratorService = mockk()

    private val unleashService: UnleashService = mockk()

    @BeforeEach
    fun setUp() {
        val økonomiService = ØkonomiService(

            økonomiKlient = økonomiKlient,
            beregningService = beregningService,
            tilkjentYtelseValideringService = mockk(),
            tilkjentYtelseRepository = tilkjentYtelseRepository,
            kontrollerNyUtbetalingsgeneratorService = kontrollerNyUtbetalingsgeneratorService,
            utbetalingsoppdragGeneratorService = utbetalingsoppdragGeneratorService,
            unleashService = unleashService,
        )
        statusFraOppdrag = StatusFraOppdrag(
            økonomiService = økonomiService,
            taskRepository = mockk<TaskRepositoryWrapper>().also { every { it.save(any()) } returns mockk() },
        )

        every { unleashService.isEnabled(toggleId = any(), properties = any()) } returns false
    }

    @Test
    fun `henter status fra økonomi for behandling der alle utbetalingene hører til denne behandlinga`() {
        val tilfeldigPerson = tilfeldigPerson()
        val nyBehandling = lagBehandling()
        lagTilkjentYtelse(nyBehandling, listOf(lagUtbetalingsperiode(nyBehandling)))

        every {
            økonomiKlient.hentStatus(
                match { it.behandlingsId == nyBehandling.id.toString() },
            )
        } returns OppdragStatus.KVITTERT_OK
        val andelerTilkjentYtelse = listOf(
            lagAndelTilkjentYtelse(
                årMnd("2019-04"),
                årMnd("2020-03"),
                YtelseType.ORDINÆR_BARNETRYGD,
                10,
                behandling = nyBehandling,
                person = tilfeldigPerson,
                aktør = mockk(),
                tilkjentYtelse = mockk(),
                kildeBehandlingId = null,
            ),
        )

        every { beregningService.hentAndelerTilkjentYtelseMedUtbetalingerForBehandling(any()) } returns andelerTilkjentYtelse

        val nesteSteg =
            statusFraOppdrag.utførStegOgAngiNeste(nyBehandling, statusFraOppdragMedTask(tilfeldigPerson, nyBehandling))
        assertThat(nesteSteg).isEqualTo(StegType.IVERKSETT_MOT_FAMILIE_TILBAKE)
        verify { økonomiKlient.hentStatus(match { it.behandlingsId == nyBehandling.id.toString() }) }
    }

    @Test
    fun `kan håndtere nullutbetaling uten tidligere historikk`() {
        val tilfeldigPerson = tilfeldigPerson()
        val nyBehandling = lagBehandling()
        lagTilkjentYtelse(nyBehandling, listOf())

        every {
            økonomiKlient.hentStatus(
                match { it.behandlingsId == nyBehandling.id.toString() },
            )
        } returns OppdragStatus.KVITTERT_OK
        val andelerTilkjentYtelse = listOf(
            lagAndelTilkjentYtelse(
                årMnd("2019-04"),
                årMnd("2020-03"),
                YtelseType.ORDINÆR_BARNETRYGD,
                0,
                behandling = nyBehandling,
                person = tilfeldigPerson,
                aktør = mockk(),
                tilkjentYtelse = mockk(),
                kildeBehandlingId = null,
            ),
        )

        every { beregningService.hentAndelerTilkjentYtelseMedUtbetalingerForBehandling(any()) } returns andelerTilkjentYtelse

        val nesteSteg =
            statusFraOppdrag.utførStegOgAngiNeste(nyBehandling, statusFraOppdragMedTask(tilfeldigPerson, nyBehandling))
        assertThat(nesteSteg).isEqualTo(StegType.IVERKSETT_MOT_FAMILIE_TILBAKE)
        verify(exactly = 0) { økonomiKlient.hentStatus(any()) }
    }

    private fun lagTilkjentYtelse(behandling: Behandling, utbetalingsperiode: List<Utbetalingsperiode>) {
        val nyTilkjentYtelse = lagInitiellTilkjentYtelse(
            behandling = behandling,
            utbetalingsoppdrag = objectMapper.writeValueAsString(lagUtbetalingsoppdrag(utbetalingsperiode = utbetalingsperiode)),
        )
        every { tilkjentYtelseRepository.findByBehandling(behandling.id) } returns nyTilkjentYtelse
    }

    private fun statusFraOppdragMedTask(
        tilfeldigPerson: Person,
        nyBehandling: Behandling,
    ) = StatusFraOppdragMedTask(
        statusFraOppdragDTO = StatusFraOppdragDTO(
            fagsystem = "BA",
            personIdent = tilfeldigPerson.aktør.aktivFødselsnummer(),
            aktørId = "Søker1",
            behandlingsId = nyBehandling.id,
            vedtaksId = 0L,
        ),
        task = Task(
            type = StatusFraOppdragTask.TASK_STEP_TYPE,
            payload = "",
        ),
    )

    private fun lagUtbetalingsperiode(nyBehandling: Behandling) =
        Utbetalingsperiode(
            vedtakdatoFom = LocalDate.of(
                2019,
                Month.APRIL,
                1,
            ),
            vedtakdatoTom = LocalDate.of(2020, Month.MARCH, 31),
            erEndringPåEksisterendePeriode = false,
            periodeId = 1L,
            behandlingId = nyBehandling.id,
            datoForVedtak = LocalDate.of(2020, Month.APRIL, 1),
            klassifisering = "",
            sats = BigDecimal.ONE,
            satsType = Utbetalingsperiode.SatsType.MND,
            utbetalesTil = "",
        )
}
