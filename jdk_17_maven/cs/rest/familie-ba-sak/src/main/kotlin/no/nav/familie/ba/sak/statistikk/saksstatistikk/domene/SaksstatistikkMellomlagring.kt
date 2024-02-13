package no.nav.familie.ba.sak.statistikk.saksstatistikk.domene

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ba.sak.statistikk.saksstatistikk.sakstatistikkObjectMapper
import no.nav.familie.eksterne.kontrakter.saksstatistikk.BehandlingDVH
import no.nav.familie.eksterne.kontrakter.saksstatistikk.SakDVH
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

@Entity(name = "SaksstatistikkMellomlagring")
@Table(name = "SAKSSTATISTIKK_MELLOMLAGRING")
data class SaksstatistikkMellomlagring(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "saksstatistikk_mellomlagring_seq_generator")
    @SequenceGenerator(
        name = "saksstatistikk_mellomlagring_seq_generator",
        sequenceName = "SAKSSTATISTIKK_MELLOMLAGRING_SEQ",
        allocationSize = 50,
    )
    val id: Long = 0,

    @Column(name = "offset_verdi")
    var offsetVerdiOnPrem: Long? = null,

    @Column(name = "offset_aiven")
    var offsetVerdi: Long? = null,

    @Column(name = "funksjonell_id")
    val funksjonellId: String,

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    val type: SaksstatistikkMellomlagringType,

    @Column(name = "kontrakt_versjon")
    val kontraktVersjon: String,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "json")
    val json: String,

    @Column(name = "opprettet_tid", nullable = false, updatable = false)
    val opprettetTidspunkt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "konvertert_tid")
    var konvertertTidspunkt: LocalDateTime? = null,

    @Column(name = "sendt_tid")
    var sendtTidspunkt: LocalDateTime? = null,

    @Column(name = "type_id")
    var typeId: Long? = null,
) {
    fun jsonToSakDVH(): SakDVH {
        return sakstatistikkObjectMapper.readValue(json, SakDVH::class.java)
    }

    fun jsonToBehandlingDVH(): BehandlingDVH {
        return sakstatistikkObjectMapper.readValue(json, BehandlingDVH::class.java)
    }
}

enum class SaksstatistikkMellomlagringType {
    SAK,
    BEHANDLING,
}
