package no.nav.familie.ba.sak.integrasjoner.økonomi

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
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import java.util.UUID

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "DataChunk")
@Table(name = "DATA_CHUNK")
data class DataChunk(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "data_chunk_seq")
    @SequenceGenerator(name = "data_chunk_seq")
    val id: Long = 0,

    @ManyToOne(optional = false)
    @JoinColumn(name = "fk_batch_id", nullable = false, updatable = false)
    val batch: Batch,

    @Column(name = "transaksjons_id", nullable = false)
    val transaksjonsId: UUID,

    @Column(name = "chunk_nr", nullable = false)
    val chunkNr: Int,

    @Column(name = "er_sendt", nullable = false)
    var erSendt: Boolean = false,
)
