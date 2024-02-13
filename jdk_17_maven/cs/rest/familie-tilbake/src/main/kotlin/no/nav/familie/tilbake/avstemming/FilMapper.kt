package no.nav.familie.tilbake.avstemming

class FilMapper(private val rader: List<Rad>) {

    fun tilFlatfil(): ByteArray {
        return (HEADER + rader.joinToString(SKILLETEGN_RADER) { it.toCsvString() }).toByteArray()
    }

    companion object {

        private const val SKILLETEGN_RADER = "\n"
        const val HEADER = "avsender" + Rad.SKILLETEGN_KOLONNER +
            "vedtakId" + Rad.SKILLETEGN_KOLONNER +
            "fnr" + Rad.SKILLETEGN_KOLONNER +
            "vedtaksdato" + Rad.SKILLETEGN_KOLONNER +
            "fagsakYtelseType" + Rad.SKILLETEGN_KOLONNER +
            "tilbakekrevesBruttoUtenRenter" + Rad.SKILLETEGN_KOLONNER +
            "skatt" + Rad.SKILLETEGN_KOLONNER +
            "tilbakekrevesNettoUtenRenter" + Rad.SKILLETEGN_KOLONNER +
            "renter" + Rad.SKILLETEGN_KOLONNER +
            "erOmgjøringTilIngenTilbakekreving" + SKILLETEGN_RADER
    }
}
