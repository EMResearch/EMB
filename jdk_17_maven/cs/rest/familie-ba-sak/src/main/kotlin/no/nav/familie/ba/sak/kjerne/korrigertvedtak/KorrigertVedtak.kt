package no.nav.familie.ba.sak.kjerne.korrigertvedtak

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import java.time.LocalDate

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "KorrigertVedtak")
@Table(name = "KORRIGERT_VEDTAK")
class KorrigertVedtak(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "korrigert_vedtak_seq_generator")
    @SequenceGenerator(
        name = "korrigert_vedtak_seq_generator",
        sequenceName = "korrigert_vedtak_seq",
        allocationSize = 50,
    )
    val id: Long = 0,

    @Column(name = "vedtaksdato", columnDefinition = "DATE")
    val vedtaksdato: LocalDate,

    @Column(name = "begrunnelse")
    val begrunnelse: String?,

    @ManyToOne(optional = false)
    @JoinColumn(name = "fk_behandling_id")
    val behandling: Behandling,

    @Column(name = "aktiv")
    var aktiv: Boolean,
) : BaseEntitet()

data class KorrigerVedtakRequest(
    val vedtaksdato: LocalDate,
    val begrunnelse: String?,
)

fun KorrigerVedtakRequest.tilKorrigerVedtak(behandling: Behandling) =
    KorrigertVedtak(vedtaksdato = vedtaksdato, begrunnelse = begrunnelse, behandling = behandling, aktiv = true)
