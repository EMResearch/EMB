package no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene

data class RestScenario(
    val søker: RestScenarioPerson,
    val barna: List<RestScenarioPerson>,
)
