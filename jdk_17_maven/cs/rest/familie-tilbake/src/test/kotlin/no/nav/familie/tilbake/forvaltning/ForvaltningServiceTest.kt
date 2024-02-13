package no.nav.familie.tilbake.forvaltning

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import no.nav.familie.kontrakter.felles.historikkinnslag.Aktør
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.HentFagsystemsbehandlingRequestSendtRepository
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultatstype
import no.nav.familie.tilbake.behandling.domain.Behandlingsstatus
import no.nav.familie.tilbake.behandlingskontroll.BehandlingsstegstilstandRepository
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstilstand
import no.nav.familie.tilbake.behandlingskontroll.domain.Venteårsak
import no.nav.familie.tilbake.common.ContextService
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.datavarehus.saksstatistikk.SendSakshendelseTilDvhTask
import no.nav.familie.tilbake.dokumentbestilling.vedtak.VedtaksbrevsoppsummeringRepository
import no.nav.familie.tilbake.faktaomfeilutbetaling.FaktaFeilutbetalingRepository
import no.nav.familie.tilbake.historikkinnslag.LagHistorikkinnslagTask
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.kravgrunnlag.domain.Fagområdekode
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravstatuskode
import no.nav.familie.tilbake.kravgrunnlag.domain.ØkonomiXmlMottatt
import no.nav.familie.tilbake.kravgrunnlag.ØkonomiXmlMottattArkivRepository
import no.nav.familie.tilbake.kravgrunnlag.ØkonomiXmlMottattRepository
import no.nav.familie.tilbake.oppgave.FerdigstillOppgaveTask
import no.nav.familie.tilbake.totrinn.TotrinnsvurderingRepository
import no.nav.familie.tilbake.vilkårsvurdering.VilkårsvurderingRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigInteger
import java.time.LocalDate

internal class ForvaltningServiceTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var kravgrunnlagRepository: KravgrunnlagRepository

    @Autowired
    private lateinit var økonomiXmlMottattRepository: ØkonomiXmlMottattRepository

    @Autowired
    private lateinit var økonomiXmlMottattArkivRepository: ØkonomiXmlMottattArkivRepository

    @Autowired
    private lateinit var taskService: TaskService

    @Autowired
    private lateinit var behandlingsstegstilstandRepository: BehandlingsstegstilstandRepository

    @Autowired
    private lateinit var requestSendtRepository: HentFagsystemsbehandlingRequestSendtRepository

    @Autowired
    private lateinit var faktaFeilutbetalingRepository: FaktaFeilutbetalingRepository

    @Autowired
    private lateinit var vilkårsvurderingRepository: VilkårsvurderingRepository

    @Autowired
    private lateinit var vedtaksbrevsoppsummeringRepository: VedtaksbrevsoppsummeringRepository

    @Autowired
    private lateinit var totrinnRepository: TotrinnsvurderingRepository

    @Autowired
    private lateinit var forvaltningService: ForvaltningService

    private val behandling = Testdata.behandling

    @BeforeEach
    fun init() {
        fagsakRepository.insert(Testdata.fagsak)
        behandlingRepository.insert(behandling)
        behandlingsstegstilstandRepository
            .insert(
                Behandlingsstegstilstand(
                    behandlingId = behandling.id,
                    behandlingssteg = Behandlingssteg.GRUNNLAG,
                    behandlingsstegsstatus = Behandlingsstegstatus.VENTER,
                    tidsfrist = LocalDate.now().plusWeeks(3),
                    venteårsak = Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG,
                ),
            )
    }

    @Test
    fun `korrigerKravgrunnlag skal ikke hente korrigert kravgrunnlag når behandling er avsluttet`() {
        behandlingRepository.update(
            behandlingRepository.findByIdOrThrow(behandling.id)
                .copy(status = Behandlingsstatus.AVSLUTTET),
        )

        val exception = shouldThrow<RuntimeException> {
            forvaltningService.korrigerKravgrunnlag(
                behandling.id,
                BigInteger.ZERO,
            )
        }
        exception.message shouldBe "Behandling med id=${behandling.id} er allerede ferdig behandlet."
    }

    @Test
    fun `korrigerKravgrunnlag skal hente korrigert kravgrunnlag når behandling allerede har et kravgrunnlag`() {
        kravgrunnlagRepository.insert(Testdata.kravgrunnlag431)

        forvaltningService.korrigerKravgrunnlag(
            behandling.id,
            Testdata.kravgrunnlag431.eksternKravgrunnlagId,
        )

        val kravgrunnlagene = kravgrunnlagRepository.findByBehandlingId(behandling.id)
        kravgrunnlagene.size shouldBe 2
        kravgrunnlagRepository.existsByBehandlingIdAndAktivTrue(behandling.id).shouldBeTrue()

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.GRUNNLAG, Behandlingsstegstatus.UTFØRT)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.FAKTA, Behandlingsstegstatus.KLAR)
    }

    @Test
    fun `korrigerKravgrunnlag skal hente korrigert kravgrunnlag når behandling ikke har et kravgrunnlag`() {
        lagMottattXml()
        forvaltningService.korrigerKravgrunnlag(behandling.id, BigInteger.ZERO)

        val kravgrunnlagene = kravgrunnlagRepository.findByBehandlingId(behandling.id)
        kravgrunnlagene.size shouldBe 1
        kravgrunnlagRepository.existsByBehandlingIdAndAktivTrue(behandling.id).shouldBeTrue()

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.GRUNNLAG, Behandlingsstegstatus.UTFØRT)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.FAKTA, Behandlingsstegstatus.KLAR)
    }

    @Test
    fun `arkiverMottattKravgrunnlag skal arkivere mottatt xml`() {
        val økonomiXmlMottatt = lagMottattXml()
        forvaltningService.arkiverMottattKravgrunnlag(økonomiXmlMottatt.id)

        økonomiXmlMottattRepository.existsById(økonomiXmlMottatt.id).shouldBeFalse()
        økonomiXmlMottattArkivRepository.findByEksternFagsakIdAndYtelsestype(
            økonomiXmlMottatt.eksternFagsakId,
            økonomiXmlMottatt.ytelsestype,
        ).shouldNotBeEmpty()
    }

    @Test
    fun `tvingHenleggBehandling skal ikke henlegge behandling når behandling er avsluttet`() {
        behandlingRepository.update(
            behandlingRepository.findByIdOrThrow(behandling.id)
                .copy(status = Behandlingsstatus.AVSLUTTET),
        )

        val exception = shouldThrow<RuntimeException> {
            forvaltningService.tvingHenleggBehandling(behandling.id)
        }
        exception.message shouldBe "Behandling med id=${behandling.id} er allerede ferdig behandlet."
    }

    @Test
    fun `tvingHenleggBehandling skal henlegge behandling når behandling ikke er avsluttet`() {
        kravgrunnlagRepository.insert(Testdata.kravgrunnlag431)
        forvaltningService.korrigerKravgrunnlag(behandling.id, Testdata.kravgrunnlag431.eksternKravgrunnlagId)

        var behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.GRUNNLAG, Behandlingsstegstatus.UTFØRT)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.FAKTA, Behandlingsstegstatus.KLAR)

        forvaltningService.tvingHenleggBehandling(behandling.id)

        behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.GRUNNLAG, Behandlingsstegstatus.AVBRUTT)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.FAKTA, Behandlingsstegstatus.AVBRUTT)

        val oppdatertBehandling = behandlingRepository.findByIdOrThrow(behandling.id)
        oppdatertBehandling.erAvsluttet.shouldBeTrue()
        oppdatertBehandling.ansvarligSaksbehandler shouldBe ContextService.hentSaksbehandler()
        oppdatertBehandling.avsluttetDato shouldBe LocalDate.now()
        oppdatertBehandling.sisteResultat!!.type shouldBe Behandlingsresultatstype.HENLAGT_TEKNISK_VEDLIKEHOLD

        val tasker = taskService.findAll()
        tasker.shouldHaveSingleElement {
            LagHistorikkinnslagTask.TYPE == it.type &&
                behandling.id.toString() == it.payload &&
                Aktør.SAKSBEHANDLER.name == it.metadata.getProperty("aktør") &&
                TilbakekrevingHistorikkinnslagstype.BEHANDLING_HENLAGT.name == it.metadata.getProperty("historikkinnslagstype")
        }
        tasker.any {
            SendSakshendelseTilDvhTask.TASK_TYPE == it.type &&
                behandling.id.toString() == it.payload
        }.shouldBeTrue()
        tasker.shouldHaveSingleElement {
            FerdigstillOppgaveTask.TYPE == it.type &&
                behandling.id.toString() == it.payload
        }
    }

    @Test
    fun `flyttBehandlingsstegTilbakeTilFakta skal ikke flytte behandlingssteg når behandling er avsluttet`() {
        behandlingRepository.update(
            behandlingRepository.findByIdOrThrow(behandling.id)
                .copy(status = Behandlingsstatus.AVSLUTTET),
        )

        val exception = shouldThrow<RuntimeException> {
            forvaltningService.flyttBehandlingsstegTilbakeTilFakta(behandling.id)
        }
        exception.message shouldBe "Behandling med id=${behandling.id} er allerede ferdig behandlet."
    }

    @Test
    fun `flyttBehandlingsstegTilbakeTilFakta skal flytte behandlingssteg til FAKTA når behandling er i IVERKSETT_VEDTAK steg`() {
        behandlingRepository.update(
            behandlingRepository.findByIdOrThrow(behandling.id)
                .copy(status = Behandlingsstatus.IVERKSETTER_VEDTAK),
        )
        kravgrunnlagRepository.insert(Testdata.kravgrunnlag431)
        behandlingsstegstilstandRepository
            .update(
                behandlingsstegstilstandRepository.findByBehandlingIdAndBehandlingssteg(
                    behandling.id,
                    Behandlingssteg.GRUNNLAG,
                )!!
                    .copy(behandlingsstegsstatus = Behandlingsstegstatus.UTFØRT),
            )

        faktaFeilutbetalingRepository.insert(Testdata.faktaFeilutbetaling)
        lagBehandlingssteg(Behandlingssteg.FAKTA, Behandlingsstegstatus.UTFØRT)
        lagBehandlingssteg(Behandlingssteg.FORELDELSE, Behandlingsstegstatus.AUTOUTFØRT)

        vilkårsvurderingRepository.insert(Testdata.vilkårsvurdering)
        lagBehandlingssteg(Behandlingssteg.VILKÅRSVURDERING, Behandlingsstegstatus.UTFØRT)

        vedtaksbrevsoppsummeringRepository.insert(Testdata.vedtaksbrevsoppsummering)
        lagBehandlingssteg(Behandlingssteg.FORESLÅ_VEDTAK, Behandlingsstegstatus.UTFØRT)

        totrinnRepository.insert(Testdata.totrinnsvurdering)
        lagBehandlingssteg(Behandlingssteg.FATTE_VEDTAK, Behandlingsstegstatus.UTFØRT)
        lagBehandlingssteg(Behandlingssteg.IVERKSETT_VEDTAK, Behandlingsstegstatus.KLAR)

        forvaltningService.flyttBehandlingsstegTilbakeTilFakta(behandling.id)
        val behandling = behandlingRepository.findByIdOrThrow(behandling.id)
        behandling.status shouldBe Behandlingsstatus.UTREDES

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.GRUNNLAG, Behandlingsstegstatus.UTFØRT)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.FAKTA, Behandlingsstegstatus.KLAR)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.FORELDELSE, Behandlingsstegstatus.TILBAKEFØRT)
        assertBehandlingssteg(
            behandlingsstegstilstand,
            Behandlingssteg.VILKÅRSVURDERING,
            Behandlingsstegstatus.TILBAKEFØRT,
        )
        assertBehandlingssteg(
            behandlingsstegstilstand,
            Behandlingssteg.FORESLÅ_VEDTAK,
            Behandlingsstegstatus.TILBAKEFØRT,
        )
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.FATTE_VEDTAK, Behandlingsstegstatus.TILBAKEFØRT)
        assertBehandlingssteg(
            behandlingsstegstilstand,
            Behandlingssteg.IVERKSETT_VEDTAK,
            Behandlingsstegstatus.TILBAKEFØRT,
        )

        faktaFeilutbetalingRepository.findByBehandlingIdAndAktivIsTrue(behandling.id).shouldBeNull()
        vilkårsvurderingRepository.findByBehandlingIdAndAktivIsTrue(behandling.id).shouldBeNull()
        vedtaksbrevsoppsummeringRepository.findByBehandlingId(behandling.id).shouldBeNull()

        taskService.findAll().shouldHaveSingleElement {
            it.type == LagHistorikkinnslagTask.TYPE &&
                it.payload == behandling.id.toString() &&
                it.metadata["historikkinnslagstype"] == TilbakekrevingHistorikkinnslagstype
                    .BEHANDLING_FLYTTET_MED_FORVALTNING.name &&
                it.metadata["aktør"] == Aktør.VEDTAKSLØSNING.name
        }
    }

    @Test
    fun `annulerKravgrunnlag skal annulere kravgrunnlag som er koblet med en behandling`() {
        val kravgrunnlag = kravgrunnlagRepository.insert(Testdata.kravgrunnlag431)
        shouldNotThrowAny { forvaltningService.annulerKravgrunnlag(kravgrunnlag.eksternKravgrunnlagId) }
    }

    @Test
    fun `annulerKravgrunnlag skal annulere kravgrunnlag som er mottatt i økonomiXmlMottatt`() {
        val økonomiXmlMottatt = økonomiXmlMottattRepository.insert(Testdata.økonomiXmlMottatt)
        shouldNotThrowAny { forvaltningService.annulerKravgrunnlag(økonomiXmlMottatt.eksternKravgrunnlagId!!) }
    }

    @Test
    fun `annulerKravgrunnlag skal ikke annulere kravgrunnlag når behandling venter på kravgrunnlag`() {
        val eksternKravgrunnlagId = BigInteger.ZERO
        val exception = shouldThrow<RuntimeException> { forvaltningService.annulerKravgrunnlag(eksternKravgrunnlagId) }
        exception.message shouldBe "Finnes ikke eksternKravgrunnlagId=$eksternKravgrunnlagId"
    }

    @Test
    fun `hentForvaltningsinfo skal hente forvaltningsinfo basert på eksternFagsakId og ytelsestype`() {
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        val kravgrunnlag = Testdata.kravgrunnlag431
        kravgrunnlagRepository.insert(
            kravgrunnlag.copy(
                fagsystemId = fagsak.eksternFagsakId,
                fagområdekode = Fagområdekode.values()
                    .first { it.ytelsestype == fagsak.ytelsestype },
            ),
        )
        val forvaltningsinfo =
            forvaltningService.hentForvaltningsinfo(fagsak.ytelsestype, fagsak.eksternFagsakId).first()
        forvaltningsinfo.eksternKravgrunnlagId shouldBe kravgrunnlag.eksternKravgrunnlagId
        forvaltningsinfo.mottattXmlId.shouldBeNull()
        forvaltningsinfo.eksternId shouldBe kravgrunnlag.referanse
    }

    @Test
    fun `hentForvaltningsinfo skal hente forvaltningsinfo basert på eksternFagsakId og ytelsestype fra mottattXml`() {
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        val mottattXml = Testdata.økonomiXmlMottatt
        økonomiXmlMottattRepository.insert(
            mottattXml.copy(
                eksternFagsakId = fagsak.eksternFagsakId,
                ytelsestype = fagsak.ytelsestype,
            ),
        )
        val forvaltningsinfo =
            forvaltningService.hentForvaltningsinfo(fagsak.ytelsestype, fagsak.eksternFagsakId).first()
        forvaltningsinfo.eksternKravgrunnlagId shouldBe mottattXml.eksternKravgrunnlagId
        forvaltningsinfo.mottattXmlId shouldBe mottattXml.id
        forvaltningsinfo.eksternId shouldBe mottattXml.referanse
    }

    @Test
    fun `hentForvaltningsinfo skal ikke hente forvaltningsinfo når behandling venter på kravgrunnlag`() {
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        val exception = shouldThrow<RuntimeException> {
            forvaltningService.hentForvaltningsinfo(
                fagsak.ytelsestype,
                fagsak.eksternFagsakId,
            )
        }
        exception.message shouldBe "Finnes ikke data i systemet for ytelsestype=${fagsak.ytelsestype} " +
            "og eksternFagsakId=${fagsak.eksternFagsakId}"
    }

    private fun lagMottattXml(): ØkonomiXmlMottatt {
        val mottattXml = readXml("/kravgrunnlagxml/kravgrunnlag_BA_riktig_eksternfagsakId_ytelsestype.xml")
        return økonomiXmlMottattRepository.insert(
            ØkonomiXmlMottatt(
                melding = mottattXml,
                kravstatuskode = Kravstatuskode.NYTT,
                eksternFagsakId = "0",
                ytelsestype = Ytelsestype.BARNETRYGD,
                referanse = "0",
                eksternKravgrunnlagId = BigInteger.ZERO,
                vedtakId = BigInteger.ZERO,
                kontrollfelt = "2021-03-02-18.50.15.236315",
                sperret = false,
            ),
        )
    }

    private fun assertBehandlingssteg(
        behandlingsstegstilstand: List<Behandlingsstegstilstand>,
        behandlingssteg: Behandlingssteg,
        behandlingsstegstatus: Behandlingsstegstatus,
    ) {
        behandlingsstegstilstand.any {
            behandlingssteg == it.behandlingssteg &&
                behandlingsstegstatus == it.behandlingsstegsstatus
        }.shouldBeTrue()
    }

    private fun lagBehandlingssteg(
        behandlingssteg: Behandlingssteg,
        behandlingsstegstatus: Behandlingsstegstatus,
    ) {
        behandlingsstegstilstandRepository.insert(
            Behandlingsstegstilstand(
                behandlingId = behandling.id,
                behandlingssteg = behandlingssteg,
                behandlingsstegsstatus = behandlingsstegstatus,
            ),
        )
    }
}
