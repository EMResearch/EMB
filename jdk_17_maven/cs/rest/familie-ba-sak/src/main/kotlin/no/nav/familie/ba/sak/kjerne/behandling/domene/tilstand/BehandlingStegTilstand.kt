package no.nav.familie.ba.sak.kjerne.behandling.domene.tilstand

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
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.steg.BehandlingStegStatus
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import java.util.Objects

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "BehandlingStegTilstand")
@Table(name = "BEHANDLING_STEG_TILSTAND")
data class BehandlingStegTilstand(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "behandling_steg_tilstand_seq_generator")
    @SequenceGenerator(
        name = "behandling_steg_tilstand_seq_generator",
        sequenceName = "behandling_steg_tilstand_seq",
        allocationSize = 50,
    )
    val id: Long = 0,

    @ManyToOne(optional = false)
    @JoinColumn(name = "fk_behandling_id", nullable = false, updatable = false)
    @JsonIgnore
    val behandling: Behandling,

    @Enumerated(EnumType.STRING)
    @Column(name = "behandling_steg", nullable = false)
    val behandlingSteg: StegType,

    @Enumerated(EnumType.STRING)
    @Column(name = "behandling_steg_status", nullable = false)
    var behandlingStegStatus: BehandlingStegStatus = BehandlingStegStatus.IKKE_UTFØRT,
) : BaseEntitet() {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BehandlingStegTilstand

        return behandlingSteg == other.behandlingSteg
    }

    override fun hashCode(): Int {
        return Objects.hash(behandlingSteg)
    }

    override fun toString(): String {
        return "BehandlingStegTilstand(id=$id, behandling=${behandling.id}, behandlingSteg=$behandlingSteg, behandlingStegStatus=$behandlingStegStatus)"
    }
}
