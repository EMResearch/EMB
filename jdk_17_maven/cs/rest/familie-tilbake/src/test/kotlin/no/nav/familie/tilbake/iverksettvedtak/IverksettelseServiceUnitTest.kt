package no.nav.familie.tilbake.iverksettvedtak

import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.BehandlingsvedtakService
import no.nav.familie.tilbake.beregning.TilbakekrevingsberegningService
import no.nav.familie.tilbake.beregning.modell.Beregningsresultat
import no.nav.familie.tilbake.beregning.modell.Beregningsresultatsperiode
import no.nav.familie.tilbake.beregning.modell.Vedtaksresultat
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.FeatureToggleService
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.integration.økonomi.OppdragClient
import no.nav.familie.tilbake.iverksettvedtak.domain.KodeResultat.DELVIS_TILBAKEKREVING
import no.nav.familie.tilbake.iverksettvedtak.domain.KodeResultat.FULL_TILBAKEKREVING
import no.nav.familie.tilbake.iverksettvedtak.domain.Tilbakekrevingsbeløp
import no.nav.familie.tilbake.iverksettvedtak.domain.Tilbakekrevingsperiode
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.kravgrunnlag.domain.Klassekode
import no.nav.familie.tilbake.kravgrunnlag.domain.Klassetype
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlagsperiode432
import no.nav.okonomi.tilbakekrevingservice.TilbakekrevingsvedtakRequest
import no.nav.okonomi.tilbakekrevingservice.TilbakekrevingsvedtakResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.YearMonth

class IverksettelseServiceUnitTest {

    val behandlingRepository = mockk<BehandlingRepository>()
    val kravgrunnlagRepository = mockk<KravgrunnlagRepository>()
    val økonomiXmlSendtRepository = mockk<ØkonomiXmlSendtRepository>()
    val tilbakekrevingsvedtakBeregningService = mockk<TilbakekrevingsvedtakBeregningService>()
    val beregningService = mockk<TilbakekrevingsberegningService>()
    val behandlingVedtakService = mockk<BehandlingsvedtakService>()
    val oppdragClient = mockk<OppdragClient>()
    val featureToggleService = mockk<FeatureToggleService>()

    val behandling = Testdata.behandling

    val iverksettelseService = IverksettelseService(
        behandlingRepository,
        kravgrunnlagRepository,
        økonomiXmlSendtRepository,
        tilbakekrevingsvedtakBeregningService,
        beregningService,
        behandlingVedtakService,
        oppdragClient,
        featureToggleService,
    )

    @Test
    fun `skal endre fra delvis til full tilbakekreving dersom utestående beløp er 0 og featuretoggle skrudd på`() {
        val requestSlot = settOppMockDataSomGirUriktigDelvisTilbakekrevingForEnPeriode()

        every { featureToggleService.isEnabled(any()) } returns true
        iverksettelseService.sendIverksettVedtak(behandling.id)

        val tilbakekrevingsperioder = requestSlot.captured.tilbakekrevingsvedtak.tilbakekrevingsperiode

        assertThat(tilbakekrevingsperioder).hasSize(2)
        assertThat(tilbakekrevingsperioder.first().tilbakekrevingsbelop.first().kodeResultat).isEqualTo(DELVIS_TILBAKEKREVING.kode)
        assertThat(tilbakekrevingsperioder.last().tilbakekrevingsbelop.first().kodeResultat).isEqualTo(FULL_TILBAKEKREVING.kode)
    }

    @Test
    fun `skal beholde delvis tilbakekreving selv om utestående beløp er 0 når featuretoggle ikke er på`() {
        val requestSlot = settOppMockDataSomGirUriktigDelvisTilbakekrevingForEnPeriode()

        every { featureToggleService.isEnabled(any()) } returns false
        iverksettelseService.sendIverksettVedtak(behandling.id)

        val tilbakekrevingsperioder = requestSlot.captured.tilbakekrevingsvedtak.tilbakekrevingsperiode

        assertThat(tilbakekrevingsperioder).hasSize(2)
        assertThat(tilbakekrevingsperioder.first().tilbakekrevingsbelop.first().kodeResultat).isEqualTo(DELVIS_TILBAKEKREVING.kode)
        assertThat(tilbakekrevingsperioder.last().tilbakekrevingsbelop.first().kodeResultat).isEqualTo(DELVIS_TILBAKEKREVING.kode)
    }

    private fun settOppMockDataSomGirUriktigDelvisTilbakekrevingForEnPeriode(): CapturingSlot<TilbakekrevingsvedtakRequest> {
        val requestSlot = slot<TilbakekrevingsvedtakRequest>()
        val tilbakekrevingsperioder = lagTilbakekrevingsperiode()
        val kravgrunnlag = lagKravgrunnlag()
        every { behandlingRepository.findByIdOrThrow(any()) } returns behandling
        every { kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(any()) } returns kravgrunnlag
        every { tilbakekrevingsvedtakBeregningService.beregnVedtaksperioder(any(), any()) } returns tilbakekrevingsperioder
        every { økonomiXmlSendtRepository.insert(any()) } returns Testdata.økonomiXmlSendt
        every { oppdragClient.iverksettVedtak(any(), capture(requestSlot)) } returns TilbakekrevingsvedtakResponse()
        every { økonomiXmlSendtRepository.findByIdOrThrow(any()) } returns Testdata.økonomiXmlSendt
        every { økonomiXmlSendtRepository.update(any()) } returns Testdata.økonomiXmlSendt
        every { beregningService.beregn(any()) } returns lagBeregningsresultat()
        every { behandlingVedtakService.oppdaterBehandlingsvedtak(any(), any()) } returns behandling
        return requestSlot
    }

    private fun lagBeregningsresultat() = Beregningsresultat(
        beregningsresultatsperioder = listOf(
            Beregningsresultatsperiode(
                periode = Månedsperiode(YearMonth.now().minusMonths(2), YearMonth.now().minusMonths(1)),
                vurdering = null,
                feilutbetaltBeløp = BigDecimal(10000),
                andelAvBeløp = BigDecimal(9983).divide(BigDecimal(10000), 2, RoundingMode.HALF_UP),
                renteprosent = null,
                manueltSattTilbakekrevingsbeløp = BigDecimal(9983),
                tilbakekrevingsbeløpUtenRenter = BigDecimal(9983),
                rentebeløp = BigDecimal.ZERO,
                tilbakekrevingsbeløp = BigDecimal(9983),
                skattebeløp = BigDecimal.ZERO,
                tilbakekrevingsbeløpEtterSkatt = BigDecimal(9983),
                utbetaltYtelsesbeløp = BigDecimal.ZERO,
                riktigYtelsesbeløp = BigDecimal.ZERO,
            ),
            Beregningsresultatsperiode(
                periode = Månedsperiode(YearMonth.now().minusMonths(1), YearMonth.now()),
                vurdering = null,
                feilutbetaltBeløp = BigDecimal(47),
                andelAvBeløp = BigDecimal.ONE,
                renteprosent = null,
                manueltSattTilbakekrevingsbeløp = BigDecimal(47),
                tilbakekrevingsbeløpUtenRenter = BigDecimal(47),
                rentebeløp = BigDecimal.ZERO,
                tilbakekrevingsbeløp = BigDecimal(47),
                skattebeløp = BigDecimal.ZERO,
                tilbakekrevingsbeløpEtterSkatt = BigDecimal(47),
                utbetaltYtelsesbeløp = BigDecimal.ZERO,
                riktigYtelsesbeløp = BigDecimal.ZERO,
            ),
        ),
        vedtaksresultat = Vedtaksresultat.DELVIS_TILBAKEBETALING,
    )

    private fun lagTilbakekrevingsperiode() =
        listOf(
            Tilbakekrevingsperiode(
                periode = Månedsperiode(YearMonth.now().minusMonths(2), YearMonth.now().minusMonths(1)),
                renter = BigDecimal.ZERO,
                beløp = listOf(
                    Tilbakekrevingsbeløp(
                        klassetype = Klassetype.YTEL,
                        klassekode = Klassekode.EFOG,
                        nyttBeløp = BigDecimal.ZERO,
                        utbetaltBeløp = BigDecimal.ZERO,
                        tilbakekrevesBeløp = BigDecimal(9983),
                        uinnkrevdBeløp = BigDecimal(17),
                        skattBeløp = BigDecimal.ZERO,
                        kodeResultat = DELVIS_TILBAKEKREVING,
                    ),
                ),
            ),
            Tilbakekrevingsperiode(
                periode = Månedsperiode(YearMonth.now().minusMonths(1), YearMonth.now()),
                renter = BigDecimal.ZERO,
                beløp = listOf(
                    Tilbakekrevingsbeløp(
                        klassetype = Klassetype.YTEL,
                        klassekode = Klassekode.EFOG,
                        nyttBeløp = BigDecimal.ZERO,
                        utbetaltBeløp = BigDecimal.ZERO,
                        tilbakekrevesBeløp = BigDecimal(47),
                        uinnkrevdBeløp = BigDecimal(0),
                        skattBeløp = BigDecimal.ZERO,
                        kodeResultat = DELVIS_TILBAKEKREVING,
                    ),
                ),
            ),
        )

    private fun lagKravgrunnlag() = Testdata.kravgrunnlag431.copy(
        perioder = setOf(
            Kravgrunnlagsperiode432(
                periode = Månedsperiode(
                    YearMonth.now().minusMonths(2),
                    YearMonth.now().minusMonths(1),
                ),
                beløp = setOf(
                    Testdata.feilKravgrunnlagsbeløp433,
                    Testdata.ytelKravgrunnlagsbeløp433,
                ),
                månedligSkattebeløp = BigDecimal.ZERO,
            ),
            Kravgrunnlagsperiode432(
                periode = Månedsperiode(
                    YearMonth.now().minusMonths(1),
                    YearMonth.now(),
                ),
                beløp = setOf(
                    Testdata.feilKravgrunnlagsbeløp433,
                    Testdata.ytelKravgrunnlagsbeløp433.copy(
                        tilbakekrevesBeløp = BigDecimal(47),
                        opprinneligUtbetalingsbeløp = BigDecimal(47),
                    ),
                ),
                månedligSkattebeløp = BigDecimal.ZERO,
            ),
        ),
    )
}
