package no.nav.familie.ba.sak.sikkerhet

import no.nav.familie.ba.sak.ekstern.restDomene.TilgangDTO
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.FamilieIntegrasjonerTilgangskontrollService
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
@ProtectedWithClaims(issuer = "azuread")
class TilgangController(
    private val personopplysningerService: PersonopplysningerService,
    private val personidentService: PersonidentService,
    private val familieIntegrasjonerTilgangskontrollService: FamilieIntegrasjonerTilgangskontrollService,
) {

    @PostMapping(path = ["tilgang"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentTilgangOgDiskresjonskode(@RequestBody tilgangRequestDTO: TilgangRequestDTO): ResponseEntity<Ressurs<TilgangDTO>> {
        val aktør = personidentService.hentAktør(tilgangRequestDTO.brukerIdent)

        val adressebeskyttelse = personopplysningerService.hentAdressebeskyttelseSomSystembruker(aktør)
        val tilgang = familieIntegrasjonerTilgangskontrollService.sjekkTilgangTilPerson(tilgangRequestDTO.brukerIdent)
        return ResponseEntity.ok(
            Ressurs.success(
                data = TilgangDTO(
                    saksbehandlerHarTilgang = tilgang.harTilgang,
                    adressebeskyttelsegradering = adressebeskyttelse,
                ),
            ),
        )
    }
}

class TilgangRequestDTO(val brukerIdent: String)
