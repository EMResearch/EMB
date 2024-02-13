package no.nav.familie.ba.sak.kjerne.arbeidsfordeling.domene

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "ArbeidsfordelingPåBehandling")
@Table(name = "ARBEIDSFORDELING_PA_BEHANDLING")
data class ArbeidsfordelingPåBehandling(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "arbeidsfordeling_pa_behandling_seq_generator")
    @SequenceGenerator(
        name = "arbeidsfordeling_pa_behandling_seq_generator",
        sequenceName = "arbeidsfordeling_pa_behandling_seq",
        allocationSize = 50,
    )
    val id: Long = 0,

    @Column(name = "fk_behandling_id", nullable = false, updatable = false, unique = true)
    val behandlingId: Long,

    @Column(name = "behandlende_enhet_id", nullable = false)
    var behandlendeEnhetId: String,

    @Column(name = "behandlende_enhet_navn", nullable = false)
    var behandlendeEnhetNavn: String,

    @Column(name = "manuelt_overstyrt", nullable = false)
    var manueltOverstyrt: Boolean = false,
) {
    override fun toString(): String {
        return "ArbeidsfordelingPåBehandling(id=$id, manueltOverstyrt=$manueltOverstyrt)"
    }

    fun toSecureString(): String {
        return "ArbeidsfordelingPåBehandling(id=$id, behandlendeEnhetId=$behandlendeEnhetId, behandlendeEnhetNavn=$behandlendeEnhetNavn, manueltOverstyrt=$manueltOverstyrt)"
    }
}
