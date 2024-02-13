package no.nav.familie.ba.sak.integrasjoner.infotrygd

import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.kontrakter.ba.infotrygd.Sak
import no.nav.familie.kontrakter.ba.infotrygd.Stønad
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/infotrygd")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class InfotrygdController(
    private val infotrygdBarnetrygdClient: InfotrygdBarnetrygdClient,
    private val personidentService: PersonidentService,
    private val infotrygdService: InfotrygdService,
) {

    @PostMapping(path = ["/hent-infotrygdsaker-for-soker"])
    fun hentInfotrygdsakerForSøker(@RequestBody personIdent: Personident): ResponseEntity<Ressurs<RestInfotrygdsaker>> {
        val aktør = personidentService.hentAktør(personIdent.ident)
        val infotrygdsaker = infotrygdService.hentMaskertRestInfotrygdsakerVedManglendeTilgang(aktør)
            ?: RestInfotrygdsaker(infotrygdService.hentInfotrygdsakerForSøker(aktør).bruker)

        return ResponseEntity.ok(Ressurs.success(infotrygdsaker))
    }

    @PostMapping(path = ["/hent-infotrygdstonader-for-soker"])
    fun hentInfotrygdstønaderForSøker(@RequestBody personIdent: Personident): ResponseEntity<Ressurs<RestInfotrygdstønader>> {
        val aktør = personidentService.hentAktør(personIdent.ident)
        val infotrygdstønader = infotrygdService.hentMaskertRestInfotrygdstønaderVedManglendeTilgang(aktør)
            ?: RestInfotrygdstønader(infotrygdService.hentInfotrygdstønaderForSøker(personIdent.ident).bruker)

        return ResponseEntity.ok(Ressurs.success(infotrygdstønader))
    }

    @PostMapping(path = ["/har-lopende-sak"])
    fun harLøpendeSak(@RequestBody personIdent: Personident): ResponseEntity<Ressurs<RestLøpendeSak>> {
        val harLøpendeSak = infotrygdBarnetrygdClient.harLøpendeSakIInfotrygd(listOf(personIdent.ident))
        return ResponseEntity.ok(Ressurs.success(RestLøpendeSak(harLøpendeSak)))
    }
}

class Personident(val ident: String)

class RestInfotrygdsaker(
    val saker: List<Sak> = emptyList(),
    val adressebeskyttelsegradering: ADRESSEBESKYTTELSEGRADERING? = null,
    val harTilgang: Boolean = true,
)

class RestInfotrygdstønader(
    val stønader: List<Stønad> = emptyList(),
    val adressebeskyttelsegradering: ADRESSEBESKYTTELSEGRADERING? = null,
    val harTilgang: Boolean = true,
)

class RestLøpendeSak(val harLøpendeSak: Boolean)
