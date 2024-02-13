package no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner

import no.nav.familie.ba.sak.config.hentCacheForSaksbehandler
import no.nav.familie.ba.sak.ekstern.restDomene.RestPersonInfo
import no.nav.familie.ba.sak.integrasjoner.pdl.SystemOnlyPdlRestClient
import no.nav.familie.ba.sak.integrasjoner.pdl.tilAdressebeskyttelse
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.kontrakter.felles.tilgangskontroll.Tilgang
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Service

@Service
class FamilieIntegrasjonerTilgangskontrollService(
    private val familieIntegrasjonerTilgangskontrollClient: FamilieIntegrasjonerTilgangskontrollClient,
    private val cacheManager: CacheManager,
    private val systemOnlyPdlRestClient: SystemOnlyPdlRestClient,
) {

    fun hentMaskertPersonInfoVedManglendeTilgang(aktør: Aktør): RestPersonInfo? {
        val harTilgang = sjekkTilgangTilPerson(personIdent = aktør.aktivFødselsnummer()).harTilgang
        return if (!harTilgang) {
            val adressebeskyttelse = systemOnlyPdlRestClient.hentAdressebeskyttelse(aktør).tilAdressebeskyttelse()
            RestPersonInfo(
                personIdent = aktør.aktivFødselsnummer(),
                adressebeskyttelseGradering = adressebeskyttelse,
                harTilgang = false,
            )
        } else {
            null
        }
    }

    fun sjekkTilgangTilPerson(personIdent: String): Tilgang {
        return sjekkTilgangTilPersoner(listOf(personIdent)).values.single()
    }

    fun sjekkTilgangTilPersoner(personIdenter: List<String>): Map<String, Tilgang> {
        return cacheManager.hentCacheForSaksbehandler("sjekkTilgangTilPersoner", personIdenter) {
            familieIntegrasjonerTilgangskontrollClient.sjekkTilgangTilPersoner(it).associateBy { it.personIdent }
        }
    }

    fun hentIdenterMedStrengtFortroligAdressebeskyttelse(personIdenter: List<String>): List<String> {
        val adresseBeskyttelseBolk = systemOnlyPdlRestClient.hentAdressebeskyttelseBolk(personIdenter)
        return adresseBeskyttelseBolk.filter { (_, person) ->
            person.adressebeskyttelse.any { adressebeskyttelse ->
                adressebeskyttelse.gradering == ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG ||
                    adressebeskyttelse.gradering == ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG_UTLAND
            }
        }.map { it.key }
    }
}
