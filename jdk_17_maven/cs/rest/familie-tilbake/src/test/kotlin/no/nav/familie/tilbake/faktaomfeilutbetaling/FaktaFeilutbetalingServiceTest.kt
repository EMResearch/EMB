package no.nav.familie.tilbake.faktaomfeilutbetaling

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.api.dto.FaktaFeilutbetalingDto
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.FaktaFeilutbetaling
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.FaktaFeilutbetalingsperiode
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsestype
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsesundertype
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlagsperiode432
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.YearMonth
import java.util.UUID

internal class FaktaFeilutbetalingServiceTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var faktaFeilutbetalingRepository: FaktaFeilutbetalingRepository

    @Autowired
    private lateinit var kravgrunnlagRepository: KravgrunnlagRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var faktaFeilutbetalingService: FaktaFeilutbetalingService

    private val behandling = Testdata.behandling
    private val periode = Månedsperiode(
        fom = YearMonth.now().minusMonths(2),
        tom = YearMonth.now(),
    )

    @BeforeEach
    fun init() {
        fagsakRepository.insert(Testdata.fagsak)
        behandlingRepository.insert(behandling)
        val kravgrunnlag = Testdata.kravgrunnlag431
            .copy(
                perioder = setOf(
                    Kravgrunnlagsperiode432(
                        periode = periode,
                        beløp = setOf(
                            Testdata.feilKravgrunnlagsbeløp433,
                            Testdata.ytelKravgrunnlagsbeløp433,
                        ),
                        månedligSkattebeløp = BigDecimal("123.11"),
                    ),
                ),
            )
        kravgrunnlagRepository.insert(kravgrunnlag)
    }

    @Test
    fun `hentFaktaomfeilutbetaling skal hente fakta om feilutbetaling for en gitt behandling`() {
        lagFaktaomfeilutbetaling(behandling.id)

        val faktaFeilutbetalingDto = faktaFeilutbetalingService.hentFaktaomfeilutbetaling(behandlingId = behandling.id)

        faktaFeilutbetalingDto.begrunnelse shouldBe "Fakta begrunnelse"
        val varsletData = behandling.aktivtVarsel
        faktaFeilutbetalingDto.varsletBeløp shouldBe varsletData?.varselbeløp

        assertFagsystemsbehandling(faktaFeilutbetalingDto, behandling)
        assertFeilutbetaltePerioder(
            faktaFeilutbetalingDto = faktaFeilutbetalingDto,
            hendelsestype = Hendelsestype.ANNET,
            hendelsesundertype = Hendelsesundertype.ANNET_FRITEKST,
        )
    }

    @Test
    fun `hentFaktaomfeilutbetaling skal hente fakta om feilutbetaling for behandling uten varsel`() {
        val lagretBehandling = behandlingRepository.findByIdOrThrow(behandling.id)
        val oppdatertBehandling = lagretBehandling.copy(varsler = emptySet())
        behandlingRepository.update(oppdatertBehandling)
        lagFaktaomfeilutbetaling(behandlingId = oppdatertBehandling.id)

        val faktaFeilutbetalingDto = faktaFeilutbetalingService.hentFaktaomfeilutbetaling(behandlingId = oppdatertBehandling.id)

        faktaFeilutbetalingDto.begrunnelse shouldBe "Fakta begrunnelse"
        faktaFeilutbetalingDto.varsletBeløp.shouldBeNull()
        assertFagsystemsbehandling(faktaFeilutbetalingDto, behandling)
        assertFeilutbetaltePerioder(
            faktaFeilutbetalingDto = faktaFeilutbetalingDto,
            hendelsestype = Hendelsestype.ANNET,
            hendelsesundertype = Hendelsesundertype.ANNET_FRITEKST,
        )
    }

    @Test
    fun `hentFaktaomfeilutbetaling skal hente fakta om feilutbetaling første gang for en gitt behandling`() {
        val faktaFeilutbetalingDto = faktaFeilutbetalingService.hentFaktaomfeilutbetaling(behandlingId = behandling.id)

        faktaFeilutbetalingDto.begrunnelse shouldBe ""
        faktaFeilutbetalingDto.varsletBeløp shouldBe behandling.aktivtVarsel?.varselbeløp
        assertFagsystemsbehandling(faktaFeilutbetalingDto, behandling)
        assertFeilutbetaltePerioder(
            faktaFeilutbetalingDto = faktaFeilutbetalingDto,
            hendelsestype = null,
            hendelsesundertype = null,
        )
    }

    private fun lagFaktaomfeilutbetaling(behandlingId: UUID) {
        val faktaPerioder = FaktaFeilutbetalingsperiode(
            periode = periode,
            hendelsestype = Hendelsestype.ANNET,
            hendelsesundertype = Hendelsesundertype.ANNET_FRITEKST,
        )
        val faktaFeilutbetaling = FaktaFeilutbetaling(
            behandlingId = behandlingId,
            begrunnelse = "Fakta begrunnelse",
            perioder = setOf(faktaPerioder),
        )
        faktaFeilutbetalingRepository.insert(faktaFeilutbetaling)
    }

    private fun assertFagsystemsbehandling(
        faktaFeilutbetalingDto: FaktaFeilutbetalingDto,
        behandling: Behandling,
    ) {
        val fagsystemsbehandling = behandling.aktivFagsystemsbehandling
        val faktainfo = faktaFeilutbetalingDto.faktainfo
        faktainfo.tilbakekrevingsvalg shouldBe fagsystemsbehandling.tilbakekrevingsvalg
        faktaFeilutbetalingDto.revurderingsvedtaksdato shouldBe fagsystemsbehandling.revurderingsvedtaksdato
        faktainfo.revurderingsresultat shouldBe fagsystemsbehandling.resultat
        faktainfo.revurderingsårsak shouldBe fagsystemsbehandling.årsak
        faktainfo.konsekvensForYtelser.shouldBeEmpty()
    }

    private fun assertFeilutbetaltePerioder(
        faktaFeilutbetalingDto: FaktaFeilutbetalingDto,
        hendelsestype: Hendelsestype?,
        hendelsesundertype: Hendelsesundertype?,
    ) {
        faktaFeilutbetalingDto.totalFeilutbetaltPeriode shouldBe periode.toDatoperiode()
        faktaFeilutbetalingDto.totaltFeilutbetaltBeløp shouldBe BigDecimal.valueOf(1000000, 2)

        faktaFeilutbetalingDto.feilutbetaltePerioder.size shouldBe 1
        val feilutbetaltePeriode = faktaFeilutbetalingDto.feilutbetaltePerioder.first()
        feilutbetaltePeriode.hendelsestype shouldBe hendelsestype
        feilutbetaltePeriode.hendelsesundertype shouldBe hendelsesundertype
        feilutbetaltePeriode.periode shouldBe periode.toDatoperiode()
    }
}
