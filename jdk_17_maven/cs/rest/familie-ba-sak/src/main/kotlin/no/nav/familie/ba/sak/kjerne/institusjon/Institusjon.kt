package no.nav.familie.ba.sak.kjerne.institusjon

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "Institusjon")
@Table(name = "INSTITUSJON")
data class Institusjon(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "institusjon_seq_generator")
    @SequenceGenerator(name = "institusjon_seq_generator", sequenceName = "institusjon_seq", allocationSize = 50)
    val id: Long = 0,

    @Column(name = "org_nummer", updatable = false, length = 50)
    val orgNummer: String,

    @Column(name = "tss_ekstern_id", updatable = false, length = 50)
    val tssEksternId: String?,
) : BaseEntitet()
