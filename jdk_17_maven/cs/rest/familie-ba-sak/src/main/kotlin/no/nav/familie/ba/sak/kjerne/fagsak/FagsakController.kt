package no.nav.familie.ba.sak.kjerne.fagsak

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.RessursUtils.illegalState
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.config.AuditLoggerEvent
import no.nav.familie.ba.sak.ekstern.restDomene.InstitusjonInfo
import no.nav.familie.ba.sak.ekstern.restDomene.RestFagsak
import no.nav.familie.ba.sak.ekstern.restDomene.RestFagsakDeltager
import no.nav.familie.ba.sak.ekstern.restDomene.RestHentFagsakForPerson
import no.nav.familie.ba.sak.ekstern.restDomene.RestHentFagsakerForPerson
import no.nav.familie.ba.sak.ekstern.restDomene.RestMinimalFagsak
import no.nav.familie.ba.sak.ekstern.restDomene.RestSøkParam
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import no.nav.familie.ba.sak.kjerne.tilbakekreving.TilbakekrevingService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/fagsaker")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class FagsakController(
    private val fagsakService: FagsakService,
    private val personidentService: PersonidentService,
    private val tilgangService: TilgangService,
    private val tilbakekrevingService: TilbakekrevingService,
) {

    @PostMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentEllerOpprettFagsak(@RequestBody fagsakRequest: FagsakRequest): ResponseEntity<Ressurs<RestMinimalFagsak>> {
        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} henter eller oppretter ny fagsak")
        tilgangService.validerTilgangTilPersoner(
            personIdenter = listOf(fagsakRequest.personIdent),
            event = AuditLoggerEvent.CREATE,
        )
        tilgangService.verifiserHarTilgangTilHandling(BehandlerRolle.SAKSBEHANDLER, "opprette fagsak")

        return Result.runCatching { fagsakService.hentEllerOpprettFagsak(fagsakRequest) }
            .fold(
                onSuccess = { ResponseEntity.status(HttpStatus.CREATED).body(it) },
                onFailure = { throw it },
            )
    }

    @GetMapping(path = ["/{fagsakId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentRestFagsak(@PathVariable fagsakId: Long): ResponseEntity<Ressurs<RestFagsak>> {
        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} henter fagsak med id $fagsakId")
        tilgangService.validerTilgangTilFagsak(fagsakId = fagsakId, event = AuditLoggerEvent.ACCESS)

        val fagsak = fagsakService.hentRestFagsak(fagsakId)
        return ResponseEntity.ok().body(fagsak)
    }

    @GetMapping(path = ["/minimal/{fagsakId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentMinimalFagsak(@PathVariable fagsakId: Long): ResponseEntity<Ressurs<RestMinimalFagsak>> {
        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} henter minimal fagsak med id $fagsakId")
        tilgangService.validerTilgangTilFagsak(fagsakId = fagsakId, event = AuditLoggerEvent.ACCESS)

        val fagsak = fagsakService.hentRestMinimalFagsak(fagsakId)
        return ResponseEntity.ok().body(fagsak)
    }

    @PostMapping(path = ["/hent-fagsak-paa-person"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentMinimalFagsakForPerson(@RequestBody request: RestHentFagsakForPerson): ResponseEntity<Ressurs<RestMinimalFagsak>> {
        tilgangService.validerTilgangTilPersoner(
            personIdenter = listOf(request.personIdent),
            event = AuditLoggerEvent.ACCESS,
        )

        return Result.runCatching {
            val aktør = personidentService.hentAktør(request.personIdent)
            fagsakService.hentMinimalFagsakForPerson(aktør, request.fagsakType)
        }.fold(
            onSuccess = { return ResponseEntity.ok().body(it) },
            onFailure = { illegalState("Ukjent feil ved henting data for manuell journalføring.", it) },
        )
    }

    @PostMapping(path = ["/hent-fagsaker-paa-person"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentMinimalFagsakerForPerson(
        @RequestBody
        request: RestHentFagsakerForPerson,
    ): ResponseEntity<Ressurs<List<RestMinimalFagsak>>> {
        tilgangService.validerTilgangTilPersoner(
            personIdenter = listOf(request.personIdent),
            event = AuditLoggerEvent.ACCESS,
        )

        return Result.runCatching {
            val aktør = personidentService.hentAktør(request.personIdent)
            fagsakService.hentMinimalFagsakerForPerson(aktør = aktør, fagsakTyper = request.fagsakTyper)
        }.fold(
            onSuccess = { return ResponseEntity.ok().body(it) },
            onFailure = { illegalState("Ukjent feil ved henting data for manuell journalføring.", it) },
        )
    }

    @PostMapping(path = ["/sok"])
    fun søkFagsak(@RequestBody søkParam: RestSøkParam): ResponseEntity<Ressurs<List<RestFagsakDeltager>>> {
        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} søker fagsak")

        val fagsakDeltagere = fagsakService.hentFagsakDeltager(søkParam.personIdent)
        return ResponseEntity.ok().body(Ressurs.success(fagsakDeltagere))
    }

    @PostMapping(path = ["/sok/fagsaker-hvor-person-er-deltaker"])
    fun søkFagsakerHvorPersonErSøkerEllerMottarOrdinærBarnetrygd(@RequestBody request: RestSøkFagsakRequest): ResponseEntity<Ressurs<List<RestFagsakIdOgTilknyttetAktørId>>> {
        tilgangService.validerTilgangTilPersoner(
            personIdenter = listOf(request.personIdent),
            event = AuditLoggerEvent.ACCESS,
        )

        val aktør = personidentService.hentAktør(request.personIdent)

        val fagsakerHvorAktørErSøkerEllerMottarLøpendeOrdinær =
            fagsakService.finnAlleFagsakerHvorAktørErSøkerEllerMottarLøpendeOrdinær(aktør)

        val fagsakIdOgTilknyttetAktørId = fagsakerHvorAktørErSøkerEllerMottarLøpendeOrdinær.map {
            RestFagsakIdOgTilknyttetAktørId(aktørId = it.aktør.aktørId, fagsakId = it.id)
        }

        return ResponseEntity.ok().body(Ressurs.success(fagsakIdOgTilknyttetAktørId))
    }

    @PostMapping(path = ["/sok/fagsaker-hvor-person-mottar-lopende-ytelse"])
    fun søkFagsakerHvorPersonMottarLøpendeYtelse(@RequestBody request: RestSøkFagsakRequest): ResponseEntity<Ressurs<List<RestFagsakIdOgTilknyttetAktørId>>> {
        tilgangService.validerTilgangTilPersoner(
            personIdenter = listOf(request.personIdent),
            event = AuditLoggerEvent.ACCESS,
        )

        val aktør = personidentService.hentAktør(request.personIdent)

        val fagsakerHvorAktørMottarLøpendeUtvidetEllerOrdinær =
            fagsakService.finnAlleFagsakerHvorAktørHarLøpendeYtelseAvType(
                aktør = aktør,
                ytelseTyper = listOf(YtelseType.ORDINÆR_BARNETRYGD, YtelseType.UTVIDET_BARNETRYGD),
            )

        val fagsakIdOgTilknyttetAktørId = fagsakerHvorAktørMottarLøpendeUtvidetEllerOrdinær.map {
            RestFagsakIdOgTilknyttetAktørId(aktørId = it.aktør.aktørId, fagsakId = it.id)
        }

        return ResponseEntity.ok().body(Ressurs.success(fagsakIdOgTilknyttetAktørId))
    }

    data class RestSøkFagsakRequest(val personIdent: String)
    data class RestFagsakIdOgTilknyttetAktørId(
        val aktørId: String,
        val fagsakId: Long,
    )

    @PostMapping(path = ["/sok/fagsakdeltagere"])
    fun oppgiFagsakdeltagere(@RequestBody restSøkParam: RestSøkParam): ResponseEntity<Ressurs<List<RestFagsakDeltager>>> {
        return Result.runCatching {
            val aktør = personidentService.hentAktør(restSøkParam.personIdent)
            val barnsAktørId = personidentService.hentAktørIder(restSøkParam.barnasIdenter)

            fagsakService.oppgiFagsakdeltagere(aktør, barnsAktørId)
        }
            .fold(
                onSuccess = { ResponseEntity.ok(Ressurs.success(it)) },
                onFailure = {
                    logger.info("Henting av fagsakdeltagere feilet.")
                    secureLogger.info("Henting av fagsakdeltagere feilet: ${it.message}", it)
                    ResponseEntity
                        .status(if (it is Feil) it.httpStatus else HttpStatus.OK)
                        .body(
                            Ressurs.failure(
                                error = it,
                                errorMessage = "Henting av fagsakdeltagere feilet: ${it.message}",
                            ),
                        )
                },
            )
    }

    @GetMapping(path = ["/{fagsakId}/har-apen-tilbakekreving"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun harÅpenTilbakekreving(@PathVariable fagsakId: Long): ResponseEntity<Ressurs<Boolean>> {
        return ResponseEntity.ok(
            Ressurs.success(tilbakekrevingService.søkerHarÅpenTilbakekreving(fagsakId)),
        )
    }

    @GetMapping(path = ["/{fagsakId}/opprett-tilbakekreving"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun opprettTilbakekrevingsbehandling(@PathVariable fagsakId: Long): Ressurs<String> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "opprette tilbakekrevingbehandling",
        )

        return tilbakekrevingService.opprettTilbakekrevingsbehandlingManuelt(fagsakId)
    }

    companion object {

        private val logger: Logger = LoggerFactory.getLogger(FagsakController::class.java)
    }
}

data class FagsakRequest(
    val personIdent: String,
    val fagsakType: FagsakType? = FagsakType.NORMAL,
    val institusjon: InstitusjonInfo? = null,
)

data class RestBeslutningPåVedtak(
    val beslutning: Beslutning,
    val begrunnelse: String? = null,
    val kontrollerteSider: List<String> = emptyList(),
)

enum class Beslutning {
    GODKJENT,
    UNDERKJENT,
    ;

    fun erGodkjent() = this == GODKJENT
}
