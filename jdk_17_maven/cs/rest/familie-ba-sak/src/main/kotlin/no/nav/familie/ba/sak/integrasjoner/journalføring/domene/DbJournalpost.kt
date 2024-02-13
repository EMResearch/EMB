package no.nav.familie.ba.sak.integrasjoner.journalføring.domene

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
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import java.time.LocalDateTime
import java.util.Objects

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "Journalpost")
@Table(name = "JOURNALPOST")
data class DbJournalpost(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "journalpost_seq_generator")
    @SequenceGenerator(name = "journalpost_seq_generator", sequenceName = "journalpost_seq", allocationSize = 50)
    val id: Long = 0,

    @Column(name = "opprettet_av", nullable = false, updatable = false)
    val opprettetAv: String = SikkerhetContext.hentSaksbehandlerNavn(),

    @Column(name = "opprettet_tid", nullable = false, updatable = false)
    val opprettetTidspunkt: LocalDateTime = LocalDateTime.now(),

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "fk_behandling_id", nullable = false)
    val behandling: Behandling,

    @Column(name = "journalpost_id")
    val journalpostId: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    val type: DbJournalpostType? = null,
) {
    override fun hashCode(): Int {
        return Objects.hashCode(id)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DbJournalpost

        if (id != other.id) return false

        return true
    }
}

enum class DbJournalpostType {
    I, U
}
