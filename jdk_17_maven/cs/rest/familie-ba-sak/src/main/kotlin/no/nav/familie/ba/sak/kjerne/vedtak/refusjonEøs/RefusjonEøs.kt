package no.nav.familie.ba.sak.kjerne.vedtak.refusjonEøs

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
import java.time.LocalDate

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "RefusjonEos")
@Table(name = "REFUSJON_EOS")
data class RefusjonEøs(
    @Column(name = "fk_behandling_id", updatable = false, nullable = false)
    val behandlingId: Long,
    @Column(name = "fom", columnDefinition = "DATE", nullable = false)
    var fom: LocalDate,
    @Column(name = "tom", columnDefinition = "DATE", nullable = false)
    var tom: LocalDate,
    @Column(name = "refusjonsbeloep", nullable = false)
    var refusjonsbeløp: Int,
    @Column(name = "land", nullable = false)
    var land: String,
    @Column(name = "refusjon_avklart", nullable = false)
    var refusjonAvklart: Boolean,

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "refusjon_eos_seq_generator")
    @SequenceGenerator(
        name = "refusjon_eos_seq_generator",
        sequenceName = "refusjon_eos_seq",
        allocationSize = 50,
    )
    val id: Long = 0,
) : BaseEntitet()
