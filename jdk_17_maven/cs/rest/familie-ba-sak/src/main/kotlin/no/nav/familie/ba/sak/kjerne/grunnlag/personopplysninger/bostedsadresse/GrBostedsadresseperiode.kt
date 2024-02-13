package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse

import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase

/**
 * Ble brukt i tidlig fase av automatisk vurdering av fødselshendelser, men brukes ikke lenger.
 * Tar vare på i tilfelle vi må hente opp dataene igjen.
 */
@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "GrBostedsadresseperiode")
@Table(name = "PO_BOSTEDSADRESSEPERIODE")
data class GrBostedsadresseperiode(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "po_bostedsadresseperiode_seq_generator")
    @SequenceGenerator(
        name = "po_bostedsadresseperiode_seq_generator",
        sequenceName = "po_bostedsadresseperiode_seq",
        allocationSize = 50,
    )
    val id: Long = 0,

    @Embedded
    val periode: DatoIntervallEntitet? = null,
) : BaseEntitet()
