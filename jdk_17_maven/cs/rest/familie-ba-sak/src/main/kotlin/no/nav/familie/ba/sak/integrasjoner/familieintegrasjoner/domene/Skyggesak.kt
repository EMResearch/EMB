package no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene

data class Skyggesak(
    val aktoerId: String,
    val fagsakNr: String,
    val tema: String = "BAR",
    val applikasjon: String = "BA",
)
