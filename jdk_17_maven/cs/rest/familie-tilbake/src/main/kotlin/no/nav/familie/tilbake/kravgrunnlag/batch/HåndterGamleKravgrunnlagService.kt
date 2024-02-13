package no.nav.familie.tilbake.kravgrunnlag.batch

import no.nav.familie.kontrakter.felles.historikkinnslag.Aktør
import no.nav.familie.kontrakter.felles.tilbakekreving.Behandlingstype
import no.nav.familie.kontrakter.felles.tilbakekreving.Faktainfo
import no.nav.familie.kontrakter.felles.tilbakekreving.HentFagsystemsbehandling
import no.nav.familie.kontrakter.felles.tilbakekreving.OpprettTilbakekrevingRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.BehandlingService
import no.nav.familie.tilbake.behandling.FagsystemUtil
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.steg.StegService
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.Behandlingsstegsinfo
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.behandlingskontroll.domain.Venteårsak
import no.nav.familie.tilbake.common.exceptionhandler.KravgrunnlagIkkeFunnetFeil
import no.nav.familie.tilbake.common.exceptionhandler.SperretKravgrunnlagFeil
import no.nav.familie.tilbake.common.exceptionhandler.UgyldigKravgrunnlagFeil
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.historikkinnslag.HistorikkService
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import no.nav.familie.tilbake.kravgrunnlag.HentKravgrunnlagService
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagMapper
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagUtil
import no.nav.familie.tilbake.kravgrunnlag.domain.Fagområdekode
import no.nav.familie.tilbake.kravgrunnlag.domain.KodeAksjon
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravstatuskode
import no.nav.familie.tilbake.kravgrunnlag.domain.ØkonomiXmlMottatt
import no.nav.familie.tilbake.kravgrunnlag.domain.ØkonomiXmlMottattArkiv
import no.nav.familie.tilbake.kravgrunnlag.ØkonomiXmlMottattService
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Service
class HåndterGamleKravgrunnlagService(
    private val behandlingRepository: BehandlingRepository,
    private val kravgrunnlagRepository: KravgrunnlagRepository,
    private val behandlingService: BehandlingService,
    private val behandlingskontrollService: BehandlingskontrollService,
    private val økonomiXmlMottattService: ØkonomiXmlMottattService,
    private val hentKravgrunnlagService: HentKravgrunnlagService,
    private val stegService: StegService,
    private val historikkService: HistorikkService,
) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    fun hentFrakobletKravgrunnlag(mottattXmlId: UUID): ØkonomiXmlMottatt {
        return økonomiXmlMottattService.hentMottattKravgrunnlag(mottattXmlId)
    }

    fun sjekkOmDetFinnesEnAktivBehandling(mottattXml: ØkonomiXmlMottatt) {
        val eksternFagsakId = mottattXml.eksternFagsakId
        val ytelsestype = mottattXml.ytelsestype
        val mottattXmlId = mottattXml.id

        logger.info("Sjekker om det finnes en aktiv behandling for fagsak=$eksternFagsakId og ytelsestype=$ytelsestype")
        if (behandlingRepository.finnÅpenTilbakekrevingsbehandling(ytelsestype, eksternFagsakId) != null) {
            throw UgyldigKravgrunnlagFeil(
                melding = "Kravgrunnlag med $mottattXmlId er ugyldig." +
                    "Det finnes allerede en åpen behandling for " +
                    "fagsak=$eksternFagsakId og ytelsestype=$ytelsestype. " +
                    "Kravgrunnlaget skulle være koblet. Kravgrunnlaget arkiveres manuelt" +
                    "ved å bruke forvaltningsrutine etter feilundersøkelse.",
            )
        }
    }

    fun sjekkArkivForDuplikatKravgrunnlagMedKravstatusAvsluttet(kravgrunnlagIkkeFunnet: ØkonomiXmlMottatt): Boolean {
        val arkiverteXmlMottattPåSammeFagsak = økonomiXmlMottattService.hentArkiverteMottattXml(
            eksternFagsakId = kravgrunnlagIkkeFunnet.eksternFagsakId,
            ytelsestype = kravgrunnlagIkkeFunnet.ytelsestype,
        )
        val arkiverteKravgrunnlag = arkiverteXmlMottattPåSammeFagsak
            .filter { it.melding.contains(Constants.kravgrunnlagXmlRootElement) }
        val arkiverteStatusmeldinger = arkiverteXmlMottattPåSammeFagsak
            .filter { it.melding.contains(Constants.statusmeldingXmlRootElement) }

        return arkiverteKravgrunnlag
            .any { arkivertKravgrunnlag ->
                arkivertKravgrunnlag.sporbar.opprettetTid.isAfter(kravgrunnlagIkkeFunnet.sporbar.opprettetTid) &&
                    sjekkDiff(
                        arkivertXml = arkivertKravgrunnlag,
                        mottattXml = kravgrunnlagIkkeFunnet,
                        forventedeAvvik = listOf("kravgrunnlagId", "vedtakId", "kontrollfelt"),
                    ) &&
                    arkivertKravgrunnlag.harKravstatusAvsluttet(arkiverteStatusmeldinger)
            }
    }

    @Transactional(rollbackFor = [Exception::class])
    fun håndter(fagsystemsbehandlingData: HentFagsystemsbehandling, mottattXml: ØkonomiXmlMottatt, task: Task) {
        logger.info("Håndterer kravgrunnlag med kravgrunnlagId=${mottattXml.eksternKravgrunnlagId}")
        val hentetData: Pair<DetaljertKravgrunnlagDto, Boolean> = try {
            hentKravgrunnlagFraØkonomi(mottattXml)
        } catch (e: KravgrunnlagIkkeFunnetFeil) {
            if (sjekkArkivForDuplikatKravgrunnlagMedKravstatusAvsluttet(kravgrunnlagIkkeFunnet = mottattXml)) {
                logger.warn(
                    "Kravgrunnlag(id=${mottattXml.id}, eksternFagsakId=${mottattXml.eksternFagsakId}) ble ikke funnet hos økonomi," +
                        " men identisk kravgrunnlag med påfølgende melding om at kravet er avsluttet ble funnet i arkivet.",
                )
                arkiverKravgrunnlag(mottattXml.id)
                task.metadata["merknad"] =
                    "Arkivert da kravgrunnlag ikke ble funnet hos økonomi, og duplikat kravgrunnlag med kravstatus AVSLUTTET funnet i arkivet"
                return
            } else {
                throw e
            }
        }
        val hentetKravgrunnlag = hentetData.first
        val erSperret = hentetData.second

        arkiverKravgrunnlag(mottattXml.id)
        val behandling = opprettBehandling(hentetKravgrunnlag, fagsystemsbehandlingData)
        val behandlingId = behandling.id

        val mottattKravgrunnlag = KravgrunnlagUtil.unmarshalKravgrunnlag(mottattXml.melding)
        val diffs = KravgrunnlagUtil.sammenlignKravgrunnlag(mottattKravgrunnlag, hentetKravgrunnlag)
        if (diffs.isNotEmpty()) {
            logger.warn("Det finnes avvik mellom hentet kravgrunnlag og mottatt kravgrunnlag for ${hentetKravgrunnlag.kodeFagomraade}. Avvikene er $diffs")
        }
        logger.info(
            "Kobler kravgrunnlag med kravgrunnlagId=${hentetKravgrunnlag.kravgrunnlagId} " +
                "til behandling=$behandlingId",
        )
        val kravgrunnlag = KravgrunnlagMapper.tilKravgrunnlag431(hentetKravgrunnlag, behandlingId)
        kravgrunnlagRepository.insert(kravgrunnlag)

        historikkService.lagHistorikkinnslag(
            behandlingId = behandlingId,
            historikkinnslagstype = TilbakekrevingHistorikkinnslagstype.KRAVGRUNNLAG_HENT,
            aktør = Aktør.VEDTAKSLØSNING,
            opprettetTidspunkt = LocalDateTime.now(),
        )

        stegService.håndterSteg(behandlingId)
        if (erSperret) {
            logger.info(
                "Hentet kravgrunnlag med kravgrunnlagId=${hentetKravgrunnlag.kravgrunnlagId} " +
                    "til behandling=$behandlingId er sperret. Venter behandlingen på ny kravgrunnlag fra økonomi",
            )
            sperKravgrunnlag(behandlingId)
        }
    }

    @Transactional
    fun arkiverKravgrunnlag(mottattXmlId: UUID) {
        val mottattXml = hentFrakobletKravgrunnlag(mottattXmlId)
        økonomiXmlMottattService.arkiverMottattXml(mottattXml.melding, mottattXml.eksternFagsakId, mottattXml.ytelsestype)
        økonomiXmlMottattService.slettMottattXml(mottattXmlId)
    }

    private fun hentKravgrunnlagFraØkonomi(mottattXml: ØkonomiXmlMottatt): Pair<DetaljertKravgrunnlagDto, Boolean> {
        return try {
            hentKravgrunnlagService.hentKravgrunnlagFraØkonomi(
                mottattXml.eksternKravgrunnlagId!!,
                KodeAksjon.HENT_KORRIGERT_KRAVGRUNNLAG,
            ) to false
        } catch (e: SperretKravgrunnlagFeil) {
            logger.warn(e.melding)
            KravgrunnlagUtil.unmarshalKravgrunnlag(mottattXml.melding) to true
        }
    }

    fun opprettBehandling(
        hentetKravgrunnlag: DetaljertKravgrunnlagDto,
        fagsystemsbehandlingData: HentFagsystemsbehandling,
    ): Behandling {
        val opprettTilbakekrevingRequest =
            lagOpprettBehandlingsrequest(
                eksternFagsakId = hentetKravgrunnlag.fagsystemId,
                ytelsestype = Fagområdekode.fraKode(hentetKravgrunnlag.kodeFagomraade)
                    .ytelsestype,
                eksternId = hentetKravgrunnlag.referanse,
                fagsystemsbehandlingData = fagsystemsbehandlingData,
            )
        return behandlingService.opprettBehandling(opprettTilbakekrevingRequest)
    }

    private fun lagOpprettBehandlingsrequest(
        eksternFagsakId: String,
        ytelsestype: Ytelsestype,
        eksternId: String,
        fagsystemsbehandlingData: HentFagsystemsbehandling,
    ): OpprettTilbakekrevingRequest {
        return OpprettTilbakekrevingRequest(
            fagsystem = FagsystemUtil.hentFagsystemFraYtelsestype(ytelsestype),
            ytelsestype = ytelsestype,
            eksternFagsakId = eksternFagsakId,
            eksternId = eksternId,
            behandlingstype = Behandlingstype.TILBAKEKREVING,
            manueltOpprettet = false,
            saksbehandlerIdent = "VL",
            personIdent = fagsystemsbehandlingData.personIdent,
            språkkode = fagsystemsbehandlingData.språkkode,
            enhetId = fagsystemsbehandlingData.enhetId,
            enhetsnavn = fagsystemsbehandlingData.enhetsnavn,
            revurderingsvedtaksdato = fagsystemsbehandlingData.revurderingsvedtaksdato,
            faktainfo = setFaktainfo(fagsystemsbehandlingData.faktainfo),
            verge = fagsystemsbehandlingData.verge,
            varsel = null,
        )
    }

    private fun sperKravgrunnlag(behandlingId: UUID) {
        val kravgrunnlag = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
        kravgrunnlagRepository.update(kravgrunnlag.copy(sperret = true))
        val venteårsak = Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG
        behandlingskontrollService
            .tilbakehoppBehandlingssteg(
                behandlingId,
                Behandlingsstegsinfo(
                    behandlingssteg = Behandlingssteg.GRUNNLAG,
                    behandlingsstegstatus = Behandlingsstegstatus.VENTER,
                    venteårsak = venteårsak,
                    tidsfrist = LocalDate.now()
                        .plusWeeks(venteårsak.defaultVenteTidIUker),
                ),
            )
        historikkService.lagHistorikkinnslag(
            behandlingId = behandlingId,
            historikkinnslagstype = TilbakekrevingHistorikkinnslagstype.BEHANDLING_PÅ_VENT,
            aktør = Aktør.VEDTAKSLØSNING,
            beskrivelse = venteårsak.beskrivelse,
            opprettetTidspunkt = LocalDateTime.now(),
        )
    }

    private fun setFaktainfo(faktainfo: Faktainfo): Faktainfo {
        return Faktainfo(
            revurderingsresultat = faktainfo.revurderingsresultat,
            revurderingsårsak = faktainfo.revurderingsårsak,
            tilbakekrevingsvalg = Tilbakekrevingsvalg.IGNORER_TILBAKEKREVING,
            konsekvensForYtelser = faktainfo.konsekvensForYtelser,
        )
    }

    private fun sjekkDiff(
        arkivertXml: ØkonomiXmlMottattArkiv,
        mottattXml: ØkonomiXmlMottatt,
        forventedeAvvik: List<String>,
    ) = arkivertXml.melding.linjeformatert.lines().minus(mottattXml.melding.linjeformatert.lines()).none { avvik ->
        forventedeAvvik.none { it in avvik }
    }
}

private val String.linjeformatert: String
    get() = replace("<urn", "\n<urn")

private fun ØkonomiXmlMottattArkiv.harKravstatusAvsluttet(statusmeldingerMottatt: List<ØkonomiXmlMottattArkiv>): Boolean {
    val kravgrunnlagDto = KravgrunnlagUtil.unmarshalKravgrunnlag(melding)

    return statusmeldingerMottatt.any {
        KravgrunnlagUtil.unmarshalStatusmelding(it.melding).let { statusmelding ->
            statusmelding.vedtakId == kravgrunnlagDto.vedtakId &&
                statusmelding.kodeStatusKrav == Kravstatuskode.AVSLUTTET.kode
        }
    }
}
