package no.nav.familie.ba.sak.kjerne.arbeidsfordeling

import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService.IdentMedAdressebeskyttelse
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING

fun finnPersonMedStrengesteAdressebeskyttelse(personer: List<IdentMedAdressebeskyttelse>): String? {
    return personer.fold(
        null,
        fun(
            person: IdentMedAdressebeskyttelse?,
            neste: IdentMedAdressebeskyttelse,
        ): IdentMedAdressebeskyttelse? {
            return when {
                person?.adressebeskyttelsegradering == ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG -> {
                    person
                }
                neste.adressebeskyttelsegradering == ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG -> {
                    neste
                }
                person?.adressebeskyttelsegradering == ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG_UTLAND -> {
                    person
                }
                neste.adressebeskyttelsegradering == ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG_UTLAND -> {
                    neste
                }
                person?.adressebeskyttelsegradering == ADRESSEBESKYTTELSEGRADERING.FORTROLIG -> {
                    person
                }
                neste.adressebeskyttelsegradering == ADRESSEBESKYTTELSEGRADERING.FORTROLIG
                -> {
                    neste
                }
                else -> null
            }
        },
    )?.ident
}
