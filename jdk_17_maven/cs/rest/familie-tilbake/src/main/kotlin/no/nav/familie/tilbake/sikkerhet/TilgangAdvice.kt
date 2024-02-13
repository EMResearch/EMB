package no.nav.familie.tilbake.sikkerhet

import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.api.dto.BehandlingsstegFatteVedtaksstegDto
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.FagsystemUtil
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.common.ContextService
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.config.RolleConfig
import no.nav.familie.tilbake.integration.familie.IntegrasjonerClient
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.kravgrunnlag.ØkonomiXmlMottattRepository
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.math.BigInteger
import java.util.UUID
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties

enum class HenteParam {
    BEHANDLING_ID,
    YTELSESTYPE_OG_EKSTERN_FAGSAK_ID,
    FAGSYSTEM_OG_EKSTERN_FAGSAK_ID,
    MOTTATT_XML_ID,
    EKSTERN_KRAVGRUNNLAG_ID,
    INGEN,
}

@Aspect
@Configuration
class TilgangAdvice(
    private val rolleConfig: RolleConfig,
    private val fagsakRepository: FagsakRepository,
    private val behandlingRepository: BehandlingRepository,
    private val kravgrunnlagRepository: KravgrunnlagRepository,
    private val auditLogger: AuditLogger,
    private val økonomiXmlMottattRepository: ØkonomiXmlMottattRepository,
    private val integrasjonerClient: IntegrasjonerClient,
) {

    private val feltnavnFagsystem = "fagsystem"
    private val feltnavnYtelsestype = "ytelsestype"
    private val feltnavnBehandlingId = "behandlingId"
    private val feltnavnEksternBrukId = "eksternBrukId"
    private val feltnavnEksternFagsakId = "eksternFagsakId"

    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    @Before("@annotation(rolletilgangssjekk) ")
    fun sjekkTilgang(joinpoint: JoinPoint, rolletilgangssjekk: Rolletilgangssjekk) {
        if (ContextService.hentSaksbehandler() == Constants.BRUKER_ID_VEDTAKSLØSNINGEN) {
            // når behandler har system tilgang, trenges ikke det validering på fagsystem eller rolle
            return
        }

        val httpRequest = (RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes).request

        if (HttpMethod.GET.matches(httpRequest.method) || rolletilgangssjekk.henteParam != HenteParam.INGEN) {
            validateFagsystemTilgangIGetRequest(
                rolletilgangssjekk.henteParam,
                joinpoint.args,
                rolletilgangssjekk,
            )
        } else if (HttpMethod.POST.matches(httpRequest.method) || HttpMethod.PUT.matches(httpRequest.method)) {
            validateFagsystemTilgangIPostRequest(
                joinpoint.args[0],
                rolletilgangssjekk,
            )
        } else {
            logger.error("${httpRequest.requestURI} støtter ikke tilgangssjekk")
        }
    }

    private fun validateFagsystemTilgangIGetRequest(
        param: HenteParam,
        requestBody: Array<Any>,
        rolletilgangssjekk: Rolletilgangssjekk,
    ) {
        when (param) {
            HenteParam.BEHANDLING_ID -> {
                val behandlingId = requestBody.first() as UUID
                val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
                val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)

                var behandlerRolle = rolletilgangssjekk.minimumBehandlerrolle
                if (requestBody.size > 1) {
                    behandlerRolle = bestemBehandlerRolleForUtførFatteVedtakSteg(
                        requestBody[1],
                        rolletilgangssjekk.minimumBehandlerrolle,
                    )
                }
                validate(
                    fagsystem = fagsak.fagsystem,
                    minimumBehandlerrolle = behandlerRolle,
                    fagsak = fagsak,
                    handling = rolletilgangssjekk.handling,
                )
                logAccess(rolletilgangssjekk, fagsak, behandling)
            }
            HenteParam.YTELSESTYPE_OG_EKSTERN_FAGSAK_ID -> {
                val ytelsestype = Ytelsestype.valueOf(requestBody.first().toString())
                val fagsystem = FagsystemUtil.hentFagsystemFraYtelsestype(ytelsestype)
                val eksternFagsakId = requestBody[1].toString()
                val fagsak = fagsakRepository.findByFagsystemAndEksternFagsakId(fagsystem, eksternFagsakId)

                validate(
                    fagsystem = fagsystem,
                    minimumBehandlerrolle = rolletilgangssjekk.minimumBehandlerrolle,
                    fagsak = fagsak,
                    handling = rolletilgangssjekk.handling,
                )
                logAccess(rolletilgangssjekk, fagsak)
            }
            HenteParam.FAGSYSTEM_OG_EKSTERN_FAGSAK_ID -> {
                val fagsystem = Fagsystem.valueOf(requestBody.first().toString())
                val eksternFagsakId = requestBody[1].toString()
                val fagsak = fagsakRepository.findByFagsystemAndEksternFagsakId(fagsystem, eksternFagsakId)

                validate(
                    fagsystem = fagsystem,
                    minimumBehandlerrolle = rolletilgangssjekk.minimumBehandlerrolle,
                    fagsak = fagsak,
                    handling = rolletilgangssjekk.handling,
                )
                logAccess(rolletilgangssjekk, fagsak)
            }
            HenteParam.MOTTATT_XML_ID -> {
                val mottattXmlId = requestBody.first() as UUID
                val økonomiXmlMottatt = økonomiXmlMottattRepository.findByIdOrThrow(mottattXmlId)

                validate(
                    fagsystem = FagsystemUtil.hentFagsystemFraYtelsestype(økonomiXmlMottatt.ytelsestype),
                    minimumBehandlerrolle = rolletilgangssjekk.minimumBehandlerrolle,
                    fagsak = null,
                    handling = rolletilgangssjekk.handling,
                )
            }
            HenteParam.EKSTERN_KRAVGRUNNLAG_ID -> {
                val eksternKravgrunnlagId = requestBody.first() as BigInteger
                val økonomiXmlMottatt = økonomiXmlMottattRepository.findByEksternKravgrunnlagId(eksternKravgrunnlagId)
                val kravgrunnlag = kravgrunnlagRepository.findByEksternKravgrunnlagIdAndAktivIsTrue(eksternKravgrunnlagId)
                if (økonomiXmlMottatt == null && kravgrunnlag == null) {
                    throw Feil(
                        message = "Finnes ikke eksternKravgrunnlagId=$eksternKravgrunnlagId",
                        httpStatus = HttpStatus.BAD_REQUEST,
                    )
                }
                val ytelsestype = økonomiXmlMottatt?.ytelsestype ?: kravgrunnlag!!.fagområdekode.ytelsestype

                validate(
                    fagsystem = FagsystemUtil.hentFagsystemFraYtelsestype(ytelsestype),
                    minimumBehandlerrolle = rolletilgangssjekk.minimumBehandlerrolle,
                    fagsak = null,
                    handling = rolletilgangssjekk.handling,
                )
            }
            else -> {
                kastTilgangssjekkException(rolletilgangssjekk.handling)
            }
        }
    }

    private fun validateFagsystemTilgangIPostRequest(
        requestBody: Any,
        rolletilgangssjekk: Rolletilgangssjekk,
    ) {
        val fields: Collection<KProperty1<out Any, *>> = requestBody::class.declaredMemberProperties

        val ytelsestypeFraRequest: KProperty1<out Any, *>? = fields.find { feltnavnYtelsestype == it.name }
        val fagsystemFraRequest = fields.find { feltnavnFagsystem == it.name }
        val behandlingIdFraRequest = fields.find { feltnavnBehandlingId == it.name }
        val eksternBrukIdFraRequest = fields.find { feltnavnEksternBrukId == it.name }
        val eksternFagsakIdFraRequest = fields.find { feltnavnEksternFagsakId == it.name }

        when {
            behandlingIdFraRequest != null -> {
                val behandlingId: UUID = behandlingIdFraRequest.getter.call(requestBody) as UUID
                val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
                val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)

                validate(
                    fagsystem = fagsak.fagsystem,
                    minimumBehandlerrolle = rolletilgangssjekk.minimumBehandlerrolle,
                    fagsak = fagsak,
                    handling = rolletilgangssjekk.handling,
                )
                logAccess(rolletilgangssjekk, fagsak, behandling)
            }
            eksternBrukIdFraRequest != null -> {
                val eksternBrukId: UUID = eksternBrukIdFraRequest.getter.call(requestBody) as UUID
                val fagsak = fagsakRepository.finnFagsakForEksternBrukId(eksternBrukId)

                validate(
                    fagsystem = fagsak.fagsystem,
                    minimumBehandlerrolle = rolletilgangssjekk.minimumBehandlerrolle,
                    fagsak = fagsak,
                    handling = rolletilgangssjekk.handling,
                )
                logAccess(rolletilgangssjekk, fagsak)
            }
            fagsystemFraRequest != null && eksternFagsakIdFraRequest != null -> {
                val fagsystem = fagsystemFraRequest.getter.call(requestBody) as Fagsystem
                val eksternFagsakId = eksternFagsakIdFraRequest.getter.call(requestBody).toString()
                val fagsak = fagsakRepository.findByFagsystemAndEksternFagsakId(fagsystem, eksternFagsakId)

                validate(
                    fagsystem = fagsystem,
                    minimumBehandlerrolle = rolletilgangssjekk.minimumBehandlerrolle,
                    fagsak = fagsak,
                    handling = rolletilgangssjekk.handling,
                )
                logAccess(rolletilgangssjekk, fagsak)
            }
            ytelsestypeFraRequest != null && eksternFagsakIdFraRequest != null -> {
                val ytelsestype = Ytelsestype.valueOf(ytelsestypeFraRequest.getter.call(requestBody).toString())
                val fagsystem = FagsystemUtil.hentFagsystemFraYtelsestype(ytelsestype)
                val eksternFagsakId = eksternFagsakIdFraRequest.getter.call(requestBody).toString()
                val fagsak = fagsakRepository.findByFagsystemAndEksternFagsakId(fagsystem, eksternFagsakId)

                validate(
                    fagsystem = fagsystem,
                    minimumBehandlerrolle = rolletilgangssjekk.minimumBehandlerrolle,
                    fagsak = fagsak,
                    handling = rolletilgangssjekk.handling,
                )
                logAccess(rolletilgangssjekk, fagsak)
            }
            ytelsestypeFraRequest != null -> {
                val ytelsestype = Ytelsestype.valueOf(ytelsestypeFraRequest.getter.call(requestBody).toString())

                validate(
                    fagsystem = FagsystemUtil.hentFagsystemFraYtelsestype(ytelsestype),
                    minimumBehandlerrolle = rolletilgangssjekk.minimumBehandlerrolle,
                    fagsak = null,
                    handling = rolletilgangssjekk.handling,
                )
            }
            else -> {
                kastTilgangssjekkException(rolletilgangssjekk.handling)
            }
        }
    }

    private fun validate(
        fagsystem: Fagsystem,
        minimumBehandlerrolle: Behandlerrolle,
        fagsak: Fagsak?,
        handling: String,
    ) {
        val brukerRolleOgFagsystemstilgang =
            ContextService.hentHøyesteRolletilgangOgYtelsestypeForInnloggetBruker(rolleConfig, handling)

        // når behandler har forvaltningstilgang, blir rollen bare validert
        if (brukerRolleOgFagsystemstilgang.tilganger.contains(Tilgangskontrollsfagsystem.FORVALTER_TILGANG)) {
            validateForvaltingsrolle(
                brukerRolleOgFagsystemstilgang = brukerRolleOgFagsystemstilgang,
                minimumBehandlerrolle = minimumBehandlerrolle,
                handling = handling,
            )
            return
        }
        val tilgangskontrollsfagsystem = Tilgangskontrollsfagsystem.fraFagsystem(fagsystem)
        // sjekk om saksbehandler har riktig gruppe å aksessere denne ytelsestypen
        validateFagsystem(tilgangskontrollsfagsystem, brukerRolleOgFagsystemstilgang, handling)

        // sjekk om saksbehandler har riktig rolle å aksessere denne ytelsestypen
        validateRolle(
            brukersrolleTilFagsystemet = brukerRolleOgFagsystemstilgang.tilganger.getValue(tilgangskontrollsfagsystem),
            minimumBehandlerrolle = minimumBehandlerrolle,
            handling = handling,
        )

        validateEgenAnsattKode6Kode7(
            fagsak = fagsak,
            handling = handling,
        )
    }

    fun logAccess(rolletilgangssjekk: Rolletilgangssjekk, fagsak: Fagsak?, behandling: Behandling? = null) {
        fagsak?.let {
            auditLogger.log(
                Sporingsdata(
                    rolletilgangssjekk.auditLoggerEvent,
                    fagsak.bruker.ident,
                    CustomKeyValue("eksternFagsakId", fagsak.eksternFagsakId),
                    behandling?.let {
                        CustomKeyValue("behandlingEksternBrukId", behandling.eksternBrukId.toString())
                    },
                ),
            )
        }
    }

    private fun validateEgenAnsattKode6Kode7(
        fagsak: Fagsak?,
        handling: String,
    ) {
        val personerIBehandlingen = fagsak?.bruker?.ident?.let { listOf(it) } ?: return
        val tilganger = integrasjonerClient.sjekkTilgangTilPersoner(personerIBehandlingen)
        if (tilganger.any { !it.harTilgang }) {
            throw Feil(
                message = "${ContextService.hentSaksbehandler()} har ikke tilgang til person i $handling",
                frontendFeilmelding = "${ContextService.hentSaksbehandler()}  har ikke tilgang til person i $handling",
                httpStatus = HttpStatus.FORBIDDEN,
            )
        }
    }

    private fun validateFagsystem(
        fagsystem: Tilgangskontrollsfagsystem,
        brukerRolleOgFagsystemstilgang: InnloggetBrukertilgang,
        handling: String,
    ) {
        if (!brukerRolleOgFagsystemstilgang.tilganger.contains(fagsystem)) {
            throw Feil(
                message = "${ContextService.hentSaksbehandler()} har ikke tilgang til $handling",
                frontendFeilmelding = "${ContextService.hentSaksbehandler()}  har ikke tilgang til $handling",
                httpStatus = HttpStatus.FORBIDDEN,
            )
        }
    }

    private fun validateRolle(
        brukersrolleTilFagsystemet: Behandlerrolle,
        minimumBehandlerrolle: Behandlerrolle,
        handling: String,
    ) {
        if (minimumBehandlerrolle == Behandlerrolle.FORVALTER) {
            throw Feil(
                message = "${ContextService.hentSaksbehandler()} med rolle $brukersrolleTilFagsystemet " +
                    "har ikke tilgang til å kalle forvaltningstjeneste $handling. Krever FORVALTER.",
                frontendFeilmelding = "Du har ikke tilgang til å $handling.",
                httpStatus = HttpStatus.FORBIDDEN,
            )
        }
        if (minimumBehandlerrolle.nivå > brukersrolleTilFagsystemet.nivå) {
            throw Feil(
                message = "${ContextService.hentSaksbehandler()} med rolle $brukersrolleTilFagsystemet " +
                    "har ikke tilgang til å $handling. Krever $minimumBehandlerrolle.",
                frontendFeilmelding = "Du har ikke tilgang til å $handling.",
                httpStatus = HttpStatus.FORBIDDEN,
            )
        }
    }

    private fun validateForvaltingsrolle(
        brukerRolleOgFagsystemstilgang: InnloggetBrukertilgang,
        minimumBehandlerrolle: Behandlerrolle,
        handling: String,
    ) {
        val tilganger = brukerRolleOgFagsystemstilgang.tilganger
        // Forvalter kan kun kalle forvaltningstjenestene og tjenestene som kan kalles av Veileder
        if (minimumBehandlerrolle.nivå > Behandlerrolle.FORVALTER.nivå &&
            tilganger.all { it.value == Behandlerrolle.FORVALTER }
        ) {
            throw Feil(
                message = "${ContextService.hentSaksbehandler()} med rolle FORVALTER " +
                    "har ikke tilgang til å $handling. Krever $minimumBehandlerrolle.",
                frontendFeilmelding = "Du har ikke tilgang til å $handling.",
                httpStatus = HttpStatus.FORBIDDEN,
            )
        }
    }

    private fun kastTilgangssjekkException(handling: String) {
        val feilmelding: String = "$handling kan ikke valideres for tilgangssjekk. " +
            "Det finnes ikke en av de påkrevde parameterne i request"
        throw Feil(
            message = feilmelding,
            frontendFeilmelding = feilmelding,
            httpStatus = HttpStatus.BAD_REQUEST,
        )
    }

    private fun bestemBehandlerRolleForUtførFatteVedtakSteg(
        requestBody: Any,
        minimumBehandlerrolle: Behandlerrolle,
    ): Behandlerrolle {
        // Behandlerrolle blir endret til Beslutter kun når FatteVedtak steg utføres
        if (requestBody is BehandlingsstegFatteVedtaksstegDto) {
            return Behandlerrolle.BESLUTTER
        }
        return minimumBehandlerrolle
    }
}
