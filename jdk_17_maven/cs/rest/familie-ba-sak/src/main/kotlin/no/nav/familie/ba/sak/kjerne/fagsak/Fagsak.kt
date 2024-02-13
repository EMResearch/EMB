package no.nav.familie.ba.sak.kjerne.fagsak

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.kjerne.institusjon.Institusjon
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import java.util.Objects

@Entity(name = "Fagsak")
@Table(name = "FAGSAK")
data class Fagsak(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "fagsak_seq_generator")
    @SequenceGenerator(name = "fagsak_seq_generator", sequenceName = "fagsak_seq", allocationSize = 50)
    val id: Long = 0,

    @OneToOne(optional = false)
    @JoinColumn(
        name = "fk_aktoer_id",
        nullable = false,
        updatable = false,
    )
    val aktør: Aktør,

    @ManyToOne(optional = true)
    @JoinColumn(
        name = "fk_institusjon_id",
        nullable = true,
        updatable = true,
    )
    var institusjon: Institusjon? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: FagsakStatus = FagsakStatus.OPPRETTET,

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    val type: FagsakType = FagsakType.NORMAL,

    @Column(name = "arkivert", nullable = false)
    var arkivert: Boolean = false,
) : BaseEntitet() {

    override fun hashCode(): Int {
        return Objects.hashCode(id)
    }

    override fun toString(): String {
        return "Fagsak(id=$id)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Fagsak

        if (id != other.id) return false

        return true
    }
}

enum class FagsakStatus {
    OPPRETTET,
    LØPENDE, // Har minst én behandling gjeldende for fremtidig utbetaling
    AVSLUTTET,
}

enum class FagsakType {
    NORMAL,
    BARN_ENSLIG_MINDREÅRIG,
    INSTITUSJON,
    ;

    fun erBarnSøker() = this == BARN_ENSLIG_MINDREÅRIG || this == INSTITUSJON
}
