package no.nav.familie.ba.sak.integrasjoner.skyggesak

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity(name = "Skyggesak")
@Table(name = "SKYGGESAK")
data class Skyggesak(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "skyggesak_seq_generator")
    @SequenceGenerator(
        name = "skyggesak_seq_generator",
        sequenceName = "SKYGGESAK_SEQ",
        allocationSize = 50,
    )
    val id: Long = 0,

    @Column(name = "fk_fagsak_id", nullable = false, updatable = false)
    val fagsakId: Long,

    @Column(name = "sendt_tid")
    var sendtTidspunkt: LocalDateTime? = null,
)
