package no.nav.familie.ba.sak.kjerne.behandling.domene

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
import java.time.LocalDate

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "BehandlingMigreringsinfo")
@Table(name = "BEHANDLING_MIGRERINGSINFO")
data class BehandlingMigreringsinfo(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "behandling_migreringsinfo_seq_generator")
    @SequenceGenerator(
        name = "behandling_migreringsinfo_seq_generator",
        sequenceName = "behandling_migreringsinfo_seq",
        allocationSize = 50,
    )
    val id: Long = 0,

    @ManyToOne(optional = false)
    @JoinColumn(name = "fk_behandling_id", nullable = false, updatable = false)
    val behandling: Behandling,

    val migreringsdato: LocalDate,

) : BaseEntitet()
