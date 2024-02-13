package no.nav.familie.ba.sak.integrasjoner.infotrygd

import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.FamilieIntegrasjonerTilgangskontrollService
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.kontrakter.ba.infotrygd.InfotrygdSøkResponse
import no.nav.familie.kontrakter.ba.infotrygd.Sak
import no.nav.familie.kontrakter.ba.infotrygd.Stønad
import org.springframework.stereotype.Service

@Service
class InfotrygdService(
    private val infotrygdBarnetrygdClient: InfotrygdBarnetrygdClient,
    private val familieIntegrasjonerTilgangskontrollService: FamilieIntegrasjonerTilgangskontrollService,
    private val personidentService: PersonidentService,
) {
    fun hentInfotrygdsakerForSøker(aktør: Aktør): InfotrygdSøkResponse<Sak> {
        return infotrygdBarnetrygdClient.hentSaker(listOf(aktør.aktivFødselsnummer()), emptyList())
    }

    fun hentMaskertRestInfotrygdsakerVedManglendeTilgang(aktør: Aktør): RestInfotrygdsaker? {
        return familieIntegrasjonerTilgangskontrollService.hentMaskertPersonInfoVedManglendeTilgang(aktør)
            ?.let {
                RestInfotrygdsaker(
                    adressebeskyttelsegradering = it.adressebeskyttelseGradering,
                    harTilgang = false,
                )
            }
    }

    fun hentInfotrygdstønaderForSøker(ident: String, historikk: Boolean = false): InfotrygdSøkResponse<Stønad> {
        val søkerIdenter = personidentService.hentIdenter(personIdent = ident, historikk = true)
            .filter { it.gruppe == "FOLKEREGISTERIDENT" }
            .map { it.ident }
        return infotrygdBarnetrygdClient.hentStønader(søkerIdenter, emptyList(), historikk)
    }

    fun hentMaskertRestInfotrygdstønaderVedManglendeTilgang(aktør: Aktør): RestInfotrygdstønader? {
        return familieIntegrasjonerTilgangskontrollService.hentMaskertPersonInfoVedManglendeTilgang(aktør)
            ?.let {
                RestInfotrygdstønader(
                    adressebeskyttelsegradering = it.adressebeskyttelseGradering,
                    harTilgang = false,
                )
            }
    }

    fun harÅpenSakIInfotrygd(søkerIdenter: List<String>, barnasIdenter: List<String> = emptyList()): Boolean {
        return infotrygdBarnetrygdClient.harÅpenSakIInfotrygd(søkerIdenter, barnasIdenter)
    }

    fun harLøpendeSakIInfotrygd(søkerIdenter: List<String>, barnasIdenter: List<String> = emptyList()): Boolean {
        return infotrygdBarnetrygdClient.harLøpendeSakIInfotrygd(søkerIdenter, barnasIdenter)
    }

    fun harSendtbrev(søkerIdenter: List<String>, brevkoder: List<InfotrygdBrevkode>): Boolean {
        if (brevkoder.isEmpty()) {
            return false
        }

        val infotrygdbrevrespons = infotrygdBarnetrygdClient.harNyligSendtBrevFor(søkerIdenter, brevkoder)
        secureLogger.info("InfotrygdBrevRespons  $infotrygdbrevrespons")
        return infotrygdbrevrespons.harSendtBrev
    }
}
