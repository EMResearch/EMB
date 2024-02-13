package no.nav.familie.tilbake.kravgrunnlag.domain

import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.tilbake.common.repository.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.MappedCollection
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.util.Objects
import java.util.UUID

data class Kravgrunnlag431(
    @Id
    val id: UUID = UUID.randomUUID(),
    val behandlingId: UUID,
    val aktiv: Boolean = true,
    val sperret: Boolean = false,
    val avsluttet: Boolean = false,
    val vedtakId: BigInteger,
    val omgjortVedtakId: BigInteger? = null,
    val kravstatuskode: Kravstatuskode,
    @Column("fagomradekode")
    val fagområdekode: Fagområdekode,
    val fagsystemId: String,
    val fagsystemVedtaksdato: LocalDate? = null,
    val gjelderVedtakId: String,
    val gjelderType: GjelderType,
    val utbetalesTilId: String,
    val utbetIdType: GjelderType,
    val hjemmelkode: String? = null,
    val beregnesRenter: Boolean? = null,
    val ansvarligEnhet: String,
    val bostedsenhet: String,
    val behandlingsenhet: String,
    val kontrollfelt: String,
    val saksbehandlerId: String,
    val referanse: String,
    val eksternKravgrunnlagId: BigInteger,
    @MappedCollection(idColumn = "kravgrunnlag431_id")
    val perioder: Set<Kravgrunnlagsperiode432> = setOf(),
    @Version
    val versjon: Long = 0,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
)

data class Kravgrunnlagsperiode432(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val periode: Månedsperiode,
    @Column("manedlig_skattebelop")
    val månedligSkattebeløp: BigDecimal,
    @MappedCollection(idColumn = "kravgrunnlagsperiode432_id")
    val beløp: Set<Kravgrunnlagsbeløp433> = setOf(),
    @Version
    val versjon: Long = 0,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
) {

    fun harIdentiskKravgrunnlagsperiode(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as Kravgrunnlagsperiode432
        return this.månedligSkattebeløp == that.månedligSkattebeløp &&
            this.periode == that.periode &&
            this.beløp == that.beløp
    }
}

@Table("kravgrunnlagsbelop433")
data class Kravgrunnlagsbeløp433(
    @Id
    val id: UUID = UUID.randomUUID(),
    val klassekode: Klassekode,
    val klassetype: Klassetype,
    @Column("opprinnelig_utbetalingsbelop")
    val opprinneligUtbetalingsbeløp: BigDecimal,
    @Column("nytt_belop")
    val nyttBeløp: BigDecimal,
    @Column("tilbakekreves_belop")
    val tilbakekrevesBeløp: BigDecimal,
    @Column("uinnkrevd_belop")
    val uinnkrevdBeløp: BigDecimal? = null,
    val resultatkode: String? = null,
    @Column("arsakskode")
    val årsakskode: String? = null,
    val skyldkode: String? = null,
    val skatteprosent: BigDecimal,
    @Version
    val versjon: Long = 0,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as Kravgrunnlagsbeløp433
        return this.klassekode == that.klassekode &&
            this.klassetype == that.klassetype &&
            this.nyttBeløp == that.nyttBeløp &&
            this.opprinneligUtbetalingsbeløp == that.opprinneligUtbetalingsbeløp &&
            this.uinnkrevdBeløp == that.uinnkrevdBeløp &&
            this.tilbakekrevesBeløp == that.tilbakekrevesBeløp &&
            this.resultatkode == that.resultatkode &&
            this.skatteprosent == that.skatteprosent &&
            this.skyldkode == that.skyldkode &&
            this.årsakskode == that.årsakskode
    }

    override fun hashCode(): Int {
        return Objects.hash(
            klassekode,
            klassetype,
            nyttBeløp,
            opprinneligUtbetalingsbeløp,
            uinnkrevdBeløp,
            tilbakekrevesBeløp,
            resultatkode,
            skatteprosent,
            skyldkode,
            årsakskode,
        )
    }
}

enum class Klassekode(val aktivitet: String) {
    KL_KODE_FEIL_BA(""),
    KL_KODE_FEIL_EFOG(""),
    KL_KODE_FEIL_PEN(""),
    KL_KODE_FEIL_KS(""), // Kontantstøtte
    KL_KODE_JUST_BA(""),
    KL_KODE_JUST_KS(""),
    KL_KODE_JUST_EFOG(""),
    KL_KODE_JUST_PEN(""),
    BATR("Barnetrygd"),
    BATRSMA("Småbarnstillegg"),
    BAOREUMS("Barnetrygd-EU-Norge-Infotrygd"),
    BAOROSMS("Barnetrygd-Infotrygd"),
    BAUTEFMS("BarnetrygdUtvidet-Infotrygd"),
    BAUTEFSM("Småbarnstillegg-Infotrygd"),
    BAUTMDMS("Barnetrygd-Utvidet-Delt-Bosted"),
    EFOG("Overgangsstønad"),
    EFBT("Barnetilsyn"),
    EFBTOR("Barnetilsyn-Infotrygd"),
    EFSP("Skolepenger"),
    KS("Kontantstøtte"),
    TREK_KODER(""), // Felles klassekode for alle TREK klassetyper
    ;
    companion object {

        fun fraKode(kode: String, klassetype: Klassetype): Klassekode {
            if (klassetype == Klassetype.TREK) return TREK_KODER
            return values().firstOrNull { it.name == kode }
                ?: throw IllegalArgumentException("Ukjent KlasseKode $kode")
        }
    }
}

enum class Klassetype(val navn: String) {
    FEIL("Feilkonto"),
    JUST("Justeringskonto"),
    SKAT("Skatt"),
    TREK("Trekk"),
    YTEL("Ytelseskonto"),
    ;

    companion object {

        fun fraKode(kode: String): Klassetype {
            return values().firstOrNull { it.name == kode }
                ?: throw IllegalArgumentException("Ukjent Klassetype $kode")
        }
    }
}
