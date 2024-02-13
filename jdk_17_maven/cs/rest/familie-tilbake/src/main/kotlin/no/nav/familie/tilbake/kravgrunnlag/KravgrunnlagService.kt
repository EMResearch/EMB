package no.nav.familie.tilbake.kravgrunnlag

import no.nav.familie.kontrakter.felles.historikkinnslag.Aktør
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsystemUtil
import no.nav.familie.tilbake.behandling.HentFagsystemsbehandlingService
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.steg.StegService
import no.nav.familie.tilbake.behandling.task.OppdaterFaktainfoTask
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.behandlingskontroll.domain.Venteårsak
import no.nav.familie.tilbake.config.PropertyName
import no.nav.familie.tilbake.historikkinnslag.HistorikkTaskService
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlag431
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravstatuskode
import no.nav.familie.tilbake.kravgrunnlag.event.EndretKravgrunnlagEventPublisher
import no.nav.familie.tilbake.micrometer.TellerService
import no.nav.familie.tilbake.oppgave.OppgaveTaskService
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Properties

@Service
class KravgrunnlagService(
    private val kravgrunnlagRepository: KravgrunnlagRepository,
    private val behandlingRepository: BehandlingRepository,
    private val mottattXmlService: ØkonomiXmlMottattService,
    private val stegService: StegService,
    private val behandlingskontrollService: BehandlingskontrollService,
    private val taskService: TaskService,
    private val tellerService: TellerService,
    private val oppgaveTaskService: OppgaveTaskService,
    private val historikkTaskService: HistorikkTaskService,
    private val hentFagsystemsbehandlingService: HentFagsystemsbehandlingService,
    private val endretKravgrunnlagEventPublisher: EndretKravgrunnlagEventPublisher,
) {

    private val log = LoggerFactory.getLogger(this::class.java)

    @Transactional
    fun håndterMottattKravgrunnlag(kravgrunnlagXml: String) {
        val kravgrunnlag: DetaljertKravgrunnlagDto = KravgrunnlagUtil.unmarshalKravgrunnlag(kravgrunnlagXml)
        // valider grunnlag
        KravgrunnlagValidator.validerGrunnlag(kravgrunnlag)

        val fagsystemId = kravgrunnlag.fagsystemId
        val ytelsestype: Ytelsestype = KravgrunnlagUtil.tilYtelsestype(kravgrunnlag.kodeFagomraade)

        val behandling: Behandling? = finnÅpenBehandling(ytelsestype, fagsystemId)
        val fagsystem = FagsystemUtil.hentFagsystemFraYtelsestype(ytelsestype)
        if (behandling == null) {
            mottattXmlService.arkiverEksisterendeGrunnlag(kravgrunnlag)
            mottattXmlService.lagreMottattXml(kravgrunnlagXml, kravgrunnlag, ytelsestype)
            tellerService.tellUkobletKravgrunnlag(fagsystem)
            return
        }
        // mapper grunnlag til Kravgrunnlag431
        val kravgrunnlag431: Kravgrunnlag431 = KravgrunnlagMapper.tilKravgrunnlag431(kravgrunnlag, behandling.id)
        sjekkIdentiskKravgrunnlag(kravgrunnlag431, behandling)
        lagreKravgrunnlag(kravgrunnlag431, ytelsestype)
        mottattXmlService.arkiverMottattXml(kravgrunnlagXml, fagsystemId, ytelsestype)

        historikkTaskService.lagHistorikkTask(
            behandling.id,
            TilbakekrevingHistorikkinnslagstype.KRAVGRUNNLAG_MOTTATT,
            Aktør.VEDTAKSLØSNING,
        )

        // oppdater frist på oppgave når behandling venter på grunnlag
        val aktivBehandlingsstegstilstand = behandlingskontrollService.finnAktivStegstilstand(behandling.id)
        if (aktivBehandlingsstegstilstand?.venteårsak == Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG) {
            håndterOppgave(behandling)
        } else {
            håndterOppgavePrioritet(behandling)
        }

        if (Kravstatuskode.ENDRET == kravgrunnlag431.kravstatuskode) {
            log.info("Mottatt ENDR kravgrunnlag. Fjerner eksisterende data for behandling ${behandling.id}")
            endretKravgrunnlagEventPublisher.fireEvent(behandlingId = behandling.id)
            // flytter behandlingssteg tilbake til fakta,
            // behandling har allerede fått SPER melding og venter på kravgrunnlag
            when (aktivBehandlingsstegstilstand?.behandlingsstegsstatus) {
                Behandlingsstegstatus.VENTER -> {
                    log.info(
                        "Behandling ${behandling.id} venter på kravgrunnlag, mottatt ENDR kravgrunnlag. " +
                            "Flytter behandlingen til fakta steg",
                    )
                    behandlingskontrollService.tilbakeførBehandledeSteg(behandling.id)
                }

                else -> { // behandling har ikke fått SPER melding og har noen steg som blir behandlet
                    log.info(
                        "Behandling ${behandling.id} blir behandlet, mottatt ENDR kravgrunnlag. " +
                            "Flytter behandlingen til fakta steg",
                    )
                    behandlingskontrollService.behandleStegPåNytt(behandling.id, Behandlingssteg.FAKTA)
                }
            }
        }
        stegService.håndterSteg(behandling.id)
        tellerService.tellKobletKravgrunnlag(fagsystem)
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

    private fun lagreKravgrunnlag(kravgrunnlag431: Kravgrunnlag431, ytelsestype: Ytelsestype) {
        val finnesKravgrunnlag = kravgrunnlagRepository.existsByBehandlingIdAndAktivTrue(kravgrunnlag431.behandlingId)
        if (finnesKravgrunnlag) {
            val eksisterendeKravgrunnlag = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(kravgrunnlag431.behandlingId)
            loggFeilHvisGammeltKravgrunnlag(kravgrunnlag431, eksisterendeKravgrunnlag)
            kravgrunnlagRepository.update(eksisterendeKravgrunnlag.copy(aktiv = false))
            if (eksisterendeKravgrunnlag.referanse != kravgrunnlag431.referanse) {
                hentOgOppdaterFaktaInfo(kravgrunnlag431, ytelsestype)
            }
        }
        kravgrunnlagRepository.insert(kravgrunnlag431)
    }

    private fun loggFeilHvisGammeltKravgrunnlag(
        kravgrunnlag431: Kravgrunnlag431,
        eksisterendeKravgrunnlag: Kravgrunnlag431,
    ) {
        if (kravgrunnlag431.kontrollfelt < eksisterendeKravgrunnlag.kontrollfelt) {
            log.error(
                "Skitpomfrit! Det hentes inn et eldre kravgrunnlag enn det som allerede finnes på behandlingen. Dette må sjekkes nærmere! " +
                    "Gjelder behandling=${kravgrunnlag431.behandlingId}, " +
                    "eksisterendeKravgrunnlagId=${eksisterendeKravgrunnlag.id}, " +
                    "eksisterendeEksternKravgrunnlagId=${eksisterendeKravgrunnlag.eksternKravgrunnlagId}, " +
                    "nyKravgrunnlagId=${kravgrunnlag431.id}, " +
                    "nyEksternKravgrunnlagId=${kravgrunnlag431.eksternKravgrunnlagId}",
            )
        }
    }

    private fun hentOgOppdaterFaktaInfo(
        kravgrunnlag431: Kravgrunnlag431,
        ytelsestype: Ytelsestype,
    ) {
        // henter faktainfo fra fagsystem for ny referanse via kafka
        hentFagsystemsbehandlingService.sendHentFagsystemsbehandlingRequest(
            eksternFagsakId = kravgrunnlag431.fagsystemId,
            ytelsestype = ytelsestype,
            eksternId = kravgrunnlag431.referanse,
        )
        // OppdaterFaktainfoTask skal oppdatere fakta info med ny hentet faktainfo
        taskService.save(
            Task(
                type = OppdaterFaktainfoTask.TYPE,
                payload = "",
                properties = Properties().apply {
                    setProperty("eksternFagsakId", kravgrunnlag431.fagsystemId)
                    setProperty("ytelsestype", ytelsestype.name)
                    setProperty("eksternId", kravgrunnlag431.referanse)
                    setProperty(PropertyName.FAGSYSTEM, FagsystemUtil.hentFagsystemFraYtelsestype(ytelsestype).name)
                },
            ),
        )
    }

    private fun håndterOppgave(behandling: Behandling) {
        val revurderingsvedtaksdato = behandling.aktivFagsystemsbehandling.revurderingsvedtaksdato
        val interval = ChronoUnit.DAYS.between(revurderingsvedtaksdato, LocalDate.now())
        if (interval >= FRIST_DATO_GRENSE) {
            oppgaveTaskService.oppdaterOppgaveTask(
                behandlingId = behandling.id,
                beskrivelse = "Behandling er tatt av vent, pga mottatt kravgrunnlag",
                frist = LocalDate.now().plusDays(1),
            )
        } else {
            val beskrivelse = "Behandling er tatt av vent, " +
                "men revurderingsvedtaksdato er mindre enn $FRIST_DATO_GRENSE dager fra dagens dato." +
                "Fristen settes derfor $FRIST_DATO_GRENSE dager fra revurderingsvedtaksdato " +
                "for å sikre at behandlingen har mottatt oppdatert kravgrunnlag"
            oppgaveTaskService.oppdaterOppgaveTask(
                behandlingId = behandling.id,
                beskrivelse = beskrivelse,
                frist = revurderingsvedtaksdato.plusDays(FRIST_DATO_GRENSE),
            )
        }
    }

    private fun håndterOppgavePrioritet(behandling: Behandling) {
        oppgaveTaskService.oppdaterOppgavePrioritetTask(behandlingId = behandling.id, fagsakId = behandling.aktivFagsystemsbehandling.eksternId)
    }

    private fun sjekkIdentiskKravgrunnlag(endretKravgrunnlag: Kravgrunnlag431, behandling: Behandling) {
        if (endretKravgrunnlag.kravstatuskode != Kravstatuskode.ENDRET ||
            // sjekker ikke identisk kravgrunnlag for behandlinger som har sendt varselbrev
            behandling.aktivtVarsel != null ||
            // sjekker ikke identisk kravgrunnlag når behandling ikke har koblet med et NYTT kravgrunnlag
            !kravgrunnlagRepository.existsByBehandlingIdAndAktivTrue(endretKravgrunnlag.behandlingId)
        ) {
            return
        }
        val forrigeKravgrunnlag = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(endretKravgrunnlag.behandlingId)
        val harSammeAntallPerioder = forrigeKravgrunnlag.perioder.size == endretKravgrunnlag.perioder.size
        val perioderIForrigeKravgrunnlag = forrigeKravgrunnlag.perioder.sortedBy { it.periode }
        val perioderIEndretKravgrunnlag = endretKravgrunnlag.perioder.sortedBy { it.periode }
        var erIdentiskKravgrunnlag = harSammeAntallPerioder
        if (harSammeAntallPerioder) {
            for (i in perioderIEndretKravgrunnlag.indices step 1) {
                if (!perioderIEndretKravgrunnlag[i].harIdentiskKravgrunnlagsperiode(perioderIForrigeKravgrunnlag[i])) {
                    erIdentiskKravgrunnlag = false
                }
            }
        }
        if (erIdentiskKravgrunnlag) {
            log.warn(
                "Mottatt kravgrunnlag med kravgrunnlagId ${endretKravgrunnlag.eksternKravgrunnlagId}," +
                    "status ${endretKravgrunnlag.kravstatuskode.kode} og referanse ${endretKravgrunnlag.referanse} " +
                    "for behandlingId=${endretKravgrunnlag.behandlingId} " +
                    "er identisk med eksisterende kravgrunnlag med kravgrunnlagId ${forrigeKravgrunnlag.eksternKravgrunnlagId}," +
                    "status ${forrigeKravgrunnlag.kravstatuskode.kode} og referanse ${forrigeKravgrunnlag.referanse}." +
                    "Undersøk om ny referanse kan gi feil i brev..",
            )
        }
    }

    companion object {

        const val FRIST_DATO_GRENSE = 10L
    }
}
