package no.nav.familie.ba.sak.kjerne.behandling

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.RessursUtils.illegalState
import no.nav.familie.ba.sak.common.RessursUtils.ok
import no.nav.familie.ba.sak.config.AuditLoggerEvent
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.kjerne.behandling.behandlingstema.BehandlingstemaService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseValideringService
import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.ba.sak.task.BehandleFødselshendelseTask
import no.nav.familie.ba.sak.task.dto.BehandleFødselshendelseTaskDTO
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/behandlinger")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class BehandlingController(
    private val stegService: StegService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val behandlingstemaService: BehandlingstemaService,
    private val taskRepository: TaskRepositoryWrapper,
    private val tilgangService: TilgangService,
    private val utvidetBehandlingService: UtvidetBehandlingService,
    private val tilkjentYtelseValideringService: TilkjentYtelseValideringService,
) {

    @PostMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun opprettBehandling(@RequestBody nyBehandling: NyBehandling): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        tilgangService.validerTilgangTilPersoner(
            personIdenter = listOf(nyBehandling.søkersIdent),
            event = AuditLoggerEvent.CREATE,
        )
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "opprette behandling",
        )
        // Basert på hvilke personer som ble hentet inn på behandlingen kan saksbehandler ha mistet tilgangen til behandlingen
        val behandling = stegService.håndterNyBehandlingOgSendInfotrygdFeed(nyBehandling)

        // Basert på hvilke personer som ble hentet inn på behandlingen kan saksbehandler ha mistet tilgangen til behandlingen
        tilgangService.validerTilgangTilBehandling(behandlingId = behandling.id, AuditLoggerEvent.UPDATE)
        return ResponseEntity.ok(
            Ressurs.success(
                utvidetBehandlingService
                    .lagRestUtvidetBehandling(behandlingId = behandling.id),
            ),
        )
    }

    @PutMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun opprettEllerOppdaterBehandlingFraHendelse(
        @RequestBody
        nyBehandling: NyBehandlingHendelse,
    ): ResponseEntity<Ressurs<String>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SYSTEM,
            handling = "opprette behandling fra hendelse",
        )

        return try {
            val task = BehandleFødselshendelseTask.opprettTask(BehandleFødselshendelseTaskDTO(nyBehandling))
            taskRepository.save(task)
            ok("Task opprettet for behandling av fødselshendelse.")
        } catch (ex: Throwable) {
            illegalState("Task kunne ikke opprettes for behandling av fødselshendelse: ${ex.message}", ex)
        }
    }

    @PutMapping(path = ["/{behandlingId}/behandlingstema"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun endreBehandlingstema(
        @PathVariable behandlingId: Long,
        @RequestBody
        endreBehandling: RestEndreBehandlingstema,
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.UPDATE)
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "endre behandlingstema",
        )
        tilgangService.validerKanRedigereBehandling(behandlingId)

        val behandling = behandlingstemaService.oppdaterBehandlingstema(
            behandling = behandlingHentOgPersisterService.hent(behandlingId),
            overstyrtUnderkategori = endreBehandling.behandlingUnderkategori,
            overstyrtKategori = endreBehandling.behandlingKategori,
            manueltOppdatert = true,
        )

        return ResponseEntity.ok(
            Ressurs.success(
                utvidetBehandlingService
                    .lagRestUtvidetBehandling(behandlingId = behandling.id),
            ),
        )
    }

    @GetMapping(path = ["/{behandlingId}/personer-med-ugyldig-etterbetalingsperiode"])
    fun hentPersonerMedUgyldigEtterbetalingsperiode(
        @PathVariable behandlingId: Long,
    ): ResponseEntity<Ressurs<List<String>>> {
        tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.ACCESS)
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
            handling = "hent gyldig etterbetaling",
        )

        val aktørerMedUgyldigEtterbetalingsperiode =
            tilkjentYtelseValideringService.finnAktørerMedUgyldigEtterbetalingsperiode(
                behandlingId = behandlingId,
            )
        val personerMedUgyldigEtterbetalingsperiode =
            aktørerMedUgyldigEtterbetalingsperiode.map { it.aktivFødselsnummer() }

        return ResponseEntity.ok(Ressurs.success(personerMedUgyldigEtterbetalingsperiode))
    }
}

data class NyBehandling(
    val kategori: BehandlingKategori? = null,
    val underkategori: BehandlingUnderkategori? = null,
    val søkersIdent: String,
    val behandlingType: BehandlingType,
    val behandlingÅrsak: BehandlingÅrsak = BehandlingÅrsak.SØKNAD,
    val skalBehandlesAutomatisk: Boolean = false,
    val navIdent: String? = null,
    val barnasIdenter: List<String> = emptyList(),
    val nyMigreringsdato: LocalDate? = null,
    val søknadMottattDato: LocalDate? = null,
    val søknadsinfo: Søknadsinfo? = null,
    val fagsakId: Long,
) {

    init { // Initiell validering på request
        when {
            søkersIdent.isBlank() -> throw Feil(
                message = "Søkers ident kan ikke være blank",
                frontendFeilmelding = "Klarte ikke å opprette behandling. Mangler ident på bruker.",
            )
            BehandlingType.MIGRERING_FRA_INFOTRYGD == behandlingType &&
                behandlingÅrsak.erManuellMigreringsårsak() &&
                nyMigreringsdato == null -> {
                throw FunksjonellFeil(
                    melding = "Du må sette ny migreringsdato før du kan fortsette videre",
                    frontendFeilmelding = "Du må sette ny migreringsdato før du kan fortsette videre",
                )
            }
            behandlingType in listOf(BehandlingType.FØRSTEGANGSBEHANDLING, BehandlingType.REVURDERING) &&
                behandlingÅrsak == BehandlingÅrsak.SØKNAD &&
                søknadMottattDato == null -> {
                throw FunksjonellFeil(
                    melding = "Du må sette søknads mottatt dato før du kan fortsette videre",
                    frontendFeilmelding = "Du må sette søknads mottatt dato før du kan fortsette videre",
                )
            }
        }
    }
}

data class NyBehandlingHendelse(
    val morsIdent: String,
    val barnasIdenter: List<String>,
)

data class RestEndreBehandlingstema(
    val behandlingUnderkategori: BehandlingUnderkategori,
    val behandlingKategori: BehandlingKategori,
)

data class Søknadsinfo(
    val journalpostId: String,
    val brevkode: String,
    val erDigital: Boolean,
)
