package no.nav.familie.ba.sak.kjerne.simulering.domene

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import no.nav.familie.kontrakter.felles.simulering.MottakerType

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "OkonomiSimuleringMottaker")
@Table(name = "OKONOMI_SIMULERING_MOTTAKER")
data class ØkonomiSimuleringMottaker(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "okonomi_simulering_mottaker_seq_generator")
    @SequenceGenerator(
        name = "okonomi_simulering_mottaker_seq_generator",
        sequenceName = "okonomi_simulering_mottaker_seq",
        allocationSize = 50,
    )
    val id: Long = 0,

    @Column(name = "mottaker_nummer", nullable = false)
    val mottakerNummer: String?,

    @Enumerated(EnumType.STRING)
    @Column(name = "mottaker_type", nullable = false)
    val mottakerType: MottakerType,

    @ManyToOne(optional = false)
    @JoinColumn(name = "fk_behandling_id", nullable = false, updatable = false)
    val behandling: Behandling,

    @OneToMany(
        mappedBy = "økonomiSimuleringMottaker",
        cascade = [CascadeType.ALL],
        fetch = FetchType.EAGER,
        orphanRemoval = true,
    )
    var økonomiSimuleringPostering: List<ØkonomiSimuleringPostering> = emptyList(),
) : BaseEntitet() {

    override fun hashCode() = id.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is ØkonomiSimuleringMottaker) return false

        return (id == other.id)
    }

    override fun toString(): String {
        return "BrSimuleringMottaker(" +
            "id=$id, " +
            "mottakerType=$mottakerType, " +
            "behandling=$behandling, " +
            "økonomiSimuleringPostering=$økonomiSimuleringPostering" +
            ")"
    }
}
