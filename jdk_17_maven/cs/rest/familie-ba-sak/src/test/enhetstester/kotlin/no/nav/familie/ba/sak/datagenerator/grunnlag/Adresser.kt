package no.nav.familie.ba.sak.datagenerator.grunnlag

import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.GrVegadresse

fun opprettAdresse(
    matrikkelId: Long? = null,
    bruksenhetsnummer: String? = null,
    adressenavn: String? = null,
    husnummer: String? = null,
    husbokstav: String? = null,
    postnummer: String? = null,
) = GrVegadresse(
    matrikkelId = matrikkelId,
    husnummer = husnummer,
    husbokstav = husbokstav,
    bruksenhetsnummer = bruksenhetsnummer,
    adressenavn = adressenavn,
    kommunenummer = null,
    tilleggsnavn = null,
    postnummer = postnummer,
)
