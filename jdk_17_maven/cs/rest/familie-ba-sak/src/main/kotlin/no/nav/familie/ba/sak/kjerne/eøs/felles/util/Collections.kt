package no.nav.familie.ba.sak.kjerne.eøs.felles.util

fun <T> Collection<T>.erEkteDelmengdeAv(mengde: Collection<T>) =
    this.size < mengde.size && mengde.containsAll(this)

fun <T> Collection<T>.replaceLast(replacer: (T) -> T) =
    this.take(this.size - 1) + replacer(this.last())
