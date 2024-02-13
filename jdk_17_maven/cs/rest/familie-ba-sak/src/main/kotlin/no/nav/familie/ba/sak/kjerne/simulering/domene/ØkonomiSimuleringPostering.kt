package no.nav.familie.ba.sak.kjerne.simulering.domene

import com.fasterxml.jackson.annotation.JsonIdentityInfo
import com.fasterxml.jackson.annotation.JsonIdentityReference
import com.fasterxml.jackson.annotation.ObjectIdGenerators
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import no.nav.familie.kontrakter.felles.simulering.BetalingType
import no.nav.familie.kontrakter.felles.simulering.FagOmrådeKode
import no.nav.familie.kontrakter.felles.simulering.PosteringType
import java.math.BigDecimal
import java.time.LocalDate

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "OkonomiSimuleringPostering")
@Table(name = "OKONOMI_SIMULERING_POSTERING")
data class ØkonomiSimuleringPostering(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "okonomi_simulering_postering_seq_generator")
    @SequenceGenerator(
        name = "okonomi_simulering_postering_seq_generator",
        sequenceName = "okonomi_simulering_postering_seq",
        allocationSize = 50,
    )
    val id: Long = 0,

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_okonomi_simulering_mottaker_id", nullable = false, updatable = false)
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator::class, property = "id")
    @JsonIdentityReference(alwaysAsId = true)
    val økonomiSimuleringMottaker: ØkonomiSimuleringMottaker,

    @Enumerated(EnumType.STRING)
    @Column(name = "fag_omraade_kode", nullable = false)
    val fagOmrådeKode: FagOmrådeKode,

    @Column(name = "fom", updatable = false, nullable = false)
    val fom: LocalDate,

    @Column(name = "tom", updatable = false, nullable = false)
    val tom: LocalDate,

    @Enumerated(EnumType.STRING)
    @Column(name = "betaling_type", nullable = false)
    val betalingType: BetalingType,

    @Column(name = "belop", nullable = false)
    val beløp: BigDecimal,

    @Enumerated(EnumType.STRING)
    @Column(name = "postering_type", nullable = false)
    val posteringType: PosteringType,

    @Column(name = "forfallsdato", updatable = false, nullable = false)
    val forfallsdato: LocalDate,

    @Column(name = "uten_inntrekk", nullable = false)
    val utenInntrekk: Boolean,
) : BaseEntitet() {

    override fun hashCode() = id.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is ØkonomiSimuleringPostering) return false

        return (id == other.id)
    }

    override fun toString(): String {
        return "BrSimuleringPostering(" +
            "id=$id, " +
            "økonomiSimuleringMottaker=${økonomiSimuleringMottaker.id}, " +
            "fagOmrådeKode=$fagOmrådeKode, " +
            "fom=$fom, " +
            "tom=$tom, " +
            "betalingType=$betalingType, " +
            "beløp=$beløp, " +
            "posteringType=$posteringType, " +
            "forfallsdato=$forfallsdato, " +
            "utenInntrekk=$utenInntrekk" +
            ")"
    }

    val erManuellPostering: Boolean
        get() {
            return this.fagOmrådeKode == FagOmrådeKode.BARNETRYGD_INFOTRYGD_MANUELT || this.fagOmrådeKode == FagOmrådeKode.BARNETRYGD_MANUELT
        }
}
