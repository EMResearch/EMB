package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.korrigertvedtak.KorrigertVedtak
import java.time.LocalDate

class RestKorrigertVedtak(
    val id: Long,
    val vedtaksdato: LocalDate?,
    val begrunnelse: String?,
    val aktiv: Boolean,
)

fun KorrigertVedtak.tilRestKorrigertVedtak(): RestKorrigertVedtak = RestKorrigertVedtak(
    id = id,
    vedtaksdato = vedtaksdato,
    begrunnelse = begrunnelse,
    aktiv = aktiv,
)
