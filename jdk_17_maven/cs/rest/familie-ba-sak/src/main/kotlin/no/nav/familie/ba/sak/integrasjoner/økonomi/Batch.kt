package no.nav.familie.ba.sak.integrasjoner.økonomi

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import java.time.LocalDate

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "Batch")
@Table(name = "BATCH")
data class Batch(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "batch_seq")
    @SequenceGenerator(name = "batch_seq")
    val id: Long = 0,

    @Column(name = "kjoredato", nullable = false)
    val kjøreDato: LocalDate,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: KjøreStatus = KjøreStatus.LEDIG,
)
