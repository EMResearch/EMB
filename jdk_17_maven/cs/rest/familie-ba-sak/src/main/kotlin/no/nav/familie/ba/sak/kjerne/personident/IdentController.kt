package no.nav.familie.ba.sak.kjerne.personident

import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/ident")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class IdentController(
    private val personidentService: PersonidentService,
) {

    @PostMapping
    fun håndterPdlHendelse(@RequestBody nyIdent: PersonIdent): ResponseEntity<Ressurs<String>> {
        personidentService.opprettTaskForIdentHendelse(nyIdent)
        return ResponseEntity.ok(Ressurs.success("Håndtert ny ident"))
    }
}
