package no.nav.familie.tilbake.behandling.domain

import no.nav.familie.kontrakter.felles.Språkkode
import org.springframework.data.relational.core.mapping.Column

data class Bruker(
    val ident: String,
    @Column("sprakkode")
    val språkkode: Språkkode = Språkkode.NB,
)
