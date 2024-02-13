package no.nav.familie.ba.sak.task

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelseUtvidet
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdFeedClient
import no.nav.familie.ba.sak.integrasjoner.infotrygd.domene.InfotrygdVedtakFeedDto
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.YearMonth

internal class SendVedtakTilInfotrygdTaskTest {

    val infotrygdFeedClient: InfotrygdFeedClient = mockk()
    val andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService = mockk()
    private val sendVedtakTilInfotrygdTask = SendVedtakTilInfotrygdTask(
        infotrygdFeedClient,
        andelerTilkjentYtelseOgEndreteUtbetalingerService,
    )

    @Test
    fun `skal sende vedtak til infotrygd ved første gang behandling`() {
        val behandling = lagBehandling(status = BehandlingStatus.AVSLUTTET)
        val fom = YearMonth.now().minusMonths(2)
        every { andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(behandling.id) } returns lagAndelerMedFom(behandling, fom)
        val slot = slot<InfotrygdVedtakFeedDto>()
        every { infotrygdFeedClient.sendVedtakFeedTilInfotrygd(capture(slot)) } returns Unit

        sendVedtakTilInfotrygdTask.doTask(SendVedtakTilInfotrygdTask.opprettTask(behandling.fagsak.aktør.aktivFødselsnummer(), behandling.id))

        assertThat(slot.captured.fnrStoenadsmottaker).isEqualTo(behandling.fagsak.aktør.aktivFødselsnummer())
        assertThat(slot.captured.datoStartNyBa).isEqualTo(fom.atDay(1))
    }

    private fun lagAndelerMedFom(behandling: Behandling, fom: YearMonth): List<AndelTilkjentYtelseMedEndreteUtbetalinger> {
        val andel1 = lagAndelTilkjentYtelseUtvidet(
            fom.toString(),
            fom.plusYears(6).toString(),
            YtelseType.ORDINÆR_BARNETRYGD,
            behandling = behandling,
            person = lagPerson(),
            periodeIdOffset = 1,
        )

        val andel2 = lagAndelTilkjentYtelseUtvidet(
            fom.plusYears(6).toString(),
            fom.plusYears(12).toString(),
            YtelseType.ORDINÆR_BARNETRYGD,
            behandling = behandling,
            person = lagPerson(),
            periodeIdOffset = 2,
        )
        return listOf(AndelTilkjentYtelseMedEndreteUtbetalinger.utenEndringer(andel1), AndelTilkjentYtelseMedEndreteUtbetalinger.utenEndringer(andel2))
    }
}
