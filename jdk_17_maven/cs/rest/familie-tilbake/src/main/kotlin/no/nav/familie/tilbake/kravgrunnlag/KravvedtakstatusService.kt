package no.nav.familie.tilbake.kravgrunnlag

import no.nav.familie.kontrakter.felles.historikkinnslag.Aktør
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.api.dto.HenleggelsesbrevFritekstDto
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.BehandlingService
import no.nav.familie.tilbake.behandling.FagsystemUtil
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultatstype
import no.nav.familie.tilbake.behandling.steg.StegService
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.Behandlingsstegsinfo
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.behandlingskontroll.domain.Venteårsak
import no.nav.familie.tilbake.common.exceptionhandler.UgyldigStatusmeldingFeil
import no.nav.familie.tilbake.historikkinnslag.HistorikkTaskService
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlag431
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravstatuskode
import no.nav.familie.tilbake.kravgrunnlag.domain.ØkonomiXmlMottatt
import no.nav.familie.tilbake.micrometer.TellerService
import no.nav.familie.tilbake.oppgave.OppgaveTaskService
import no.nav.tilbakekreving.status.v1.KravOgVedtakstatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class KravvedtakstatusService(
    private val kravgrunnlagRepository: KravgrunnlagRepository,
    private val behandlingRepository: BehandlingRepository,
    private val mottattXmlService: ØkonomiXmlMottattService,
    private val stegService: StegService,
    private val tellerService: TellerService,
    private val behandlingskontrollService: BehandlingskontrollService,
    private val behandlingService: BehandlingService,
    private val historikkTaskService: HistorikkTaskService,
    private val oppgaveTaskService: OppgaveTaskService,
) {

    @Transactional
    fun håndterMottattStatusmelding(statusmeldingXml: String) {
        val kravOgVedtakstatus: KravOgVedtakstatus = KravgrunnlagUtil.unmarshalStatusmelding(statusmeldingXml)

        validerStatusmelding(kravOgVedtakstatus)

        val fagsystemId = kravOgVedtakstatus.fagsystemId
        val vedtakId = kravOgVedtakstatus.vedtakId
        val ytelsestype: Ytelsestype = KravgrunnlagUtil.tilYtelsestype(kravOgVedtakstatus.kodeFagomraade)

        val behandling: Behandling? = finnÅpenBehandling(ytelsestype, fagsystemId)
        if (behandling == null) {
            val kravgrunnlagXmlListe = mottattXmlService
                .hentMottattKravgrunnlag(
                    eksternFagsakId = fagsystemId,
                    ytelsestype = ytelsestype,
                    vedtakId = vedtakId,
                )
            håndterStatusmeldingerUtenBehandling(kravgrunnlagXmlListe, kravOgVedtakstatus)
            mottattXmlService.arkiverMottattXml(statusmeldingXml, fagsystemId, ytelsestype)
            tellerService.tellUkobletStatusmelding(FagsystemUtil.hentFagsystemFraYtelsestype(ytelsestype))
            return
        }
        val kravgrunnlag431: Kravgrunnlag431 = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandling.id)
        håndterStatusmeldingerMedBehandling(kravgrunnlag431, kravOgVedtakstatus, behandling)
        mottattXmlService.arkiverMottattXml(statusmeldingXml, fagsystemId, ytelsestype)
        tellerService.tellKobletStatusmelding(FagsystemUtil.hentFagsystemFraYtelsestype(ytelsestype))
    }

    private fun validerStatusmelding(kravOgVedtakstatus: KravOgVedtakstatus) {
        kravOgVedtakstatus.referanse
            ?: throw UgyldigStatusmeldingFeil(
                melding = "Ugyldig statusmelding for vedtakId=${kravOgVedtakstatus.vedtakId}, " +
                    "Mangler referanse.",
            )
    }

    private fun finnÅpenBehandling(
        ytelsestype: Ytelsestype,
        fagsystemId: String,
    ): Behandling? {
        return behandlingRepository.finnÅpenTilbakekrevingsbehandling(
            ytelsestype = ytelsestype,
            eksternFagsakId = fagsystemId,
        )
    }

    private fun håndterStatusmeldingerUtenBehandling(
        kravgrunnlagXmlListe: List<ØkonomiXmlMottatt>,
        kravOgVedtakstatus: KravOgVedtakstatus,
    ) {
        when (val kravstatuskode = Kravstatuskode.fraKode(kravOgVedtakstatus.kodeStatusKrav)) {
            Kravstatuskode.SPERRET, Kravstatuskode.MANUELL ->
                kravgrunnlagXmlListe.forEach { mottattXmlService.oppdaterMottattXml(it.copy(sperret = true)) }
            Kravstatuskode.ENDRET -> kravgrunnlagXmlListe.forEach {
                mottattXmlService
                    .oppdaterMottattXml(it.copy(sperret = false))
            }
            Kravstatuskode.AVSLUTTET -> kravgrunnlagXmlListe.forEach {
                mottattXmlService.arkiverMottattXml(
                    it.melding,
                    it.eksternFagsakId,
                    it.ytelsestype,
                )
                mottattXmlService.slettMottattXml(it.id)
            }
            else -> throw IllegalArgumentException("Ukjent statuskode $kravstatuskode i statusmelding")
        }
    }

    private fun håndterStatusmeldingerMedBehandling(
        kravgrunnlag431: Kravgrunnlag431,
        kravOgVedtakstatus: KravOgVedtakstatus,
        behandling: Behandling,
    ) {
        when (val kravstatuskode = Kravstatuskode.fraKode(kravOgVedtakstatus.kodeStatusKrav)) {
            Kravstatuskode.SPERRET, Kravstatuskode.MANUELL -> {
                håndterSperMeldingMedBehandling(behandling.id, kravgrunnlag431)
            }
            Kravstatuskode.ENDRET -> {
                kravgrunnlagRepository.update(kravgrunnlag431.copy(sperret = false))
                stegService.håndterSteg(behandling.id)
                oppgaveTaskService.oppdaterOppgaveTask(
                    behandlingId = behandling.id,
                    beskrivelse = "Behandling er tatt av vent, pga mottatt ENDR melding",
                    frist = LocalDate.now(),
                )
            }
            Kravstatuskode.AVSLUTTET -> {
                kravgrunnlagRepository.update(kravgrunnlag431.copy(avsluttet = true))
                behandlingService
                    .henleggBehandling(
                        behandlingId = behandling.id,
                        HenleggelsesbrevFritekstDto(
                            behandlingsresultatstype = Behandlingsresultatstype
                                .HENLAGT_KRAVGRUNNLAG_NULLSTILT,
                            begrunnelse = "",
                        ),
                    )
            }
            else -> throw IllegalArgumentException("Ukjent statuskode $kravstatuskode i statusmelding")
        }
    }

    @Transactional
    fun håndterSperMeldingMedBehandling(
        behandlingId: UUID,
        kravgrunnlag431: Kravgrunnlag431,
    ) {
        kravgrunnlagRepository.update(kravgrunnlag431.copy(sperret = true))
        val venteårsak = Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG
        val tidsfrist = LocalDate.now().plusWeeks(venteårsak.defaultVenteTidIUker)
        behandlingskontrollService
            .tilbakehoppBehandlingssteg(
                behandlingId,
                Behandlingsstegsinfo(
                    behandlingssteg = Behandlingssteg.GRUNNLAG,
                    behandlingsstegstatus = Behandlingsstegstatus.VENTER,
                    venteårsak = venteårsak,
                    tidsfrist = tidsfrist,
                ),
            )
        historikkTaskService.lagHistorikkTask(
            behandlingId = behandlingId,
            historikkinnslagstype = TilbakekrevingHistorikkinnslagstype.BEHANDLING_PÅ_VENT,
            aktør = Aktør.VEDTAKSLØSNING,
            beskrivelse = venteårsak.beskrivelse,
        )

        // oppgave oppdateres ikke dersom behandling venter på varsel
        val aktivtBehandlingssteg = behandlingskontrollService.finnAktivtSteg(behandlingId)
        if (aktivtBehandlingssteg?.let { it != Behandlingssteg.VARSEL } == true) {
            oppgaveTaskService.oppdaterOppgaveTask(
                behandlingId = behandlingId,
                beskrivelse = venteårsak.beskrivelse,
                frist = tidsfrist,
            )
        }
    }
}
