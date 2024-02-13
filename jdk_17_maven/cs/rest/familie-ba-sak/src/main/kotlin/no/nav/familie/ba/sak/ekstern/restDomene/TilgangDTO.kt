package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING

data class TilgangDTO(
    val saksbehandlerHarTilgang: Boolean,
    val adressebeskyttelsegradering: ADRESSEBESKYTTELSEGRADERING,
)
