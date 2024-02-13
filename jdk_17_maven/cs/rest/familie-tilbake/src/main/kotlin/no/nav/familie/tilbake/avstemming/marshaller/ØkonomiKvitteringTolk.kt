package no.nav.familie.tilbake.avstemming.marshaller

import no.nav.tilbakekreving.typer.v1.MmelDto

object ØkonomiKvitteringTolk {

    private val KVITTERING_OK_KODER = setOf("00", "04")

    fun erKvitteringOK(kvittering: MmelDto): Boolean {
        return KVITTERING_OK_KODER.contains(kvittering.alvorlighetsgrad)
    }
}
