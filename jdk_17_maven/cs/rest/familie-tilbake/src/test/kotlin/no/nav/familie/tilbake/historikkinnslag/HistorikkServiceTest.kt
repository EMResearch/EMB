package no.nav.familie.tilbake.historikkinnslag

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import no.nav.familie.kontrakter.felles.Applikasjon
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.historikkinnslag.Aktør
import no.nav.familie.kontrakter.felles.historikkinnslag.Historikkinnslagstype
import no.nav.familie.kontrakter.felles.historikkinnslag.OpprettHistorikkinnslagRequest
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultat
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultatstype
import no.nav.familie.tilbake.behandling.domain.Behandlingsvedtak
import no.nav.familie.tilbake.behandling.domain.Iverksettingsstatus
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Venteårsak
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevsporingRepository
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevsporing
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevtype
import no.nav.familie.tilbake.integration.kafka.DefaultKafkaProducer
import no.nav.familie.tilbake.integration.kafka.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

internal class HistorikkServiceTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var brevsporingRepository: BrevsporingRepository

    @Autowired
    private lateinit var behandlingskontrollService: BehandlingskontrollService

    private val mockKafkaTemplate: KafkaTemplate<String, String> = mockk()
    private lateinit var spyKafkaProducer: KafkaProducer
    private lateinit var historikkService: HistorikkService

    private val fagsak = Testdata.fagsak
    private val behandling = Testdata.behandling
    private val behandlingId = behandling.id
    private val opprettetTidspunkt = LocalDateTime.now()

    private val behandlingIdSlot = slot<UUID>()
    private val keySlot = slot<String>()
    private val historikkinnslagRecordSlot = slot<OpprettHistorikkinnslagRequest>()

    @BeforeEach
    fun init() {
        fagsakRepository.insert(fagsak)
        behandlingRepository.insert(behandling)

        spyKafkaProducer = spyk(DefaultKafkaProducer(mockKafkaTemplate))
        historikkService = HistorikkService(
            behandlingRepository,
            fagsakRepository,
            brevsporingRepository,
            spyKafkaProducer,
        )
        val recordMetadata = mockk<RecordMetadata>()
        every { recordMetadata.offset() } returns 1
        val result = SendResult<String, String>(mockk(), recordMetadata)
        every { mockKafkaTemplate.send(any<ProducerRecord<String, String>>()).get() }.returns(result)
    }

    @Test
    fun `lagHistorikkinnslag skal lage historikkinnslag når behandling oppretter automatisk`() {
        historikkService.lagHistorikkinnslag(
            behandlingId,
            TilbakekrevingHistorikkinnslagstype.BEHANDLING_OPPRETTET,
            Aktør.VEDTAKSLØSNING,
            opprettetTidspunkt,
        )
        verify {
            spyKafkaProducer.sendHistorikkinnslag(
                capture(behandlingIdSlot),
                capture(keySlot),
                capture(historikkinnslagRecordSlot),
            )
        }
        assertHistorikkinnslagRequest(
            Aktør.VEDTAKSLØSNING,
            Constants.BRUKER_ID_VEDTAKSLØSNINGEN,
            TilbakekrevingHistorikkinnslagstype.BEHANDLING_OPPRETTET.tittel,
            Historikkinnslagstype.HENDELSE,
        )
    }

    @Test
    fun `lagHistorikkinnslag skal lage historikkinnslag når behandling setter på vent automatisk`() {
        behandlingskontrollService.fortsettBehandling(behandlingId)
        behandlingskontrollService.settBehandlingPåVent(
            behandlingId,
            Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING,
            LocalDate.now().plusDays(20),
        )
        historikkService.lagHistorikkinnslag(
            behandlingId,
            TilbakekrevingHistorikkinnslagstype.BEHANDLING_PÅ_VENT,
            Aktør.VEDTAKSLØSNING,
            opprettetTidspunkt,
            Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING.beskrivelse,
        )
        verify {
            spyKafkaProducer.sendHistorikkinnslag(
                capture(behandlingIdSlot),
                capture(keySlot),
                capture(historikkinnslagRecordSlot),
            )
        }
        assertHistorikkinnslagRequest(
            aktør = Aktør.VEDTAKSLØSNING,
            aktørIdent = Constants.BRUKER_ID_VEDTAKSLØSNINGEN,
            tittel = TilbakekrevingHistorikkinnslagstype.BEHANDLING_PÅ_VENT.tittel,
            tekst = "Årsak: Venter på tilbakemelding fra bruker",
            type = Historikkinnslagstype.HENDELSE,
        )
    }

    @Test
    fun `lagHistorikkinnslag skal lage historikkinnslag når behandling setter på vent manuelt`() {
        behandlingskontrollService.fortsettBehandling(behandlingId)
        behandlingskontrollService.settBehandlingPåVent(
            behandlingId,
            Venteårsak.AVVENTER_DOKUMENTASJON,
            LocalDate.now().plusDays(20),
        )
        historikkService.lagHistorikkinnslag(
            behandlingId,
            TilbakekrevingHistorikkinnslagstype.BEHANDLING_PÅ_VENT,
            Aktør.SAKSBEHANDLER,
            opprettetTidspunkt,
            Venteårsak.AVVENTER_DOKUMENTASJON.beskrivelse,
        )
        verify {
            spyKafkaProducer.sendHistorikkinnslag(
                capture(behandlingIdSlot),
                capture(keySlot),
                capture(historikkinnslagRecordSlot),
            )
        }
        assertHistorikkinnslagRequest(
            aktør = Aktør.SAKSBEHANDLER,
            aktørIdent = behandling.ansvarligSaksbehandler,
            tittel = TilbakekrevingHistorikkinnslagstype.BEHANDLING_PÅ_VENT.tittel,
            tekst = "Årsak: Avventer dokumentasjon",
            type = Historikkinnslagstype.HENDELSE,
        )
    }

    @Test
    fun `lagHistorikkinnslag skal lage historikkinnslag når behandling tar av vent manuelt`() {
        historikkService.lagHistorikkinnslag(
            behandlingId,
            TilbakekrevingHistorikkinnslagstype.BEHANDLING_GJENOPPTATT,
            Aktør.SAKSBEHANDLER,
            opprettetTidspunkt,
        )
        verify {
            spyKafkaProducer.sendHistorikkinnslag(
                capture(behandlingIdSlot),
                capture(keySlot),
                capture(historikkinnslagRecordSlot),
            )
        }
        assertHistorikkinnslagRequest(
            aktør = Aktør.SAKSBEHANDLER,
            aktørIdent = behandling.ansvarligSaksbehandler,
            tittel = TilbakekrevingHistorikkinnslagstype.BEHANDLING_GJENOPPTATT.tittel,
            type = Historikkinnslagstype.HENDELSE,
        )
    }

    @Test
    fun `lagHistorikkinnslag skal lage historikkinnslag når behandling mottar et kravgrunnlag`() {
        historikkService.lagHistorikkinnslag(
            behandlingId,
            TilbakekrevingHistorikkinnslagstype.KRAVGRUNNLAG_MOTTATT,
            Aktør.VEDTAKSLØSNING,
            opprettetTidspunkt,
        )
        verify {
            spyKafkaProducer.sendHistorikkinnslag(
                capture(behandlingIdSlot),
                capture(keySlot),
                capture(historikkinnslagRecordSlot),
            )
        }
        assertHistorikkinnslagRequest(
            aktør = Aktør.VEDTAKSLØSNING,
            aktørIdent = Constants.BRUKER_ID_VEDTAKSLØSNINGEN,
            tittel = TilbakekrevingHistorikkinnslagstype.KRAVGRUNNLAG_MOTTATT.tittel,
            type = Historikkinnslagstype.HENDELSE,
        )
    }

    @Test
    fun `lagHistorikkinnslag skal lage historikkinnslag når behandling sender varselbrev`() {
        brevsporingRepository.insert(
            Brevsporing(
                behandlingId = behandlingId,
                brevtype = Brevtype.VARSEL,
                journalpostId = "testverdi",
                dokumentId = "testverdi",
            ),
        )
        historikkService.lagHistorikkinnslag(
            behandlingId = behandlingId,
            historikkinnslagstype = TilbakekrevingHistorikkinnslagstype.VARSELBREV_SENDT,
            aktør = Aktør.VEDTAKSLØSNING,
            opprettetTidspunkt = opprettetTidspunkt,
            brevtype = Brevtype.VARSEL.name,
        )
        verify {
            spyKafkaProducer.sendHistorikkinnslag(
                capture(behandlingIdSlot),
                capture(keySlot),
                capture(historikkinnslagRecordSlot),
            )
        }
        assertHistorikkinnslagRequest(
            aktør = Aktør.VEDTAKSLØSNING,
            aktørIdent = Constants.BRUKER_ID_VEDTAKSLØSNINGEN,
            tittel = TilbakekrevingHistorikkinnslagstype.VARSELBREV_SENDT.tittel,
            tekst = TilbakekrevingHistorikkinnslagstype.VARSELBREV_SENDT.tekst,
            type = Historikkinnslagstype.BREV,
            dokumentId = "testverdi",
            journalpostId = "testverdi",
        )
    }

    @Test
    fun `lagHistorikkinnslag skal lage historikkinnslag når behandling er automatisk henlagt`() {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        behandlingRepository.update(
            behandling.copy(
                resultater =
                setOf(Behandlingsresultat(type = Behandlingsresultatstype.HENLAGT_KRAVGRUNNLAG_NULLSTILT)),
            ),
        )
        historikkService.lagHistorikkinnslag(
            behandlingId,
            TilbakekrevingHistorikkinnslagstype.BEHANDLING_HENLAGT,
            Aktør.VEDTAKSLØSNING,
            opprettetTidspunkt,
        )

        verify {
            spyKafkaProducer.sendHistorikkinnslag(
                capture(behandlingIdSlot),
                capture(keySlot),
                capture(historikkinnslagRecordSlot),
            )
        }

        assertHistorikkinnslagRequest(
            aktør = Aktør.VEDTAKSLØSNING,
            aktørIdent = Constants.BRUKER_ID_VEDTAKSLØSNINGEN,
            tittel = TilbakekrevingHistorikkinnslagstype.BEHANDLING_HENLAGT.tittel,
            tekst = "Årsak: Kravgrunnlaget er nullstilt",
            type = Historikkinnslagstype.HENDELSE,
        )
    }

    @Test
    fun `lagHistorikkinnslag skal lage historikkinnslag når behandling er manuelt henlagt`() {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        behandlingRepository.update(
            behandling.copy(
                resultater =
                setOf(Behandlingsresultat(type = Behandlingsresultatstype.HENLAGT_FEILOPPRETTET)),
            ),
        )
        historikkService.lagHistorikkinnslag(
            behandlingId,
            TilbakekrevingHistorikkinnslagstype.BEHANDLING_HENLAGT,
            Aktør.VEDTAKSLØSNING,
            opprettetTidspunkt,
            "testverdi",
        )

        verify {
            spyKafkaProducer.sendHistorikkinnslag(
                capture(behandlingIdSlot),
                capture(keySlot),
                capture(historikkinnslagRecordSlot),
            )
        }

        assertHistorikkinnslagRequest(
            aktør = Aktør.VEDTAKSLØSNING,
            aktørIdent = Constants.BRUKER_ID_VEDTAKSLØSNINGEN,
            tittel = TilbakekrevingHistorikkinnslagstype.BEHANDLING_HENLAGT.tittel,
            tekst = "Årsak: Henlagt, søknaden er feilopprettet, Begrunnelse: testverdi",
            type = Historikkinnslagstype.HENDELSE,
        )
    }

    @Test
    fun `lagHistorikkinnslag skal lage historikkinnslag når behandling sender henleggelsesbrev`() {
        brevsporingRepository.insert(
            Brevsporing(
                behandlingId = behandlingId,
                brevtype = Brevtype.HENLEGGELSE,
                journalpostId = "testverdi",
                dokumentId = "testverdi",
            ),
        )
        historikkService.lagHistorikkinnslag(
            behandlingId = behandlingId,
            historikkinnslagstype = TilbakekrevingHistorikkinnslagstype.HENLEGGELSESBREV_SENDT,
            aktør = Aktør.VEDTAKSLØSNING,
            opprettetTidspunkt = opprettetTidspunkt,
            brevtype = Brevtype.HENLEGGELSE.name,
        )
        verify {
            spyKafkaProducer.sendHistorikkinnslag(
                capture(behandlingIdSlot),
                capture(keySlot),
                capture(historikkinnslagRecordSlot),
            )
        }
        assertHistorikkinnslagRequest(
            aktør = Aktør.VEDTAKSLØSNING,
            aktørIdent = Constants.BRUKER_ID_VEDTAKSLØSNINGEN,
            tittel = TilbakekrevingHistorikkinnslagstype.HENLEGGELSESBREV_SENDT.tittel,
            tekst = TilbakekrevingHistorikkinnslagstype.HENLEGGELSESBREV_SENDT.tekst,
            type = Historikkinnslagstype.BREV,
            dokumentId = "testverdi",
            journalpostId = "testverdi",
        )
    }

    @Test
    fun `lagHistorikkinnslag skal lage historikkinnslag når behandling ikke sender henleggelsesbrev for ukjent adresse`() {
        brevsporingRepository.insert(
            Brevsporing(
                behandlingId = behandlingId,
                brevtype = Brevtype.HENLEGGELSE,
                journalpostId = "testverdi",
                dokumentId = "testverdi",
            ),
        )
        historikkService.lagHistorikkinnslag(
            behandlingId = behandlingId,
            historikkinnslagstype = TilbakekrevingHistorikkinnslagstype.BREV_IKKE_SENDT_UKJENT_ADRESSE,
            aktør = Aktør.VEDTAKSLØSNING,
            opprettetTidspunkt = opprettetTidspunkt,
            brevtype = Brevtype.HENLEGGELSE.name,
            beskrivelse = TilbakekrevingHistorikkinnslagstype.HENLEGGELSESBREV_SENDT.tekst,
        )
        verify {
            spyKafkaProducer.sendHistorikkinnslag(
                capture(behandlingIdSlot),
                capture(keySlot),
                capture(historikkinnslagRecordSlot),
            )
        }
        assertHistorikkinnslagRequest(
            aktør = Aktør.VEDTAKSLØSNING,
            aktørIdent = Constants.BRUKER_ID_VEDTAKSLØSNINGEN,
            tittel = TilbakekrevingHistorikkinnslagstype.BREV_IKKE_SENDT_UKJENT_ADRESSE.tittel,
            tekst = TilbakekrevingHistorikkinnslagstype.HENLEGGELSESBREV_SENDT.tekst + " er ikke sendt",
            type = Historikkinnslagstype.BREV,
            dokumentId = "testverdi",
            journalpostId = "testverdi",
        )
    }

    @Test
    fun `lagHistorikkinnslag skal lage historikkinnslag når fakta steg er utført for behandling`() {
        historikkService.lagHistorikkinnslag(
            behandlingId,
            TilbakekrevingHistorikkinnslagstype.FAKTA_VURDERT,
            Aktør.SAKSBEHANDLER,
            opprettetTidspunkt,
        )
        verify {
            spyKafkaProducer.sendHistorikkinnslag(
                capture(behandlingIdSlot),
                capture(keySlot),
                capture(historikkinnslagRecordSlot),
            )
        }
        assertHistorikkinnslagRequest(
            aktør = Aktør.SAKSBEHANDLER,
            aktørIdent = behandling.ansvarligSaksbehandler,
            tittel = TilbakekrevingHistorikkinnslagstype.FAKTA_VURDERT.tittel,
            type = Historikkinnslagstype.SKJERMLENKE,
            steg = Behandlingssteg.FAKTA.name,
        )
    }

    @Test
    fun `lagHistorikkinnslag skal lage historikkinnslag når foreldelse steg er utført for behandling`() {
        historikkService.lagHistorikkinnslag(
            behandlingId,
            TilbakekrevingHistorikkinnslagstype.FORELDELSE_VURDERT,
            Aktør.SAKSBEHANDLER,
            opprettetTidspunkt,
        )
        verify {
            spyKafkaProducer.sendHistorikkinnslag(
                capture(behandlingIdSlot),
                capture(keySlot),
                capture(historikkinnslagRecordSlot),
            )
        }
        assertHistorikkinnslagRequest(
            aktør = Aktør.SAKSBEHANDLER,
            aktørIdent = behandling.ansvarligSaksbehandler,
            tittel = TilbakekrevingHistorikkinnslagstype.FORELDELSE_VURDERT.tittel,
            type = Historikkinnslagstype.SKJERMLENKE,
            steg = Behandlingssteg.FORELDELSE.name,
        )
    }

    @Test
    fun `lagHistorikkinnslag skal lage historikkinnslag når behandling er fattet`() {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        behandlingRepository.update(
            behandling.copy(
                resultater =
                setOf(
                    Behandlingsresultat(
                        type = Behandlingsresultatstype.FULL_TILBAKEBETALING,
                        behandlingsvedtak = Behandlingsvedtak(
                            vedtaksdato = LocalDate.now(),
                            iverksettingsstatus =
                            Iverksettingsstatus.IVERKSATT,
                        ),
                    ),
                ),
            ),
        )
        historikkService.lagHistorikkinnslag(
            behandlingId,
            TilbakekrevingHistorikkinnslagstype.VEDTAK_FATTET,
            Aktør.BESLUTTER,
            opprettetTidspunkt,
        )
        verify {
            spyKafkaProducer.sendHistorikkinnslag(
                capture(behandlingIdSlot),
                capture(keySlot),
                capture(historikkinnslagRecordSlot),
            )
        }
        assertHistorikkinnslagRequest(
            aktør = Aktør.BESLUTTER,
            aktørIdent = requireNotNull(behandling.ansvarligBeslutter),
            tittel = TilbakekrevingHistorikkinnslagstype.VEDTAK_FATTET.tittel,
            tekst = "Resultat: Full tilbakebetaling",
            type = Historikkinnslagstype.HENDELSE,
        )
    }

    @Test
    fun `lagHistorikkinnslag skal lage historikkinnslag når man bytter enhet på behandling`() {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        behandlingRepository.update(behandling.copy(behandlendeEnhet = "3434"))

        historikkService.lagHistorikkinnslag(
            behandlingId,
            TilbakekrevingHistorikkinnslagstype.ENDRET_ENHET,
            Aktør.SAKSBEHANDLER,
            opprettetTidspunkt,
            "begrunnelse for endring",
        )

        verify {
            spyKafkaProducer.sendHistorikkinnslag(
                capture(behandlingIdSlot),
                capture(keySlot),
                capture(historikkinnslagRecordSlot),
            )
        }
        assertHistorikkinnslagRequest(
            aktør = Aktør.SAKSBEHANDLER,
            aktørIdent = requireNotNull(behandling.ansvarligSaksbehandler),
            tittel = TilbakekrevingHistorikkinnslagstype.ENDRET_ENHET.tittel,
            tekst = "Ny enhet: 3434, Begrunnelse: begrunnelse for endring",
            type = Historikkinnslagstype.HENDELSE,
        )
    }

    private fun assertHistorikkinnslagRequest(
        aktør: Aktør,
        aktørIdent: String,
        tittel: String,
        type: Historikkinnslagstype,
        tekst: String? = null,
        steg: String? = null,
        dokumentId: String? = null,
        journalpostId: String? = null,
    ) {
        behandlingIdSlot.captured shouldBe behandlingId
        val request = historikkinnslagRecordSlot.captured
        keySlot.captured shouldBe request.behandlingId

        request.eksternFagsakId shouldBe fagsak.eksternFagsakId
        request.aktør shouldBe aktør
        request.aktørIdent shouldBe aktørIdent
        request.opprettetTidspunkt shouldBe opprettetTidspunkt
        request.fagsystem shouldBe Fagsystem.BA
        request.applikasjon shouldBe Applikasjon.FAMILIE_TILBAKE
        request.tittel shouldBe tittel
        request.type shouldBe type
        request.tekst shouldBe tekst
        request.steg shouldBe steg
        request.dokumentId shouldBe dokumentId
        request.journalpostId shouldBe journalpostId
    }
}
