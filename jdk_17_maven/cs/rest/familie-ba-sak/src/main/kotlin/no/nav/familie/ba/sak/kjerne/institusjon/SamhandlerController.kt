package no.nav.familie.ba.sak.kjerne.institusjon

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.http.client.RessursException
import no.nav.familie.kontrakter.ba.tss.SamhandlerInfo
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.HttpClientErrorException

@RestController
@RequestMapping("/api/samhandler")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class SamhandlerController(
    private val institusjonService: InstitusjonService,
) {

    @GetMapping(path = ["/orgnr/{orgnr}"])
    fun hentSamhandlerDataForOrganisasjon(
        @PathVariable("orgnr") orgNummer: String,
    ): Ressurs<SamhandlerInfo> = try {
        Ressurs.success(institusjonService.hentSamhandler(orgNummer).copy(orgNummer = orgNummer))
    } catch (e: Exception) {
        if (e.erNotFound()) {
            throw FunksjonellFeil(
                "Finner ikke institusjon. Kontakt NØS for å opprette TSS-ident.",
                httpStatus = HttpStatus.NOT_FOUND,
                throwable = e,
            )
        }
        throw e
    }

    fun Exception.erNotFound() = (this is RessursException && httpStatus == HttpStatus.NOT_FOUND) ||
        (this is HttpClientErrorException && statusCode == HttpStatus.NOT_FOUND)

    @PostMapping(path = ["/navn"])
    fun søkSamhandlerinfoFraNavn(
        @RequestBody request: SøkSamhandlerInfoRequest,
    ): Ressurs<List<SamhandlerInfo>> {
        if (request.navn == null && request.postnummer == null && request.område == null) {
            throw FunksjonellFeil(
                "Påkrevd variabel for søk er navn, postnummer eller område",
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }
        return Ressurs.success(
            institusjonService.søkSamhandlere(
                request.navn?.uppercase(),
                request.postnummer,
                request.område,
            ),
        )
    }
}

data class SøkSamhandlerInfoRequest(
    val navn: String?,
    val postnummer: String?,
    val område: String?,
)
