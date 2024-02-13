package no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.domene

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ba.sak.common.YearMonthConverter
import org.hibernate.Hibernate
import java.time.LocalDateTime
import java.time.YearMonth

@Entity(name = "Satskjøring")
@Table(name = "satskjoering")
data class Satskjøring(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "satskjoering_seq_generator")
    @SequenceGenerator(
        name = "satskjoering_seq_generator",
        sequenceName = "satskjoering_seq",
        allocationSize = 50,
    )
    val id: Long = 0,

    @Column(name = "fk_fagsak_id", nullable = false, updatable = false, unique = true)
    val fagsakId: Long,

    @Column(name = "start_tid", nullable = false, updatable = false)
    val startTidspunkt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "ferdig_tid")
    var ferdigTidspunkt: LocalDateTime? = null,

    @Column(name = "feiltype")
    var feiltype: String? = null,

    @Column(name = "sats_tid", columnDefinition = "DATE")
    @Convert(converter = YearMonthConverter::class)
    val satsTidspunkt: YearMonth,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as Satskjøring

        return id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()

    @Override
    override fun toString(): String {
        return this::class.simpleName + "(id = $id , fagsakId = $fagsakId )"
    }
}
