package no.nav.familie.ba.sak.kjerne.verdikjedetester

import no.nav.familie.ba.sak.common.isSameOrBefore
import no.nav.familie.ba.sak.ekstern.restDomene.RestFagsak
import no.nav.familie.ba.sak.ekstern.restDomene.RestMinimalFagsak
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.ekstern.restDomene.RestVisningBehandling
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.brev.BrevmalService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.StatusFraOppdragMedTask
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.steg.domene.JournalførVedtaksbrevDTO
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Utbetalingsperiode
import no.nav.familie.ba.sak.task.BehandleFødselshendelseTask
import no.nav.familie.ba.sak.task.DistribuerDokumentDTO
import no.nav.familie.ba.sak.task.JournalførVedtaksbrevTask
import no.nav.familie.ba.sak.task.StatusFraOppdragTask
import no.nav.familie.ba.sak.task.dto.BehandleFødselshendelseTaskDTO
import no.nav.familie.ba.sak.task.dto.FAGSYSTEM
import no.nav.familie.ba.sak.task.dto.IverksettingTaskDTO
import no.nav.familie.ba.sak.task.dto.StatusFraOppdragDTO
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.prosessering.domene.Task
import org.junit.jupiter.api.Assertions.assertEquals
import java.time.LocalDate
import java.util.Properties

fun generellAssertRestUtvidetBehandling(
    restUtvidetBehandling: Ressurs<RestUtvidetBehandling>,
    behandlingStatus: BehandlingStatus,
    behandlingStegType: StegType? = null,
    behandlingsresultat: Behandlingsresultat? = null,
) {
    if (restUtvidetBehandling.status != Ressurs.Status.SUKSESS) {
        throw IllegalStateException("generellAssertRestUtvidetBehandling feilet. status: ${restUtvidetBehandling.status.name},  melding: ${restUtvidetBehandling.melding}")
    }

    assertEquals(behandlingStatus, restUtvidetBehandling.data?.status)

    if (behandlingStegType != null) {
        assertEquals(behandlingStegType, restUtvidetBehandling.data?.steg)
    }

    if (behandlingsresultat != null) {
        assertEquals(behandlingsresultat, restUtvidetBehandling.data?.resultat)
    }
}

fun generellAssertFagsak(
    restFagsak: Ressurs<RestFagsak>,
    fagsakStatus: FagsakStatus,
    behandlingStegType: StegType? = null,
    behandlingsresultat: Behandlingsresultat? = null,
    aktivBehandlingId: Long? = null,
) {
    if (restFagsak.status != Ressurs.Status.SUKSESS) throw IllegalStateException("generellAssertFagsak feilet. status: ${restFagsak.status.name},  melding: ${restFagsak.melding}")
    assertEquals(fagsakStatus, restFagsak.data?.status)

    val aktivBehandling = if (aktivBehandlingId == null) {
        hentAktivBehandling(restFagsak = restFagsak.data!!)
    } else {
        restFagsak.data!!.behandlinger.single { it.behandlingId == aktivBehandlingId }
    }

    if (behandlingStegType != null) {
        assertEquals(behandlingStegType, aktivBehandling.steg)
    }
    if (behandlingsresultat != null) {
        assertEquals(behandlingsresultat, aktivBehandling.resultat)
    }
}

fun assertUtbetalingsperiode(utbetalingsperiode: Utbetalingsperiode, antallBarn: Int, utbetaltPerMnd: Int) {
    assertEquals(antallBarn, utbetalingsperiode.utbetalingsperiodeDetaljer.size)
    assertEquals(utbetaltPerMnd, utbetalingsperiode.utbetaltPerMnd)
}

fun hentNåværendeEllerNesteMånedsUtbetaling(behandling: RestUtvidetBehandling): Int {
    val utbetalingsperioder =
        behandling.utbetalingsperioder.sortedBy { it.periodeFom }
    val nåværendeUtbetalingsperiode = utbetalingsperioder
        .firstOrNull { it.periodeFom.isSameOrBefore(LocalDate.now()) && it.periodeTom.isAfter(LocalDate.now()) }

    val nesteUtbetalingsperiode = utbetalingsperioder.firstOrNull { it.periodeFom.isAfter(LocalDate.now()) }

    return nåværendeUtbetalingsperiode?.utbetaltPerMnd ?: nesteUtbetalingsperiode?.utbetaltPerMnd ?: 0
}

fun hentAktivBehandling(restFagsak: RestFagsak): RestUtvidetBehandling {
    return restFagsak.behandlinger.single()
}

fun hentAktivBehandling(restMinimalFagsak: RestMinimalFagsak): RestVisningBehandling {
    return restMinimalFagsak.behandlinger.single { it.aktiv }
}

fun behandleFødselshendelse(
    nyBehandlingHendelse: NyBehandlingHendelse,
    fagsakStatusEtterVurdering: FagsakStatus = FagsakStatus.OPPRETTET,
    behandleFødselshendelseTask: BehandleFødselshendelseTask,
    fagsakService: FagsakService,
    behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    personidentService: PersonidentService,
    vedtakService: VedtakService,
    stegService: StegService,
    brevmalService: BrevmalService,
): Behandling? {
    val søkerFnr = nyBehandlingHendelse.morsIdent
    val søkerAktør = personidentService.hentAktør(søkerFnr)

    behandleFødselshendelseTask.doTask(
        BehandleFødselshendelseTask.opprettTask(
            BehandleFødselshendelseTaskDTO(
                nyBehandling = nyBehandlingHendelse,
            ),
        ),
    )

    val restMinimalFagsakEtterVurdering = fagsakService.hentMinimalFagsakForPerson(aktør = søkerAktør)
    if (restMinimalFagsakEtterVurdering.status != Ressurs.Status.SUKSESS) {
        return null
    }

    val behandlingEtterVurdering =
        behandlingHentOgPersisterService.hentBehandlinger(fagsakId = restMinimalFagsakEtterVurdering.data!!.id)
            .maxByOrNull { it.opprettetTidspunkt }!!
    if (behandlingEtterVurdering.erHenlagt()) {
        return behandlingEtterVurdering
    }

    generellAssertFagsak(
        restFagsak = fagsakService.hentRestFagsak(restMinimalFagsakEtterVurdering.data!!.id),
        fagsakStatus = fagsakStatusEtterVurdering,
        behandlingStegType = StegType.IVERKSETT_MOT_OPPDRAG,
        aktivBehandlingId = behandlingEtterVurdering.id,
    )

    return håndterIverksettingAvBehandling(
        behandlingEtterVurdering = behandlingEtterVurdering,
        søkerFnr = søkerFnr,
        fagsakService = fagsakService,
        vedtakService = vedtakService,
        stegService = stegService,
        brevmalService = brevmalService,
    )
}

fun håndterIverksettingAvBehandling(
    behandlingEtterVurdering: Behandling,
    søkerFnr: String,
    fagsakStatusEtterIverksetting: FagsakStatus = FagsakStatus.LØPENDE,
    fagsakService: FagsakService,
    vedtakService: VedtakService,
    stegService: StegService,
    brevmalService: BrevmalService,
): Behandling {
    val vedtak = vedtakService.hentAktivForBehandlingThrows(behandlingId = behandlingEtterVurdering.id)
    val behandlingEtterIverksetteVedtak =
        stegService.håndterIverksettMotØkonomi(
            behandlingEtterVurdering,
            IverksettingTaskDTO(
                behandlingsId = behandlingEtterVurdering.id,
                vedtaksId = vedtak.id,
                saksbehandlerId = "System",
                personIdent = behandlingEtterVurdering.fagsak.aktør.aktivFødselsnummer(),
            ),
        )

    val behandlingEtterStatusFraOppdrag =
        stegService.håndterStatusFraØkonomi(
            behandlingEtterIverksetteVedtak,
            StatusFraOppdragMedTask(
                statusFraOppdragDTO = StatusFraOppdragDTO(
                    fagsystem = FAGSYSTEM,
                    personIdent = søkerFnr,
                    aktørId = behandlingEtterVurdering.fagsak.aktør.aktørId,
                    behandlingsId = behandlingEtterIverksetteVedtak.id,
                    vedtaksId = vedtak.id,
                ),
                task = Task(type = StatusFraOppdragTask.TASK_STEP_TYPE, payload = ""),
            ),
        )

    val behandlingEtterIverksettTilbakekreving =
        if (behandlingEtterStatusFraOppdrag.steg == StegType.IVERKSETT_MOT_FAMILIE_TILBAKE) {
            stegService.håndterIverksettMotFamilieTilbake(
                behandling = behandlingEtterStatusFraOppdrag,
                metadata = Properties(),
            )
        } else {
            behandlingEtterStatusFraOppdrag
        }

    val behandlingSomSkalFerdigstilles =
        if (behandlingEtterIverksettTilbakekreving.steg == StegType.JOURNALFØR_VEDTAKSBREV) {
            val behandlingEtterJournalførtVedtak =
                stegService.håndterJournalførVedtaksbrev(
                    behandlingEtterStatusFraOppdrag,
                    JournalførVedtaksbrevDTO(
                        vedtakId = vedtak.id,
                        task = Task(type = JournalførVedtaksbrevTask.TASK_STEP_TYPE, payload = ""),
                    ),
                )

            val behandlingEtterDistribuertVedtak =
                stegService.håndterDistribuerVedtaksbrev(
                    behandlingEtterJournalførtVedtak,
                    DistribuerDokumentDTO(
                        behandlingId = behandlingEtterJournalførtVedtak.id,
                        journalpostId = "1234",
                        personEllerInstitusjonIdent = søkerFnr,
                        brevmal = brevmalService.hentBrevmal(
                            behandlingEtterJournalførtVedtak,
                        ),
                        erManueltSendt = false,
                    ),
                )
            behandlingEtterDistribuertVedtak
        } else {
            behandlingEtterStatusFraOppdrag
        }

    val ferdigstiltBehandling = stegService.håndterFerdigstillBehandling(behandlingSomSkalFerdigstilles)

    val restMinimalFagsakEtterAvsluttetBehandling =
        fagsakService.hentMinimalFagsakForPerson(aktør = ferdigstiltBehandling.fagsak.aktør)
    generellAssertFagsak(
        restFagsak = fagsakService.hentRestFagsak(restMinimalFagsakEtterAvsluttetBehandling.data!!.id),
        fagsakStatus = fagsakStatusEtterIverksetting,
        behandlingStegType = StegType.BEHANDLING_AVSLUTTET,
        aktivBehandlingId = hentAktivBehandling(
            restMinimalFagsakEtterAvsluttetBehandling.data!!,
        ).behandlingId,
    )

    return ferdigstiltBehandling
}
