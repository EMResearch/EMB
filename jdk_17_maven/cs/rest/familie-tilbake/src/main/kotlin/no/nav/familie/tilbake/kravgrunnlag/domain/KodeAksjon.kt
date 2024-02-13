package no.nav.familie.tilbake.kravgrunnlag.domain

enum class KodeAksjon(val kode: String) {
    FINN_GRUNNLAG_OMGJØRING("3"),
    HENT_KORRIGERT_KRAVGRUNNLAG("4"),
    HENT_GRUNNLAG_OMGJØRING("5"),
    FATTE_VEDTAK("8"),
    ANNULERE_GRUNNLAG("A"),
}
