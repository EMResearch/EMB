package no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.domene

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.EØSStandardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.RestVedtaksbegrunnelse
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "eøsBegrunnelse")
@Table(name = "EOS_BEGRUNNELSE")
class EØSBegrunnelse(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "eos_begrunnelse_seq_generator")
    @SequenceGenerator(
        name = "eos_begrunnelse_seq_generator",
        sequenceName = "eos_begrunnelse_seq",
        allocationSize = 50,
    )
    val id: Long = 0,

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "fk_vedtaksperiode_id", nullable = false, updatable = false)
    val vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser,

    @Enumerated(EnumType.STRING)
    @Column(name = "begrunnelse", updatable = false)
    val begrunnelse: EØSStandardbegrunnelse,
) {
    fun kopier(vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser): EØSBegrunnelse =
        EØSBegrunnelse(
            vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
            begrunnelse = this.begrunnelse,
        )

    fun tilRestVedtaksbegrunnelse() = RestVedtaksbegrunnelse(
        standardbegrunnelse = this.begrunnelse.enumnavnTilString(),
        vedtakBegrunnelseType = this.begrunnelse.vedtakBegrunnelseType,
        vedtakBegrunnelseSpesifikasjon = this.begrunnelse.enumnavnTilString(),
    )
}
