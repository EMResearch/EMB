package no.nav.familie.ba.sak.kjerne.behandling.domene

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
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import java.time.LocalDateTime

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "BehandlingSøknadsinfo")
@Table(name = "BEHANDLING_SOKNADSINFO")
data class BehandlingSøknadsinfo(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "behandling_søknadsinfo_seq_generator")
    @SequenceGenerator(
        name = "behandling_søknadsinfo_seq_generator",
        sequenceName = "behandling_soknadsinfo_seq",
        allocationSize = 50,
    )
    val id: Long = 0,

    @Column(name = "journalpost_id")
    val journalpostId: String? = null,

    @Column(name = "brevkode")
    val brevkode: String? = null,

    @ManyToOne(optional = false)
    @JoinColumn(name = "fk_behandling_id", nullable = false, updatable = false)
    val behandling: Behandling,

    val mottattDato: LocalDateTime,

    @Column(name = "er_digital")
    val erDigital: Boolean? = null,

) : BaseEntitet()
