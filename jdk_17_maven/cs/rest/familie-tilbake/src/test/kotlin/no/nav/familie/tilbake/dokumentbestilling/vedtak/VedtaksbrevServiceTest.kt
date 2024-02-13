package no.nav.familie.tilbake.dokumentbestilling.vedtak

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import no.nav.familie.kontrakter.felles.Datoperiode
import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.api.dto.FritekstavsnittDto
import no.nav.familie.tilbake.api.dto.HentForhåndvisningVedtaksbrevPdfDto
import no.nav.familie.tilbake.api.dto.PeriodeMedTekstDto
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Behandlingsårsak
import no.nav.familie.tilbake.behandling.domain.Behandlingsårsakstype
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.behandling.domain.Verge
import no.nav.familie.tilbake.config.FeatureToggleService
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.dokumentbestilling.DistribusjonshåndteringService
import no.nav.familie.tilbake.dokumentbestilling.felles.Adresseinfo
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmottager
import no.nav.familie.tilbake.dokumentbestilling.felles.EksterneDataForBrevService
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevtype
import no.nav.familie.tilbake.dokumentbestilling.felles.pdf.Brevdata
import no.nav.familie.tilbake.dokumentbestilling.felles.pdf.PdfBrevService
import no.nav.familie.tilbake.faktaomfeilutbetaling.FaktaFeilutbetalingRepository
import no.nav.familie.tilbake.faktaomfeilutbetaling.FaktaFeilutbetalingService
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.FaktaFeilutbetaling
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.FaktaFeilutbetalingsperiode
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsestype
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsesundertype
import no.nav.familie.tilbake.integration.pdl.internal.Personinfo
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.pdfgen.validering.PdfaValidator
import no.nav.familie.tilbake.vilkårsvurdering.VilkårsvurderingRepository
import no.nav.familie.tilbake.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.tilbake.vilkårsvurdering.domain.Aktsomhet
import no.nav.familie.tilbake.vilkårsvurdering.domain.SærligGrunn
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurdering
import no.nav.familie.tilbake.vilkårsvurdering.domain.VilkårsvurderingAktsomhet
import no.nav.familie.tilbake.vilkårsvurdering.domain.VilkårsvurderingSærligGrunn
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurderingsperiode
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurderingsresultat
import org.apache.commons.lang3.RandomStringUtils
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

internal class VedtaksbrevServiceTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var vedtaksbrevgeneratorService: VedtaksbrevgeneratorService

    @Autowired
    private lateinit var vedtaksbrevgrunnlagService: VedtaksbrevgunnlagService

    @Autowired
    private lateinit var faktaRepository: FaktaFeilutbetalingRepository

    @Autowired
    private lateinit var vilkårsvurderingRepository: VilkårsvurderingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var vedtaksbrevsoppsummeringRepository: VedtaksbrevsoppsummeringRepository

    @Autowired
    private lateinit var vedtaksbrevsperiodeRepository: VedtaksbrevsperiodeRepository

    @Autowired
    private lateinit var vilkårsvurderingService: VilkårsvurderingService

    @Autowired
    private lateinit var faktaFeilutbetalingService: FaktaFeilutbetalingService

    @Autowired
    private lateinit var pdfBrevService: PdfBrevService

    private lateinit var spyPdfBrevService: PdfBrevService

    @Autowired
    private lateinit var kravgrunnlagRepository: KravgrunnlagRepository

    private val eksterneDataForBrevService: EksterneDataForBrevService = mockk()

    private lateinit var vedtaksbrevService: VedtaksbrevService

    @Autowired
    private lateinit var sendBrevService: DistribusjonshåndteringService

    @Autowired
    private lateinit var featureToggleService: FeatureToggleService

    private lateinit var behandling: Behandling
    private lateinit var fagsak: Fagsak

    @BeforeEach
    fun init() {
        spyPdfBrevService = spyk(pdfBrevService)
        vedtaksbrevService = VedtaksbrevService(
            behandlingRepository,
            vedtaksbrevgeneratorService,
            vedtaksbrevgrunnlagService,
            faktaRepository,
            vilkårsvurderingRepository,
            fagsakRepository,
            vedtaksbrevsoppsummeringRepository,
            vedtaksbrevsperiodeRepository,
            spyPdfBrevService,
            sendBrevService,
            featureToggleService,
        )

        fagsak = fagsakRepository.insert(Testdata.fagsak)
        behandling = behandlingRepository.insert(Testdata.behandling)
        val kravgrunnlagsperiode432 = Testdata.kravgrunnlag431.perioder.first().copy(periode = Månedsperiode(YearMonth.of(2023, 3), YearMonth.of(2023, 4)))
        kravgrunnlagRepository.insert(Testdata.kravgrunnlag431.copy(perioder = setOf(kravgrunnlagsperiode432)))
        vilkårsvurderingRepository.insert(
            Testdata.vilkårsvurdering
                .copy(perioder = setOf(Testdata.vilkårsperiode.copy(periode = Månedsperiode(YearMonth.of(2023, 3), YearMonth.of(2023, 4)), godTro = null))),
        )
        faktaRepository.insert(
            Testdata.faktaFeilutbetaling.copy(
                perioder = setOf(
                    FaktaFeilutbetalingsperiode(
                        periode = Månedsperiode("2020-04" to "2022-08"),
                        hendelsestype = Hendelsestype.ANNET,
                        hendelsesundertype = Hendelsesundertype.ANNET_FRITEKST,
                    ),
                    FaktaFeilutbetalingsperiode(
                        periode = Månedsperiode("2023-03" to "2023-04"),
                        hendelsestype = Hendelsestype.ANNET,
                        hendelsesundertype = Hendelsesundertype.ANNET_FRITEKST,
                    ),
                ),
            ),
        )

        val personinfo = Personinfo("28056325874", LocalDate.now(), "Fiona")

        every { eksterneDataForBrevService.hentPerson(Testdata.fagsak.bruker.ident, any()) }.returns(personinfo)
        every { eksterneDataForBrevService.hentSaksbehandlernavn(Testdata.behandling.ansvarligSaksbehandler) }
            .returns("Ansvarlig O'Saksbehandler")
        every { eksterneDataForBrevService.hentSaksbehandlernavn(Testdata.behandling.ansvarligBeslutter!!) }
            .returns("Ansvarlig O'Beslutter")
        every {
            eksterneDataForBrevService.hentAdresse(any(), any(), any<Verge>(), any())
        }.returns(Adresseinfo("12345678901", "Test"))
    }

    @Test
    fun `sendVedtaksbrev skal kalle pfdBrevService med behandling, fagsak og genererte brevdata`() {
        val behandlingSlot = slot<Behandling>()
        val fagsakSlot = slot<Fagsak>()
        val brevtypeSlot = slot<Brevtype>()
        val brevdataSlot = slot<Brevdata>()

        vedtaksbrevService.sendVedtaksbrev(Testdata.behandling, Brevmottager.BRUKER)

        verify {
            spyPdfBrevService.sendBrev(
                capture(behandlingSlot),
                capture(fagsakSlot),
                capture(brevtypeSlot),
                capture(brevdataSlot),
            )
        }
        behandlingSlot.captured shouldBe Testdata.behandling
        fagsakSlot.captured shouldBe fagsak
        brevtypeSlot.captured shouldBe Brevtype.VEDTAK
        brevdataSlot.captured.overskrift shouldBe "Du må betale tilbake barnetrygden"
    }

    @Test
    fun `hentForhåndsvisningVedtaksbrevMedVedleggSomPdf skal generere en gyldig pdf`() {
        val dto = HentForhåndvisningVedtaksbrevPdfDto(
            Testdata.behandling.id,
            "Dette er en stor og gild oppsummeringstekst",
            listOf(
                PeriodeMedTekstDto(
                    Datoperiode(
                        LocalDate.now().minusDays(1),
                        LocalDate.now(),
                    ),
                    "Friktekst om fakta",
                    "Friktekst om foreldelse",
                    "Friktekst om vilkår",
                    """Friktekst & > < ' "særligeGrunner""",
                    "Friktekst om særligeGrunnerAnnet",
                ),
            ),
        )

        val bytes = vedtaksbrevService.hentForhåndsvisningVedtaksbrevMedVedleggSomPdf(dto)
        //   File("test.pdf").writeBytes(bytes)

        PdfaValidator.validatePdf(bytes)
    }

    @Test
    fun `hentForhåndsvisningVedtaksbrevMedVedleggSomPdf skal generere en gyldig pdf med xml-spesialtegn`() {
        val bytes = vedtaksbrevService.hentForhåndsvisningVedtaksbrevMedVedleggSomPdf(forhåndvisningDto)

//        File("test.pdf").writeBytes(bytes)
        PdfaValidator.validatePdf(bytes)
    }

    @Test
    fun `hentForhåndsvisningVedtaksbrevSomTekst genererer avsnitt med tekst for forhåndsvisning av vedtaksbrev`() {
        val avsnitt = vedtaksbrevService.hentVedtaksbrevSomTekst(Testdata.behandling.id)

        avsnitt.shouldHaveSize(3)
        avsnitt.first().overskrift shouldBe "Du må betale tilbake barnetrygden"
    }

    @Test
    fun `lagreFriteksterFraSaksbehandler skal ikke lagre fritekster når en av de periodene er ugyldig`() {
        lagFakta()
        val perioderMedTekst = listOf(
            PeriodeMedTekstDto(
                periode = Datoperiode(YearMonth.of(2021, 1), YearMonth.of(2021, 3)),
                faktaAvsnitt = "fakta fritekst",
                vilkårAvsnitt = "vilkår fritekst",
            ),
            PeriodeMedTekstDto(
                periode = Datoperiode(YearMonth.of(2021, 10), YearMonth.of(2021, 10)),
                faktaAvsnitt = "ugyldig",
                vilkårAvsnitt = "ugyldig",
            ),
        )
        val fritekstAvsnittDto = FritekstavsnittDto(
            oppsummeringstekst = "oppsummeringstekst",
            perioderMedTekst = perioderMedTekst,
        )

        val exception = shouldThrow<RuntimeException> {
            vedtaksbrevService.lagreFriteksterFraSaksbehandler(
                behandlingId = behandling.id,
                fritekstAvsnittDto,
            )
        }
        exception.message shouldBe "Periode 2021-10-01-2021-10-31 er ugyldig for behandling ${behandling.id}"
    }

    @Test
    fun `lagreFriteksterFraSaksbehandler skal ikke lagre fritekster når oppsummeringstekst er for lang`() {
        lagFakta()
        val exception = shouldThrow<RuntimeException> {
            vedtaksbrevService.lagreFriteksterFraSaksbehandler(
                behandlingId = behandling.id,
                lagFritekstAvsnittDto(
                    "fakta",
                    RandomStringUtils.random(5000),
                ),
            )
        }
        exception.message shouldBe "Oppsummeringstekst er for lang for behandling ${behandling.id}"
    }

    @Test
    fun `lagreFriteksterFraSaksbehandler skal ikke lagre når fritekst mangler for ANNET særliggrunner begrunnelse`() {
        lagFakta()
        lagVilkårsvurdering()
        val exception = shouldThrow<RuntimeException> {
            vedtaksbrevService.lagreFriteksterFraSaksbehandler(
                behandlingId = behandling.id,
                lagFritekstAvsnittDto("fakta", "fakta data"),
            )
        }
        exception.message shouldBe "Mangler ANNET Særliggrunner fritekst for " +
            "${Månedsperiode(YearMonth.of(2021, 1), YearMonth.of(2021, 3))}"
    }

    @Test
    fun `lagreFriteksterFraSaksbehandler skal ikke lagre når fritekst mangler for alle fakta perioder`() {
        lagFakta()
        val exception = shouldThrow<RuntimeException> {
            vedtaksbrevService.lagreFriteksterFraSaksbehandler(
                behandlingId = behandling.id,
                lagFritekstAvsnittDto(),
            )
        }
        exception.message shouldBe "Mangler fakta fritekst for alle fakta perioder"
    }

    @Test
    fun `lagreFriteksterFraSaksbehandler skal ikke lagre når fritekst mangler for en av fakta perioder`() {
        lagFakta()
        val perioderMedTekst = listOf(
            PeriodeMedTekstDto(
                periode = Datoperiode(YearMonth.of(2021, 1), YearMonth.of(2021, 1)),
                faktaAvsnitt = "fakta fritekst",
                vilkårAvsnitt = "vilkår fritekst",
            ),
            PeriodeMedTekstDto(
                periode = Datoperiode(YearMonth.of(2021, 2), YearMonth.of(2021, 2)),
                faktaAvsnitt = "fakta fritekst",
                vilkårAvsnitt = "vilkår fritekst",
            ),
            PeriodeMedTekstDto(
                periode = Datoperiode(YearMonth.of(2021, 3), YearMonth.of(2021, 3)),
                vilkårAvsnitt = "vilkår fritekst",
            ),
        )
        val fritekstAvsnittDto = FritekstavsnittDto(
            oppsummeringstekst = "oppsummeringstekst",
            perioderMedTekst = perioderMedTekst,
        )

        val exception = shouldThrow<RuntimeException> {
            vedtaksbrevService.lagreFriteksterFraSaksbehandler(
                behandlingId = behandling.id,
                fritekstavsnittDto = fritekstAvsnittDto,
            )
        }
        exception.message shouldBe "Mangler fakta fritekst for ${LocalDate.of(2021, 3, 1)}-" +
            "${LocalDate.of(2021, 3, 31)}"
    }

    @Test
    fun `lagreFriteksterFraSaksbehandler skal lagre fritekst`() {
        lagFakta()
        lagVilkårsvurdering()

        val fritekstAvsnittDto = lagFritekstAvsnittDto(
            faktaFritekst = "fakta fritekst",
            oppsummeringstekst = "oppsummering fritekst",
            særligGrunnerAnnetFritekst = "særliggrunner annet fritekst",
        )

        vedtaksbrevService.lagreFriteksterFraSaksbehandler(
            behandlingId = behandling.id,
            fritekstavsnittDto = fritekstAvsnittDto,
        )

        val avsnittene = vedtaksbrevService.hentVedtaksbrevSomTekst(behandling.id)
        avsnittene.shouldNotBeEmpty()
        avsnittene.size shouldBe 3

        val oppsummeringsavsnitt = avsnittene.firstOrNull { Avsnittstype.OPPSUMMERING == it.avsnittstype }
        oppsummeringsavsnitt.shouldNotBeNull()
        oppsummeringsavsnitt.underavsnittsliste.size shouldBe 2
        val oppsummeringsunderavsnitt1 = oppsummeringsavsnitt.underavsnittsliste[0]
        assertUnderavsnitt(
            underavsnitt = oppsummeringsunderavsnitt1,
            fritekst = "",
            fritekstTillatt = false,
            fritekstPåkrevet = false,
        )
        val oppsummeringsunderavsnitt2 = oppsummeringsavsnitt.underavsnittsliste[1]
        assertUnderavsnitt(
            underavsnitt = oppsummeringsunderavsnitt2,
            fritekst = "oppsummering fritekst",
            fritekstTillatt = true,
            fritekstPåkrevet = false,
        )

        val periodeAvsnitter = avsnittene.firstOrNull { Avsnittstype.PERIODE == it.avsnittstype }
        periodeAvsnitter.shouldNotBeNull()
        periodeAvsnitter.fom shouldBe LocalDate.of(2021, 1, 1)
        periodeAvsnitter.tom shouldBe LocalDate.of(2021, 3, 31)

        periodeAvsnitter.underavsnittsliste.size shouldBe 7
        val faktaUnderavsnitt = periodeAvsnitter.underavsnittsliste
            .firstOrNull { Underavsnittstype.FAKTA == it.underavsnittstype }
        faktaUnderavsnitt.shouldNotBeNull()
        assertUnderavsnitt(
            underavsnitt = faktaUnderavsnitt,
            fritekst = "fakta fritekst",
            fritekstTillatt = true,
            fritekstPåkrevet = true,
        )

        val foreldelseUnderavsnitt = periodeAvsnitter.underavsnittsliste
            .firstOrNull { Underavsnittstype.FORELDELSE == it.underavsnittstype }
        foreldelseUnderavsnitt.shouldBeNull() // periodene er ikke foreldet

        val vilkårUnderavsnitter = periodeAvsnitter.underavsnittsliste.filter { Underavsnittstype.VILKÅR == it.underavsnittstype }
        vilkårUnderavsnitter.size shouldBe 2
        val vilkårUnderavsnitt1 = vilkårUnderavsnitter[0]
        assertUnderavsnitt(
            underavsnitt = vilkårUnderavsnitt1,
            fritekst = "",
            fritekstTillatt = false,
            fritekstPåkrevet = false,
        )
        val vilkårUnderavsnitt2 = vilkårUnderavsnitter[1]
        assertUnderavsnitt(
            underavsnitt = vilkårUnderavsnitt2,
            fritekst = "vilkår fritekst",
            fritekstTillatt = true,
            fritekstPåkrevet = false,
        )

        val særligGrunnerUnderavsnitt = periodeAvsnitter.underavsnittsliste
            .firstOrNull { Underavsnittstype.SÆRLIGEGRUNNER == it.underavsnittstype }
        særligGrunnerUnderavsnitt.shouldNotBeNull()
        assertUnderavsnitt(
            underavsnitt = særligGrunnerUnderavsnitt,
            fritekst = "særliggrunner fritekst",
            fritekstTillatt = true,
            fritekstPåkrevet = false,
        )

        val særligGrunnerAnnetUnderavsnitt = periodeAvsnitter.underavsnittsliste
            .firstOrNull { Underavsnittstype.SÆRLIGEGRUNNER_ANNET == it.underavsnittstype }
        særligGrunnerAnnetUnderavsnitt.shouldNotBeNull()
        assertUnderavsnitt(
            underavsnitt = særligGrunnerAnnetUnderavsnitt,
            fritekst = "særliggrunner annet fritekst",
            fritekstTillatt = true,
            fritekstPåkrevet = true,
        )

        val tilleggsavsnitt = avsnittene.firstOrNull { Avsnittstype.TILLEGGSINFORMASJON == it.avsnittstype }
        tilleggsavsnitt.shouldNotBeNull()
    }

    @Test
    fun `lagreUtkastAvFriteksterFraSaksbehandler skal lagre selv når påkrevet fritekst mangler for alle fakta perioder`() {
        lagFakta()
        lagVilkårsvurdering()

        val fritekstAvsnittDto = lagFritekstAvsnittDto(
            oppsummeringstekst = "oppsummering fritekst",
            særligGrunnerAnnetFritekst = "særliggrunner annet fritekst",
        )
        vedtaksbrevService.lagreUtkastAvFritekster(
            behandlingId = behandling.id,
            fritekstAvsnittDto,
        )

        val avsnittene = vedtaksbrevService.hentVedtaksbrevSomTekst(behandling.id)
        avsnittene.shouldNotBeEmpty()
        avsnittene.size shouldBe 3
    }

    @Test
    fun `lagreUtkastAvFriteksterFraSaksbehandler skal lagre selv når påkrevet fritekst mangler for ANNET særliggrunner begrunnelse`() {
        lagFakta()
        lagVilkårsvurdering()

        val fritekstAvsnittDto = lagFritekstAvsnittDto(
            faktaFritekst = "fakta fritekst",
            oppsummeringstekst = "oppsummering fritekst",
        )

        vedtaksbrevService.lagreUtkastAvFritekster(
            behandlingId = behandling.id,
            fritekstavsnittDto = fritekstAvsnittDto,
        )

        val avsnittene = vedtaksbrevService.hentVedtaksbrevSomTekst(behandling.id)
        avsnittene.shouldNotBeEmpty()
        avsnittene.size shouldBe 3
    }

    @Test
    fun `lagreUtkastAvFriteksterFraSaksbehandler skal lagre selv når påkrevet fritekst mangler for oppsummering`() {
        var lokalBehandling = Testdata.revurdering.copy(
            id = UUID.randomUUID(),
            eksternBrukId = UUID.randomUUID(),
            årsaker = setOf(
                Behandlingsårsak(
                    originalBehandlingId = behandling.id,
                    type = Behandlingsårsakstype.REVURDERING_OPPLYSNINGER_OM_VILKÅR,
                ),
            ),
        )
        lokalBehandling = behandlingRepository.insert(lokalBehandling)

        lagFakta(lokalBehandling.id)
        lagVilkårsvurdering(lokalBehandling.id)

        val fritekstAvsnittDto = lagFritekstAvsnittDto(
            faktaFritekst = "fakta fritekst",
            særligGrunnerAnnetFritekst = "særliggrunner annet fritekst",
        )

        vedtaksbrevService.lagreUtkastAvFritekster(
            behandlingId = lokalBehandling.id,
            fritekstavsnittDto = fritekstAvsnittDto,
        )

        val avsnittene = vedtaksbrevService.hentVedtaksbrevSomTekst(lokalBehandling.id)
        avsnittene.shouldNotBeEmpty()
        avsnittene.size shouldBe 3
    }

    @Test
    fun `lagreFriteksterFraSaksbehandler skal ikke lagre fritekster når påkrevet oppsummeringstekst mangler`() {
        var lokalBehandling = Testdata.revurdering.copy(
            id = UUID.randomUUID(),
            eksternBrukId = UUID.randomUUID(),
            årsaker = setOf(
                Behandlingsårsak(
                    originalBehandlingId = behandling.id,
                    type = Behandlingsårsakstype.REVURDERING_OPPLYSNINGER_OM_VILKÅR,
                ),
            ),
        )
        lokalBehandling = behandlingRepository.insert(lokalBehandling)
        lagFakta(lokalBehandling.id)
        lagVilkårsvurdering(lokalBehandling.id)

        val exception = shouldThrow<RuntimeException> {
            vedtaksbrevService.lagreFriteksterFraSaksbehandler(
                behandlingId = lokalBehandling.id,
                lagFritekstAvsnittDto(
                    faktaFritekst = "fakta",
                    særligGrunnerAnnetFritekst = "test",
                ),
            )
        }
        exception.message shouldBe "oppsummering fritekst påkrevet for revurdering ${lokalBehandling.id}"
    }

    private fun lagFritekstAvsnittDto(
        faktaFritekst: String? = null,
        oppsummeringstekst: String? = null,
        særligGrunnerAnnetFritekst: String? = null,
    ): FritekstavsnittDto {
        val perioderMedTekst = listOf(
            PeriodeMedTekstDto(
                periode = Datoperiode(YearMonth.of(2021, 1), YearMonth.of(2021, 3)),
                faktaAvsnitt = faktaFritekst,
                vilkårAvsnitt = "vilkår fritekst",
                foreldelseAvsnitt = "foreldelse fritekst",
                særligeGrunnerAvsnitt = "særliggrunner fritekst",
                særligeGrunnerAnnetAvsnitt = særligGrunnerAnnetFritekst,
            ),
        )
        return FritekstavsnittDto(
            oppsummeringstekst = oppsummeringstekst,
            perioderMedTekst = perioderMedTekst,
        )
    }

    private fun lagFakta(behandlingId: UUID = behandling.id) {
        val faktaFeilutbetaltePerioder =
            setOf(
                FaktaFeilutbetalingsperiode(
                    periode = Månedsperiode(YearMonth.of(2021, 1), YearMonth.of(2021, 3)),
                    hendelsestype = Hendelsestype.ANNET,
                    hendelsesundertype = Hendelsesundertype.ANNET_FRITEKST,
                ),
            )
        faktaFeilutbetalingService.deaktiverEksisterendeFaktaOmFeilutbetaling(behandlingId)
        faktaRepository.insert(
            FaktaFeilutbetaling(
                behandlingId = behandlingId,
                begrunnelse = "fakta begrrunnelse",
                perioder = faktaFeilutbetaltePerioder,
            ),
        )
    }

    private fun lagVilkårsvurdering(behandlingId: UUID = behandling.id) {
        val aktsomhet =
            VilkårsvurderingAktsomhet(
                aktsomhet = Aktsomhet.GROV_UAKTSOMHET,
                særligeGrunnerBegrunnelse = "Særlig grunner begrunnelse",
                særligeGrunnerTilReduksjon = false,
                vilkårsvurderingSærligeGrunner =
                setOf(
                    VilkårsvurderingSærligGrunn(
                        særligGrunn = SærligGrunn.ANNET,
                        begrunnelse = "Annet begrunnelse",
                    ),
                ),
                begrunnelse = "aktsomhet begrunnelse",
            )
        val vilkårsvurderingPeriode =
            Vilkårsvurderingsperiode(
                periode = Månedsperiode(YearMonth.of(2021, 1), YearMonth.of(2021, 3)),
                vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FEIL_OPPLYSNINGER_FRA_BRUKER,
                begrunnelse = "Vilkårsvurdering begrunnelse",
                aktsomhet = aktsomhet,
            )
        vilkårsvurderingService.deaktiverEksisterendeVilkårsvurdering(behandlingId)
        vilkårsvurderingRepository.insert(
            Vilkårsvurdering(
                behandlingId = behandlingId,
                perioder = setOf(vilkårsvurderingPeriode),
            ),
        )
    }

    private fun assertUnderavsnitt(
        underavsnitt: Underavsnitt,
        fritekst: String,
        fritekstTillatt: Boolean,
        fritekstPåkrevet: Boolean,
    ) {
        underavsnitt.fritekst shouldBe fritekst
        underavsnitt.fritekstTillatt shouldBe fritekstTillatt
        underavsnitt.fritekstPåkrevet shouldBe fritekstPåkrevet
    }

    companion object {

        private val forhåndvisningDto = HentForhåndvisningVedtaksbrevPdfDto(
            Testdata.behandling.id,
            "Dette er en stor og gild oppsummeringstekst",
            listOf(
                PeriodeMedTekstDto(
                    Datoperiode(
                        LocalDate.now().minusDays(1),
                        LocalDate.now(),
                    ),
                    faktaAvsnitt = "&bob",
                    vilkårAvsnitt = "<bob>",
                    særligeGrunnerAnnetAvsnitt = "'bob' \"bob\"",
                ),
            ),
        )
    }
}
