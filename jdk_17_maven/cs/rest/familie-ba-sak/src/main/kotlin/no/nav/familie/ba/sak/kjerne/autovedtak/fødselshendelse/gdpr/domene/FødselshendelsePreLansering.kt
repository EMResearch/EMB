package no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.gdpr.domene

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import java.util.Objects

/**
 * Ikke i bruk, men tar vare på den i tilfelle vi trenger dataene som ligger i prod for perioden
 * fødselshendelser var påskrudd for å telle metrikker.
 */
@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "FødselshendelsePreLansering")
@Table(name = "FOEDSELSHENDELSE_PRE_LANSERING")
data class FødselshendelsePreLansering(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "foedselshendelse_pre_lansering_seq_generator")
    @SequenceGenerator(
        name = "foedselshendelse_pre_lansering_seq_generator",
        sequenceName = "foedselshendelse_pre_lansering_seq",
        allocationSize = 50,
    )
    val id: Long = 0,

    @Column(name = "fk_behandling_id", nullable = false, updatable = false)
    val behandlingId: Long,

    @OneToOne(optional = false)
    @JoinColumn(name = "fk_aktoer_id", nullable = false, updatable = false)
    val aktør: Aktør,

    @Column(name = "ny_behandling_hendelse", nullable = false, updatable = false, columnDefinition = "TEXT")
    val nyBehandlingHendelse: String = "",

    @Column(name = "filtreringsregler_input", columnDefinition = "TEXT")
    val filtreringsreglerInput: String = "",

    @Column(name = "filtreringsregler_output", columnDefinition = "TEXT")
    val filtreringsreglerOutput: String = "",

    @Column(name = "vilkaarsvurderinger_for_foedselshendelse", columnDefinition = "TEXT")
    var vilkårsvurderingerForFødselshendelse: String = "",
) : BaseEntitet() {

    override fun hashCode(): Int {
        return Objects.hashCode(id)
    }

    override fun toString(): String {
        return "FødselshendelsePreLansering(id=$id)"
    }
}
