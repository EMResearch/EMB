package no.nav.familie.ba.sak.kjerne.personident

object Identkonverterer {
    fun er11Siffer(ident: String): Boolean = ident.all { it.isDigit() } && ident.length == 11

    fun formaterIdent(ident: String): String =
        if (er11Siffer(ident)) {
            "${ident.substring(0, 6)} ${ident.substring(6)}"
        } else {
            ident
        }
}
