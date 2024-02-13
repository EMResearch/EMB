package no.nav.familie.ba.sak.kjerne.grunnlag.småbarnstillegg

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.kjerne.beregning.domene.InternPeriodeOvergangsstønad
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import no.nav.familie.kontrakter.felles.ef.Datakilde
import no.nav.familie.kontrakter.felles.ef.EksternPeriode
import java.time.LocalDate

/**
 * Periode vi har hentet fra ef-sak som representerer når en person
 * har hatt full overgangsstønad.
 */
@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "PeriodeOvergangsstønadGrunnlag")
@Table(name = "GR_PERIODE_OVERGANGSSTONAD")
data class PeriodeOvergangsstønadGrunnlag(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gr_periode_overgangsstonad_seq_generator")
    @SequenceGenerator(
        name = "gr_periode_overgangsstonad_seq_generator",
        sequenceName = "gr_periode_overgangsstonad_seq",
        allocationSize = 50,
    )
    val id: Long = 0,

    @Column(name = "fk_behandling_id", nullable = false, updatable = false)
    val behandlingId: Long,

    @OneToOne(optional = false)
    @JoinColumn(name = "fk_aktoer_id", nullable = false, updatable = false)
    val aktør: Aktør,

    @Column(name = "fom", nullable = false, columnDefinition = "DATE")
    val fom: LocalDate,

    @Column(name = "tom", nullable = false, columnDefinition = "DATE")
    val tom: LocalDate,

    @Enumerated(EnumType.STRING)
    @Column(name = "datakilde", nullable = false)
    val datakilde: Datakilde,
) : BaseEntitet() {

    override fun toString(): String {
        return "PeriodeOvergangsstønadGrunnlag(" +
            "id=$id, " +
            "behandlingId=$behandlingId, " +
            "aktør=$aktør, " +
            "fom=$fom, " +
            "tom=$tom, " +
            "datakilde=$datakilde)"
    }
    fun tilInternPeriodeOvergangsstønad() = InternPeriodeOvergangsstønad(
        personIdent = this.aktør.aktivFødselsnummer(),
        fomDato = this.fom,
        tomDato = this.tom,
    )
}

fun EksternPeriode.tilPeriodeOvergangsstønadGrunnlag(behandlingId: Long, aktør: Aktør) =
    PeriodeOvergangsstønadGrunnlag(
        behandlingId = behandlingId,
        aktør = aktør,
        fom = this.fomDato,
        tom = this.tomDato,
        datakilde = this.datakilde,
    )
