package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.brev.mottaker.Brevmottaker
import no.nav.familie.ba.sak.kjerne.brev.mottaker.MottakerType

data class RestBrevmottaker(
    val id: Long?,
    val type: MottakerType,
    val navn: String,
    val adresselinje1: String,
    val adresselinje2: String?,
    val postnummer: String,
    val poststed: String,
    val landkode: String,
)

fun RestBrevmottaker.tilBrevMottaker(behandlingId: Long) = Brevmottaker(
    behandlingId = behandlingId,
    type = type,
    navn = navn,
    adresselinje1 = adresselinje1,
    adresselinje2 = adresselinje2,
    postnummer = postnummer.trim(),
    poststed = poststed.trim(),
    landkode = landkode,
)
