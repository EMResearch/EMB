package no.nav.familie.ba.sak.kjerne.klage

import no.nav.familie.kontrakter.felles.klage.BehandlingEventType
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
import no.nav.familie.kontrakter.felles.klage.KlagebehandlingDto

fun KlagebehandlingDto.brukVedtaksdatoFraKlageinstansHvisOversendt(): KlagebehandlingDto {
    val erOversendtTilKlageinstans = resultat == BehandlingResultat.IKKE_MEDHOLD
    val vedtaksdato = if (erOversendtTilKlageinstans) {
        klageinstansResultat
            .singleOrNull { klageinstansResultat -> klageinstansResultat.type == BehandlingEventType.KLAGEBEHANDLING_AVSLUTTET }
            ?.mottattEllerAvsluttetTidspunkt
    } else {
        vedtaksdato
    }
    return copy(vedtaksdato = vedtaksdato)
}
