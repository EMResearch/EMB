package no.nav.familie.ba.sak.kjerne.verge

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase

// Denne tabellen brukes for å lagre verge detaljer kun til institusjon
@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "Verge")
@Table(name = "VERGE")
data class Verge(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "verge_seq_generator")
    @SequenceGenerator(name = "verge_seq_generator", sequenceName = "verge_seq", allocationSize = 50)
    val id: Long = 0,

    @Column(name = "ident", updatable = true, length = 20)
    var ident: String,

    @OneToOne(optional = false)
    @JoinColumn(
        name = "fk_behandling_id",
        nullable = false,
        updatable = false,
    )
    val behandling: Behandling,
) : BaseEntitet()
