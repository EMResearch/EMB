package no.nav.familie.tilbake.behandlingskontroll

import no.nav.familie.kontrakter.felles.historikkinnslag.Aktør
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus.AUTOUTFØRT
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus.AVBRUTT
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus.KLAR
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus.TILBAKEFØRT
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus.UTFØRT
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus.VENTER
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstilstand
import no.nav.familie.tilbake.behandlingskontroll.domain.Venteårsak
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.FeatureToggleService
import no.nav.familie.tilbake.datavarehus.saksstatistikk.BehandlingTilstandService
import no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker.ManuellBrevmottakerRepository
import no.nav.familie.tilbake.historikkinnslag.HistorikkTaskService
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class BehandlingskontrollService(
    private val behandlingsstegstilstandRepository: BehandlingsstegstilstandRepository,
    private val behandlingRepository: BehandlingRepository,
    private val behandlingTilstandService: BehandlingTilstandService,
    private val kravgrunnlagRepository: KravgrunnlagRepository,
    private val historikkTaskService: HistorikkTaskService,
    private val featureToggleService: FeatureToggleService,
    private val brevmottakerRepository: ManuellBrevmottakerRepository,
) {

    private val log = LoggerFactory.getLogger(this::class.java)

    @Transactional
    fun fortsettBehandling(behandlingId: UUID) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        if (behandling.erAvsluttet) {
            return
        }
        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        val aktivtStegstilstand = finnAktivStegstilstand(behandlingsstegstilstand)

        if (aktivtStegstilstand == null) {
            val nesteStegMetaData = finnNesteBehandlingsstegMedStatus(behandling, behandlingsstegstilstand)
            persisterBehandlingsstegOgStatus(behandlingId, nesteStegMetaData)
            if (nesteStegMetaData.behandlingsstegstatus == VENTER) {
                historikkTaskService
                    .lagHistorikkTask(
                        behandlingId = behandlingId,
                        historikkinnslagstype = TilbakekrevingHistorikkinnslagstype.BEHANDLING_PÅ_VENT,
                        aktør = Aktør.VEDTAKSLØSNING,
                        beskrivelse = nesteStegMetaData.venteårsak?.beskrivelse,
                    )
            }
        } else {
            log.info(
                "Behandling har allerede et aktivt steg=${aktivtStegstilstand.behandlingssteg} " +
                    "med status=${aktivtStegstilstand.behandlingsstegsstatus}",
            )
        }
    }

    @Transactional
    fun tilbakehoppBehandlingssteg(behandlingId: UUID, behandlingsstegsinfo: Behandlingsstegsinfo) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        if (behandling.erAvsluttet) {
            throw Feil(
                "Behandling med id=$behandlingId er allerede ferdig behandlet, " +
                    "så kan ikke forsette til ${behandlingsstegsinfo.behandlingssteg}",
            )
        }
        val behandlingsstegstilstand: List<Behandlingsstegstilstand> =
            behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        val aktivtBehandlingssteg = finnAktivStegstilstand(behandlingsstegstilstand)
            ?: throw Feil("Behandling med id=$behandlingId har ikke noe aktivt steg")
        // steg som kan behandles, kan avbrytes
        if (aktivtBehandlingssteg.behandlingssteg.kanSaksbehandles) {
            behandlingsstegstilstandRepository.update(aktivtBehandlingssteg.copy(behandlingsstegsstatus = AVBRUTT))
            persisterBehandlingsstegOgStatus(behandlingId, behandlingsstegsinfo)
        }
    }

    @Transactional
    fun tilbakeførBehandledeSteg(behandlingId: UUID) {
        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        val alleIkkeVentendeSteg = behandlingsstegstilstand.filter { it.behandlingsstegsstatus != VENTER }
            .filter { it.behandlingssteg !in listOf(Behandlingssteg.VARSEL, Behandlingssteg.GRUNNLAG) }
        alleIkkeVentendeSteg.forEach {
            log.info("Tilbakefører ${it.behandlingssteg} for behandling $behandlingId")
            oppdaterBehandlingsstegStatus(behandlingId, Behandlingsstegsinfo(it.behandlingssteg, TILBAKEFØRT))
        }
    }

    @Transactional
    fun behandleStegPåNytt(behandlingId: UUID, behandledeSteg: Behandlingssteg) {
        val aktivtBehandlingssteg = finnAktivtSteg(behandlingId)
            ?: throw Feil("Behandling med id=$behandlingId har ikke noe aktivt steg")

        if (behandledeSteg.sekvens < aktivtBehandlingssteg.sekvens) {
            for (i in aktivtBehandlingssteg.sekvens downTo behandledeSteg.sekvens + 1 step 1) {
                val behandlingssteg = Behandlingssteg.fraSekvens(i, sjekkOmBrevmottakerErstatterVergeForSekvens(i, behandlingId))
                oppdaterBehandlingsstegStatus(behandlingId, Behandlingsstegsinfo(behandlingssteg, TILBAKEFØRT))
            }
            oppdaterBehandlingsstegStatus(behandlingId, Behandlingsstegsinfo(behandledeSteg, KLAR))
        }
    }

    @Transactional
    fun behandleVergeSteg(behandlingId: UUID) {
        tilbakeførBehandledeSteg(behandlingId)
        log.info("Oppretter verge steg for behandling med id=$behandlingId")
        val eksisterendeVergeSteg = behandlingsstegstilstandRepository.findByBehandlingIdAndBehandlingssteg(
            behandlingId,
            Behandlingssteg.VERGE,
        )
        when {
            eksisterendeVergeSteg != null -> {
                oppdaterBehandlingsstegStatus(behandlingId, Behandlingsstegsinfo(Behandlingssteg.VERGE, KLAR))
            }
            else -> {
                opprettBehandlingsstegOgStatus(behandlingId, Behandlingsstegsinfo(Behandlingssteg.VERGE, KLAR))
            }
        }
    }

    @Transactional
    fun behandleBrevmottakerSteg(behandlingId: UUID) {
        log.info("Aktiverer brevmottaker steg for behandling med id=$behandlingId")
        behandlingsstegstilstandRepository.findByBehandlingIdAndBehandlingssteg(
            behandlingId,
            Behandlingssteg.BREVMOTTAKER,
        ) ?.apply {
            oppdaterBehandlingsstegStatus(behandlingId, Behandlingsstegsinfo(Behandlingssteg.BREVMOTTAKER, AUTOUTFØRT))
        } ?: opprettBehandlingsstegOgStatus(
            behandlingId = behandlingId,
            nesteStegMedStatus = Behandlingsstegsinfo(Behandlingssteg.BREVMOTTAKER, AUTOUTFØRT),
            opprettSendingAvBehandlingensTilstand = false, // da det settes AUTOUTFØRT, forblir aktivt steg / tilstanden den samme
        )
    }

    @Transactional
    fun settBehandlingPåVent(behandlingId: UUID, venteårsak: Venteårsak, tidsfrist: LocalDate) {
        val behandlingsstegstilstand: List<Behandlingsstegstilstand> =
            behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        val aktivtBehandlingsstegstilstand = finnAktivStegstilstand(behandlingsstegstilstand)
            ?: throw Feil(
                message = "Behandling $behandlingId " +
                    "har ikke aktivt steg",
                frontendFeilmelding = "Behandling $behandlingId " +
                    "har ikke aktivt steg",
            )
        behandlingsstegstilstandRepository.update(
            aktivtBehandlingsstegstilstand.copy(
                behandlingsstegsstatus = VENTER,
                venteårsak = venteårsak,
                tidsfrist = tidsfrist,
            ),
        )
        // oppdater tilsvarende behandlingsstatus
        oppdaterBehandlingsstatus(behandlingId, aktivtBehandlingsstegstilstand.behandlingssteg)

        historikkTaskService.lagHistorikkTask(
            behandlingId = behandlingId,
            historikkinnslagstype = TilbakekrevingHistorikkinnslagstype.BEHANDLING_PÅ_VENT,
            aktør = Aktør.SAKSBEHANDLER,
            beskrivelse = venteårsak.beskrivelse,
        )
    }

    @Transactional
    fun henleggBehandlingssteg(behandlingId: UUID) {
        val behandlingsstegstilstand: List<Behandlingsstegstilstand> =
            behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        behandlingsstegstilstand.filter { it.behandlingssteg != Behandlingssteg.VARSEL }
            .forEach {
                behandlingsstegstilstandRepository.update(it.copy(behandlingsstegsstatus = AVBRUTT))
            }
    }

    fun erBehandlingPåVent(behandlingId: UUID): Boolean {
        val behandlingsstegstilstand: List<Behandlingsstegstilstand> =
            behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        val aktivtBehandlingsstegstilstand: Behandlingsstegstilstand = finnAktivStegstilstand(behandlingsstegstilstand)
            ?: return false
        return VENTER == aktivtBehandlingsstegstilstand.behandlingsstegsstatus
    }

    fun hentBehandlingsstegstilstand(behandling: Behandling): List<Behandlingsstegsinfo> {
        val behandlingsstegstilstand: List<Behandlingsstegstilstand> =
            behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        return behandlingsstegstilstand.map {
            Behandlingsstegsinfo(
                behandlingssteg = it.behandlingssteg,
                behandlingsstegstatus = it.behandlingsstegsstatus,
                venteårsak = it.venteårsak,
                tidsfrist = it.tidsfrist,
            )
        }
    }

    fun finnAktivtSteg(behandlingId: UUID): Behandlingssteg? {
        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        return finnAktivStegstilstand(behandlingsstegstilstand)?.behandlingssteg
    }

    fun finnAktivStegstilstand(behandlingsstegstilstand: List<Behandlingsstegstilstand>): Behandlingsstegstilstand? {
        return behandlingsstegstilstand
            .firstOrNull { Behandlingsstegstatus.erStegAktiv(it.behandlingsstegsstatus) }
        // forutsetter at behandling kan ha kun et aktiv steg om gangen
    }

    fun finnAktivStegstilstand(behandlingId: UUID): Behandlingsstegstilstand? {
        return finnAktivStegstilstand(behandlingsstegstilstandRepository.findByBehandlingId(behandlingId))
    }

    @Transactional
    fun oppdaterBehandlingsstegStatus(behandlingId: UUID, behandlingsstegsinfo: Behandlingsstegsinfo) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        if (behandling.erAvsluttet && (
                behandlingsstegsinfo.behandlingssteg != Behandlingssteg.AVSLUTTET &&
                    behandlingsstegsinfo.behandlingsstegstatus != UTFØRT
                )
        ) {
            throw Feil(
                "Behandling med id=$behandlingId er allerede ferdig behandlet, " +
                    "så status=${behandlingsstegsinfo.behandlingsstegstatus} kan ikke oppdateres",
            )
        }
        val behandlingsstegstilstand =
            behandlingsstegstilstandRepository
                .findByBehandlingIdAndBehandlingssteg(behandlingId, behandlingsstegsinfo.behandlingssteg)
                ?: throw Feil(
                    message = "Behandling med id=$behandlingId og " +
                        "steg=${behandlingsstegsinfo.behandlingssteg} finnes ikke",
                )

        behandlingsstegstilstandRepository
            .update(
                behandlingsstegstilstand.copy(
                    behandlingsstegsstatus = behandlingsstegsinfo.behandlingsstegstatus,
                    venteårsak = behandlingsstegsinfo.venteårsak,
                    tidsfrist = behandlingsstegsinfo.tidsfrist,
                ),
            )

        // oppdater tilsvarende behandlingsstatus
        oppdaterBehandlingsstatus(behandlingId, behandlingsstegsinfo.behandlingssteg)
        behandlingTilstandService.opprettSendingAvBehandlingensTilstand(behandlingId, behandlingsstegsinfo)
    }

    private fun opprettBehandlingsstegOgStatus(
        behandlingId: UUID,
        nesteStegMedStatus: Behandlingsstegsinfo,
        opprettSendingAvBehandlingensTilstand: Boolean = true,
    ) {
        // startet nytt behandlingssteg
        behandlingsstegstilstandRepository
            .insert(
                Behandlingsstegstilstand(
                    behandlingId = behandlingId,
                    behandlingssteg = nesteStegMedStatus.behandlingssteg,
                    venteårsak = nesteStegMedStatus.venteårsak,
                    tidsfrist = nesteStegMedStatus.tidsfrist,
                    behandlingsstegsstatus = nesteStegMedStatus.behandlingsstegstatus,
                ),
            )
        // oppdater tilsvarende behandlingsstatus
        oppdaterBehandlingsstatus(behandlingId, nesteStegMedStatus.behandlingssteg)
        if (opprettSendingAvBehandlingensTilstand) {
            behandlingTilstandService.opprettSendingAvBehandlingensTilstand(behandlingId, nesteStegMedStatus)
        }
    }

    private fun persisterBehandlingsstegOgStatus(
        behandlingId: UUID,
        behandlingsstegsinfo: Behandlingsstegsinfo,
    ) {
        val gammelBehandlingsstegstilstand =
            behandlingsstegstilstandRepository.findByBehandlingIdAndBehandlingssteg(
                behandlingId,
                behandlingsstegsinfo.behandlingssteg,
            )
        when (gammelBehandlingsstegstilstand) {
            null -> {
                opprettBehandlingsstegOgStatus(behandlingId, behandlingsstegsinfo)
            }
            else -> {
                oppdaterBehandlingsstegStatus(behandlingId, behandlingsstegsinfo)
            }
        }
    }

    private fun finnNesteBehandlingsstegMedStatus(
        behandling: Behandling,
        stegstilstand: List<Behandlingsstegstilstand>,
    ): Behandlingsstegsinfo {
        if (stegstilstand.isEmpty()) {
            return when {
                // setter tidsfristen fra opprettelse dato
                kanSendeVarselsbrev(behandling) -> lagBehandlingsstegsinfo(
                    Behandlingssteg.VARSEL,
                    VENTER,
                    Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING,
                    behandling.opprettetDato,
                )
                !harAktivtGrunnlag(behandling) -> lagBehandlingsstegsinfo(
                    Behandlingssteg.GRUNNLAG,
                    VENTER,
                    Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG,
                    behandling.opprettetDato,
                )
                else -> lagBehandlingsstegsinfo(Behandlingssteg.FAKTA, KLAR)
            }
        }

        val finnesAvbruttSteg = stegstilstand.any { AVBRUTT == it.behandlingsstegsstatus }
        if (finnesAvbruttSteg) {
            // forutsetter behandling har et AVBRUTT steg om gangen
            val avbruttSteg = stegstilstand.first { AVBRUTT == it.behandlingsstegsstatus }
            return lagBehandlingsstegsinfo(avbruttSteg.behandlingssteg, KLAR)
        }

        val sisteUtførteSteg = stegstilstand.filter { Behandlingsstegstatus.erStegUtført(it.behandlingsstegsstatus) }
            .maxByOrNull { it.sporbar.endret.endretTid }!!.behandlingssteg

        if (Behandlingssteg.VARSEL == sisteUtførteSteg) {
            return håndterOmSisteUtførteStegErVarsel(behandling)
        }
        return lagBehandlingsstegsinfo(
            behandlingssteg = Behandlingssteg.finnNesteBehandlingssteg(
                behandlingssteg = sisteUtførteSteg,
                harVerge = behandling.harVerge,
                harManuelleBrevmottakere = brevmottakerRepository.findByBehandlingId(behandling.id).isNotEmpty(),
            ),
            KLAR,
        )
    }

    private fun håndterOmSisteUtførteStegErVarsel(behandling: Behandling): Behandlingsstegsinfo {
        return when {
            erKravgrunnlagSperret(behandling) -> {
                val kravgrunnlag = kravgrunnlagRepository
                    .findByBehandlingIdAndAktivIsTrueAndSperretTrue(behandling.id)

                // setter tidsfristen fra sperret dato
                lagBehandlingsstegsinfo(
                    behandlingssteg = Behandlingssteg.GRUNNLAG,
                    behandlingsstegstatus = VENTER,
                    venteårsak = Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG,
                    tidsfrist = kravgrunnlag.sporbar.endret.endretTid
                        .toLocalDate(),
                )
            }
            harAktivtGrunnlag(behandling) -> {
                if (behandling.harVerge) {
                    lagBehandlingsstegsinfo(behandlingssteg = Behandlingssteg.VERGE, behandlingsstegstatus = KLAR)
                } else {
                    lagBehandlingsstegsinfo(behandlingssteg = Behandlingssteg.FAKTA, behandlingsstegstatus = KLAR)
                }
            }
            // setter tidsfristen fra opprettelse dato
            else -> lagBehandlingsstegsinfo(
                behandlingssteg = Behandlingssteg.GRUNNLAG,
                behandlingsstegstatus = VENTER,
                venteårsak = Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG,
                tidsfrist = behandling.opprettetDato,
            )
        }
    }

    private fun kanSendeVarselsbrev(behandling: Behandling): Boolean {
        return Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL == behandling.aktivFagsystemsbehandling.tilbakekrevingsvalg &&
            !behandling.manueltOpprettet && !behandling.erRevurdering
    }

    private fun harAktivtGrunnlag(behandling: Behandling): Boolean {
        return kravgrunnlagRepository.existsByBehandlingIdAndAktivTrueAndSperretFalse(behandling.id)
    }

    private fun erKravgrunnlagSperret(behandling: Behandling): Boolean {
        return kravgrunnlagRepository.existsByBehandlingIdAndAktivTrueAndSperretTrue(behandling.id)
    }

    private fun lagBehandlingsstegsinfo(
        behandlingssteg: Behandlingssteg,
        behandlingsstegstatus: Behandlingsstegstatus,
        venteårsak: Venteårsak? = null,
        tidsfrist: LocalDate? = null,
    ): Behandlingsstegsinfo {
        return Behandlingsstegsinfo(
            behandlingssteg = behandlingssteg,
            behandlingsstegstatus = behandlingsstegstatus,
            venteårsak = venteårsak,
            tidsfrist = venteårsak?.defaultVenteTidIUker?.let { tidsfrist?.plusWeeks(it) },
        )
    }

    private fun oppdaterBehandlingsstatus(behandlingId: UUID, behandlingssteg: Behandlingssteg) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        // Oppdaterer tilsvarende behandlingsstatus bortsett fra Avsluttet steg. Det håndteres separat av AvsluttBehandlingTask
        if (Behandlingssteg.AVSLUTTET != behandlingssteg) {
            behandlingRepository.update(behandling.copy(status = behandlingssteg.behandlingsstatus))
        }
    }

    private fun sjekkOmBrevmottakerErstatterVergeForSekvens(sekvens: Int, behandlingId: UUID) =
        sekvens == Behandlingssteg.VERGE.sekvens && behandlingsstegstilstandRepository
            .findByBehandlingIdAndBehandlingssteg(behandlingId, Behandlingssteg.BREVMOTTAKER) != null
}
