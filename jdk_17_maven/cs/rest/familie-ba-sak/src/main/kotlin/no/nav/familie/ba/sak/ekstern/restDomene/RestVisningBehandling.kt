package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import java.time.LocalDateTime

class RestVisningBehandling(
    val behandlingId: Long,
    val opprettetTidspunkt: LocalDateTime,
    val aktivertTidspunkt: LocalDateTime,
    val kategori: BehandlingKategori,
    val underkategori: BehandlingUnderkategoriDTO,
    val aktiv: Boolean,
    val årsak: BehandlingÅrsak?,
    val type: BehandlingType,
    val status: BehandlingStatus,
    val resultat: Behandlingsresultat,
    val vedtaksdato: LocalDateTime?,
)

fun Behandling.tilRestVisningBehandling(vedtaksdato: LocalDateTime?) = RestVisningBehandling(
    behandlingId = this.id,
    opprettetTidspunkt = this.opprettetTidspunkt,
    aktivertTidspunkt = this.aktivertTidspunkt,
    kategori = this.kategori,
    underkategori = this.underkategori.tilDto(),
    aktiv = this.aktiv,
    årsak = this.opprettetÅrsak,
    type = this.type,
    status = this.status,
    resultat = this.resultat,
    vedtaksdato = vedtaksdato,
)

enum class BehandlingUnderkategoriDTO {
    ORDINÆR,
    UTVIDET,
}

fun BehandlingUnderkategoriDTO.tilDomene() = when (this) {
    BehandlingUnderkategoriDTO.ORDINÆR -> BehandlingUnderkategori.ORDINÆR
    BehandlingUnderkategoriDTO.UTVIDET -> BehandlingUnderkategori.UTVIDET
}

fun BehandlingUnderkategori.tilDto() = when (this) {
    BehandlingUnderkategori.ORDINÆR -> BehandlingUnderkategoriDTO.ORDINÆR
    BehandlingUnderkategori.UTVIDET -> BehandlingUnderkategoriDTO.UTVIDET
}
