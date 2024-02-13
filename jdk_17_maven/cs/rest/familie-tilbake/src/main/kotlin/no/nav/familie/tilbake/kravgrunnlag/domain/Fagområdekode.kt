package no.nav.familie.tilbake.kravgrunnlag.domain

import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype

enum class Fagområdekode(val navn: String, val ytelsestype: Ytelsestype) {

    BA("Barnetrygd", Ytelsestype.BARNETRYGD),
    KS("Kontantstøtte", Ytelsestype.KONTANTSTØTTE),
    EFOG("Enslig forelder - Overgangsstønad", Ytelsestype.OVERGANGSSTØNAD),
    EFBT("Enslig forelder - Barnetilsyn", Ytelsestype.BARNETILSYN),
    EFSP("Enslig forelder - Skolepenger", Ytelsestype.SKOLEPENGER),
    ;

    companion object {

        fun fraKode(kode: String): Fagområdekode {
            for (fagområdekode in values()) {
                if (fagområdekode.name == kode) {
                    return fagområdekode
                }
            }
            throw IllegalArgumentException("Ukjent Fagområdekode $kode")
        }
    }
}
