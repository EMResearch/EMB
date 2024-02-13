package no.nav.familie.tilbake.oppgave

import io.micrometer.core.instrument.Metrics
import no.nav.familie.kontrakter.felles.oppgave.Behandlingstype
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveRequest
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveResponseDto
import no.nav.familie.kontrakter.felles.oppgave.IdentGruppe
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.OppgaveIdentV2
import no.nav.familie.kontrakter.felles.oppgave.OppgavePrioritet
import no.nav.familie.kontrakter.felles.oppgave.OppgaveResponse
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.integration.familie.IntegrasjonerClient
import no.nav.familie.tilbake.person.PersonService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
class OppgaveService(
    private val behandlingRepository: BehandlingRepository,
    private val fagsakRepository: FagsakRepository,
    private val integrasjonerClient: IntegrasjonerClient,
    private val personService: PersonService,
    private val taskService: TaskService,
    private val environment: Environment,
) {

    private val logger = LoggerFactory.getLogger(this::class.java)
    private val secureLogger: Logger = LoggerFactory.getLogger("secureLogger")

    private val antallOppgaveTyper = Oppgavetype.values().associateWith {
        Metrics.counter("oppgave.opprettet", "type", it.name)
    }

    fun finnOppgaveForBehandlingUtenOppgaveType(behandlingId: UUID): Oppgave {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)

        val finnOppgaveRequest = FinnOppgaveRequest(
            behandlingstype = Behandlingstype.Tilbakekreving,
            saksreferanse = behandling.eksternBrukId.toString(),
            tema = fagsak.ytelsestype.tilTema(),
        )
        val finnOppgaveResponse = integrasjonerClient.finnOppgaver(finnOppgaveRequest)
        when {
            finnOppgaveResponse.oppgaver.size > 1 -> {
                secureLogger.error(
                    "Mer enn en oppgave åpen for behandling ${behandling.eksternBrukId}, " +
                        "$finnOppgaveRequest, $finnOppgaveResponse",
                )
                throw Feil("Har mer enn en åpen oppgave for behandling ${behandling.eksternBrukId}")
            }

            finnOppgaveResponse.oppgaver.isEmpty() -> {
                secureLogger.error(
                    "Fant ingen oppgave for behandling ${behandling.eksternBrukId} på fagsak ${fagsak.eksternFagsakId}, " +
                        "$finnOppgaveRequest, $finnOppgaveResponse",
                )
                throw Feil("Fant ingen oppgave for behandling ${behandling.eksternBrukId} på fagsak ${fagsak.eksternFagsakId}. Oppgaven kan være manuelt lukket.")
            }

            else -> {
                return finnOppgaveResponse.oppgaver.first()
            }
        }
    }

    fun opprettOppgave(
        behandlingId: UUID,
        oppgavetype: Oppgavetype,
        enhet: String,
        beskrivelse: String?,
        fristForFerdigstillelse: LocalDate,
        saksbehandler: String?,
        prioritet: OppgavePrioritet,
    ): OppgaveResponse {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val fagsakId = behandling.fagsakId
        val fagsak = fagsakRepository.findByIdOrThrow(fagsakId)
        val aktørId = personService.hentAktivAktørId(fagsak.bruker.ident, fagsak.fagsystem)

        // Sjekk om oppgave allerede finnes for behandling
        val (_, finnOppgaveRespons) = finnOppgave(behandling, oppgavetype, fagsak)
        if (finnOppgaveRespons.oppgaver.isNotEmpty() && !finnesFerdigstillOppgaveForBehandling(behandlingId, oppgavetype)) {
            throw Feil(
                "Det finnes allerede en oppgave $oppgavetype for behandling $behandlingId og " +
                    "finnes ikke noen ferdigstilleoppgaver. Eksisterende oppgaven $oppgavetype må lukke først.",
            )
        }

        val opprettOppgave = OpprettOppgaveRequest(
            ident = OppgaveIdentV2(
                ident = aktørId,
                gruppe = IdentGruppe.AKTOERID,
            ),
            saksId = behandling.eksternBrukId.toString(),
            tema = fagsak.ytelsestype.tilTema(),
            oppgavetype = oppgavetype,
            behandlesAvApplikasjon = "familie-tilbake",
            fristFerdigstillelse = fristForFerdigstillelse,
            beskrivelse = lagOppgaveTekst(
                fagsak.eksternFagsakId,
                behandling.eksternBrukId.toString(),
                fagsak.fagsystem.name,
                beskrivelse,
            ),
            enhetsnummer = behandling.behandlendeEnhet,
            tilordnetRessurs = saksbehandler,
            behandlingstype = Behandlingstype.Tilbakekreving.value,
            behandlingstema = null,
            mappeId = finnAktuellMappe(enhet, oppgavetype),
            prioritet = prioritet,
        )

        val opprettetOppgaveId = integrasjonerClient.opprettOppgave(opprettOppgave)

        antallOppgaveTyper[oppgavetype]!!.increment()

        return opprettetOppgaveId
    }

    private fun finnAktuellMappe(enhetsnummer: String?, oppgavetype: Oppgavetype): Long? {
        if (enhetsnummer == NAY_ENSLIG_FORSØRGER) {
            val søkemønster = lagSøkeuttrykk(oppgavetype) ?: return null
            val mapper = integrasjonerClient.finnMapper(enhetsnummer)

            val mappeIdForOppgave = mapper.find { it.navn.matches(søkemønster) }?.id?.toLong()
            mappeIdForOppgave?.let {
                logger.info("Legger oppgave i Godkjenne vedtak-mappe")
            } ?: logger.error("Fant ikke mappe for oppgavetype = $oppgavetype")

            return mappeIdForOppgave
        }
        return null
    }

    private fun lagSøkeuttrykk(oppgavetype: Oppgavetype): Regex? {
        val pattern = when (oppgavetype) {
            Oppgavetype.BehandleSak, Oppgavetype.BehandleUnderkjentVedtak -> "50 Tilbakekreving?.+"
            Oppgavetype.GodkjenneVedtak -> "70 Godkjenne?.vedtak?.+"
            else -> {
                logger.error("Ukjent oppgavetype = $oppgavetype")
                return null
            }
        }
        return Regex(pattern, RegexOption.IGNORE_CASE)
    }

    fun patchOppgave(patchOppgave: Oppgave): OppgaveResponse {
        return integrasjonerClient.patchOppgave(patchOppgave)
    }

    fun tilordneOppgaveNyEnhet(oppgaveId: Long, nyEnhet: String, fjernMappeFraOppgave: Boolean): OppgaveResponse {
        return integrasjonerClient.tilordneOppgaveNyEnhet(oppgaveId, nyEnhet, fjernMappeFraOppgave)
    }

    fun ferdigstillOppgave(behandlingId: UUID, oppgavetype: Oppgavetype?) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        val (finnOppgaveRequest, finnOppgaveResponse) = finnOppgave(behandling, oppgavetype, fagsak)

        when {
            finnOppgaveResponse.oppgaver.size > 1 -> {
                secureLogger.error(
                    "Mer enn en oppgave åpen for behandling ${behandling.eksternBrukId}, " +
                        "$finnOppgaveRequest, $finnOppgaveResponse",
                )
                throw Feil("Har mer enn en åpen oppgave for behandling ${behandling.eksternBrukId}")
            }

            finnOppgaveResponse.oppgaver.isEmpty() -> {
                logger.error("Fant ingen oppgave å ferdigstille for behandling ${behandling.eksternBrukId}")
                secureLogger.error(
                    "Fant ingen oppgave å ferdigstille ${behandling.eksternBrukId}, " +
                        "$finnOppgaveRequest, $finnOppgaveResponse",
                )
            }

            else -> {
                integrasjonerClient.ferdigstillOppgave(finnOppgaveResponse.oppgaver[0].id!!)
            }
        }
    }

    private fun finnOppgave(
        behandling: Behandling,
        oppgavetype: Oppgavetype?,
        fagsak: Fagsak,
    ): Pair<FinnOppgaveRequest, FinnOppgaveResponseDto> {
        val finnOppgaveRequest = FinnOppgaveRequest(
            behandlingstype = Behandlingstype.Tilbakekreving,
            saksreferanse = behandling.eksternBrukId.toString(),
            oppgavetype = oppgavetype,
            tema = fagsak.ytelsestype.tilTema(),
        )
        val finnOppgaveResponse = integrasjonerClient.finnOppgaver(finnOppgaveRequest)
        return Pair(finnOppgaveRequest, finnOppgaveResponse)
    }

    private fun lagOppgaveTekst(
        eksternFagsakId: String,
        eksternbrukBehandlingID: String,
        fagsystem: String,
        beskrivelse: String? = null,
    ): String {
        return if (beskrivelse != null) {
            beskrivelse + "\n"
        } else {
            ""
        } + "--- Opprettet av familie-tilbake ${LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)} --- \n" +
            "https://${lagFamilieTilbakeFrontendUrl()}/fagsystem/$fagsystem/fagsak/$eksternFagsakId/behandling/" +
            eksternbrukBehandlingID
    }

    private fun lagFamilieTilbakeFrontendUrl(): String {
        return if (environment.activeProfiles.contains("prod")) {
            "familietilbakekreving.intern.nav.no"
        } else {
            "familie-tilbake-frontend.intern.dev.nav.no"
        }
    }

    private fun finnesFerdigstillOppgaveForBehandling(behandlingId: UUID, oppgavetype: Oppgavetype): Boolean {
        val ubehandledeTasker = taskService.finnTasksMedStatus(
            status = listOf(
                Status.UBEHANDLET,
                Status.PLUKKET,
                Status.FEILET,
                Status.KLAR_TIL_PLUKK,
                Status.BEHANDLER,
            ),
            type = FerdigstillOppgaveTask.TYPE,
            page = Pageable.unpaged(),
        )
        return ubehandledeTasker.any {
            it.payload == behandlingId.toString() &&
                it.metadata.getProperty("oppgavetype") == oppgavetype.name
        }
    }

    companion object {

        private const val NAY_ENSLIG_FORSØRGER = "4489"
        private const val NAY_EGNE_ANSATTE = "4483"
    }
}
