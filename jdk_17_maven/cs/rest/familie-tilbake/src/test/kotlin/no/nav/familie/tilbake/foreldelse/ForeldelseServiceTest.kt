package no.nav.familie.tilbake.foreldelse

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.familie.kontrakter.felles.Datoperiode
import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.api.dto.BehandlingsstegForeldelseDto
import no.nav.familie.tilbake.api.dto.ForeldelsesperiodeDto
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.foreldelse.domain.Foreldelsesvurderingstype
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.vilkårsvurdering.VilkårsvurderingRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

internal class ForeldelseServiceTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var kravgrunnlagRepository: KravgrunnlagRepository

    @Autowired
    private lateinit var foreldelsesRepository: VurdertForeldelseRepository

    @Autowired
    private lateinit var vilkårsvurderingRepository: VilkårsvurderingRepository

    @Autowired
    private lateinit var foreldelseService: ForeldelseService

    private var behandling = Testdata.behandling

    @BeforeEach
    fun init() {
        fagsakRepository.insert(Testdata.fagsak)
        behandling = behandlingRepository.insert(Testdata.behandling)

        val kravgrunnlag431 = Testdata.kravgrunnlag431
        val feilkravgrunnlagsbeløp = Testdata.feilKravgrunnlagsbeløp433
        val yteseskravgrunnlagsbeløp = Testdata.ytelKravgrunnlagsbeløp433
        val førsteKravgrunnlagsperiode = Testdata.kravgrunnlagsperiode432
            .copy(
                periode = Månedsperiode(YearMonth.of(2017, 1), YearMonth.of(2017, 1)),
                beløp = setOf(
                    feilkravgrunnlagsbeløp.copy(id = UUID.randomUUID()),
                    yteseskravgrunnlagsbeløp.copy(id = UUID.randomUUID()),
                ),
            )
        val andreKravgrunnlagsperiode = Testdata.kravgrunnlagsperiode432
            .copy(
                id = UUID.randomUUID(),
                periode = Månedsperiode(YearMonth.of(2017, 2), YearMonth.of(2017, 2)),
                beløp = setOf(
                    feilkravgrunnlagsbeløp.copy(id = UUID.randomUUID()),
                    yteseskravgrunnlagsbeløp.copy(id = UUID.randomUUID()),
                ),
            )
        kravgrunnlagRepository.insert(
            kravgrunnlag431.copy(
                perioder = setOf(
                    førsteKravgrunnlagsperiode,
                    andreKravgrunnlagsperiode,
                ),
            ),
        )
    }

    @Test
    fun `hentVurdertForeldelse skal returnere foreldelse data som skal vurderes`() {
        val vurdertForeldelseDto = foreldelseService.hentVurdertForeldelse(behandling.id)

        vurdertForeldelseDto.foreldetPerioder.size shouldBe 1
        val foreldetPeriode = vurdertForeldelseDto.foreldetPerioder[0]
        foreldetPeriode.periode.fom shouldBe LocalDate.of(2017, 1, 1)
        foreldetPeriode.periode.tom shouldBe LocalDate.of(2017, 2, 28)
        // feilutbetaltBeløp er 10000.00 i Testdata for hver periode
        foreldetPeriode.feilutbetaltBeløp shouldBe BigDecimal("20000")
        foreldetPeriode.foreldelsesvurderingstype.shouldBeNull()
        foreldetPeriode.begrunnelse.shouldBeNull()
        foreldetPeriode.foreldelsesfrist.shouldBeNull()
        foreldetPeriode.oppdagelsesdato.shouldBeNull()
    }

    @Test
    fun `hentVurdertForeldelse skal returnere allerede vurdert foreldelse data`() {
        foreldelseService
            .lagreVurdertForeldelse(
                behandling.id,
                BehandlingsstegForeldelseDto(
                    listOf(
                        lagForeldelsesperiode(
                            LocalDate.of(2017, 1, 1),
                            LocalDate.of(2017, 1, 31),
                            Foreldelsesvurderingstype
                                .FORELDET,
                        ),
                        lagForeldelsesperiode(
                            LocalDate.of(2017, 2, 1),
                            LocalDate.of(2017, 2, 28),
                            Foreldelsesvurderingstype
                                .IKKE_FORELDET,
                        ),
                    ),
                ),
            )

        val vurdertForeldelseDto = foreldelseService.hentVurdertForeldelse(behandling.id)

        vurdertForeldelseDto.foreldetPerioder.size shouldBe 2
        val førstePeriode = vurdertForeldelseDto.foreldetPerioder[0]
        førstePeriode.periode.fom shouldBe LocalDate.of(2017, 1, 1)
        førstePeriode.periode.tom shouldBe LocalDate.of(2017, 1, 31)
        // feilutbetaltBeløp er 10000.00 i Testdata for hver periode
        førstePeriode.feilutbetaltBeløp shouldBe BigDecimal("10000")
        førstePeriode.foreldelsesvurderingstype shouldBe Foreldelsesvurderingstype.FORELDET
        førstePeriode.begrunnelse shouldBe "foreldelses begrunnelse"
        førstePeriode.foreldelsesfrist shouldBe LocalDate.of(2017, 2, 28)
        førstePeriode.oppdagelsesdato.shouldBeNull()

        val andrePeriode = vurdertForeldelseDto.foreldetPerioder[1]
        andrePeriode.periode.fom shouldBe LocalDate.of(2017, 2, 1)
        andrePeriode.periode.tom shouldBe LocalDate.of(2017, 2, 28)
        // feilutbetaltBeløp er 10000.00 i Testdata for hver periode
        andrePeriode.feilutbetaltBeløp shouldBe BigDecimal("10000")
        andrePeriode.foreldelsesvurderingstype shouldBe Foreldelsesvurderingstype.IKKE_FORELDET
        andrePeriode.begrunnelse shouldBe "foreldelses begrunnelse"
        andrePeriode.foreldelsesfrist shouldBe LocalDate.of(2017, 2, 28)
        andrePeriode.oppdagelsesdato.shouldBeNull()
    }

    @Test
    fun `lagreVurdertForeldelse skal lagre foreldelses data for en gitt behandling`() {
        foreldelseService
            .lagreVurdertForeldelse(
                behandling.id,
                BehandlingsstegForeldelseDto(
                    listOf(
                        lagForeldelsesperiode(
                            LocalDate.of(2017, 1, 1),
                            LocalDate.of(2017, 1, 31),
                            Foreldelsesvurderingstype
                                .FORELDET,
                        ),
                    ),
                ),
            )

        val vurdertForeldelse = foreldelsesRepository.findByBehandlingIdAndAktivIsTrue(behandling.id)
        vurdertForeldelse.shouldNotBeNull()
        vurdertForeldelse.foreldelsesperioder.size shouldBe 1
        val vurdertForeldelsesperiode = vurdertForeldelse.foreldelsesperioder.toList()[0]
        vurdertForeldelsesperiode.begrunnelse shouldBe "foreldelses begrunnelse"
        vurdertForeldelsesperiode.foreldelsesvurderingstype shouldBe Foreldelsesvurderingstype.FORELDET
        vurdertForeldelsesperiode.foreldelsesfrist shouldBe LocalDate.of(2017, 2, 28)
        vurdertForeldelsesperiode.oppdagelsesdato.shouldBeNull()
        vurdertForeldelsesperiode.periode shouldBe Månedsperiode(YearMonth.of(2017, 1), YearMonth.of(2017, 1))
    }

    @Test
    fun `lagreVurdertForeldelse skal ikke lagre foreldelses data når periode ikke starter med første dato`() {
        val foreldelsesperiode = lagForeldelsesperiode(
            LocalDate.of(2017, 1, 10),
            LocalDate.of(2017, 1, 31),
            Foreldelsesvurderingstype.FORELDET,
        )
        val exception = shouldThrow<RuntimeException> {
            foreldelseService
                .lagreVurdertForeldelse(
                    behandling.id,
                    BehandlingsstegForeldelseDto(listOf(foreldelsesperiode)),
                )
        }
        exception.message shouldBe "Periode med ${foreldelsesperiode.periode} er ikke i hele måneder"
    }

    @Test
    fun `lagreVurdertForeldelse skal ikke lagre foreldelses data når periode ikke slutter med siste dato`() {
        val foreldelsesperiode = lagForeldelsesperiode(
            LocalDate.of(2017, 1, 1),
            LocalDate.of(2017, 1, 27),
            Foreldelsesvurderingstype.FORELDET,
        )
        val exception = shouldThrow<RuntimeException> {
            foreldelseService
                .lagreVurdertForeldelse(
                    behandling.id,
                    BehandlingsstegForeldelseDto(listOf(foreldelsesperiode)),
                )
        }
        exception.message shouldBe "Periode med ${foreldelsesperiode.periode} er ikke i hele måneder"
    }

    @Test
    fun `lagreVurdertForeldelse skal nullstille forrige vurdert vilkårsvurdering når det er endring i foreldesesperiode`() {
        val forrigeForeldelsesperiode = lagForeldelsesperiode(
            LocalDate.of(2017, 1, 1),
            LocalDate.of(2017, 4, 30),
            Foreldelsesvurderingstype.IKKE_FORELDET,
        )
        foreldelseService.lagreVurdertForeldelse(behandling.id, BehandlingsstegForeldelseDto(listOf(forrigeForeldelsesperiode)))
        vilkårsvurderingRepository.insert(Testdata.vilkårsvurdering)

        vilkårsvurderingRepository.findByBehandlingIdAndAktivIsTrue(behandling.id).shouldNotBeNull()

        val nyForeldelsesperiode1 = lagForeldelsesperiode(
            LocalDate.of(2017, 1, 1),
            LocalDate.of(2017, 2, 28),
            Foreldelsesvurderingstype.IKKE_FORELDET,
        )
        val nyForeldelsesperiode2 = lagForeldelsesperiode(
            LocalDate.of(2017, 3, 1),
            LocalDate.of(2017, 4, 30),
            Foreldelsesvurderingstype.IKKE_FORELDET,
        )
        foreldelseService.lagreVurdertForeldelse(
            behandling.id,
            BehandlingsstegForeldelseDto(
                listOf(
                    nyForeldelsesperiode1,
                    nyForeldelsesperiode2,
                ),
            ),
        )
        vilkårsvurderingRepository.findByBehandlingIdAndAktivIsTrue(behandling.id).shouldBeNull()
    }

    @Test
    fun `lagreVurdertForeldelse skal ikke nullstille vurdert vilkårsvurdering når det er ingen endring i foreldesesperiode`() {
        val forrigeForeldelsesperiode = lagForeldelsesperiode(
            LocalDate.of(2017, 1, 1),
            LocalDate.of(2017, 4, 30),
            Foreldelsesvurderingstype.IKKE_FORELDET,
        )
        foreldelseService.lagreVurdertForeldelse(behandling.id, BehandlingsstegForeldelseDto(listOf(forrigeForeldelsesperiode)))
        vilkårsvurderingRepository.insert(Testdata.vilkårsvurdering)

        vilkårsvurderingRepository.findByBehandlingIdAndAktivIsTrue(behandling.id).shouldNotBeNull()

        val nyForeldelsesperiode = lagForeldelsesperiode(
            LocalDate.of(2017, 1, 1),
            LocalDate.of(2017, 4, 30),
            Foreldelsesvurderingstype.FORELDET,
        )
        foreldelseService.lagreVurdertForeldelse(behandling.id, BehandlingsstegForeldelseDto(listOf(nyForeldelsesperiode)))
        vilkårsvurderingRepository.findByBehandlingIdAndAktivIsTrue(behandling.id).shouldNotBeNull()
    }

    private fun lagForeldelsesperiode(
        fom: LocalDate,
        tom: LocalDate,
        foreldelsesvurderingstype: Foreldelsesvurderingstype,
    ): ForeldelsesperiodeDto {
        return ForeldelsesperiodeDto(
            periode = Datoperiode(fom, tom),
            begrunnelse = "foreldelses begrunnelse",
            foreldelsesvurderingstype = foreldelsesvurderingstype,
            foreldelsesfrist = LocalDate.of(2017, 2, 28),
        )
    }
}
