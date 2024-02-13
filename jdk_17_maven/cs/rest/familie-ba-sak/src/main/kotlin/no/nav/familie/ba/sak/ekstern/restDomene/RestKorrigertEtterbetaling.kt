package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.korrigertetterbetaling.KorrigertEtterbetaling
import no.nav.familie.ba.sak.kjerne.korrigertetterbetaling.KorrigertEtterbetalingÅrsak
import java.time.LocalDateTime

data class RestKorrigertEtterbetaling(
    val id: Long,
    val årsak: KorrigertEtterbetalingÅrsak,
    val begrunnelse: String?,
    val opprettetTidspunkt: LocalDateTime,
    val beløp: Int,
    val aktiv: Boolean,
)

fun KorrigertEtterbetaling.tilRestKorrigertEtterbetaling(): RestKorrigertEtterbetaling =
    RestKorrigertEtterbetaling(
        id = id,
        årsak = årsak,
        begrunnelse = begrunnelse,
        opprettetTidspunkt = opprettetTidspunkt,
        beløp = beløp,
        aktiv = aktiv,
    )
