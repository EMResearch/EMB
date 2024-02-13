package no.nav.familie.ba.sak.kjerne.endretutbetaling.domene

import jakarta.persistence.Column
import jakarta.persistence.Convert
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
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.common.YearMonthConverter
import no.nav.familie.ba.sak.common.erDagenFør
import no.nav.familie.ba.sak.common.overlapperHeltEllerDelvisMed
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.ekstern.restDomene.RestEndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.beregning.domene.EndretUtbetalingAndelMedAndelerTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.brev.domene.MinimertRestEndretAndel
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tilTidslinje
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "EndretUtbetalingAndel")
@Table(name = "ENDRET_UTBETALING_ANDEL")
data class EndretUtbetalingAndel(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "endret_utbetaling_andel_seq_generator")
    @SequenceGenerator(
        name = "endret_utbetaling_andel_seq_generator",
        sequenceName = "endret_utbetaling_andel_seq",
        allocationSize = 50,
    )
    val id: Long = 0,

    @Column(name = "fk_behandling_id", updatable = false, nullable = false)
    val behandlingId: Long,

    @ManyToOne
    @JoinColumn(name = "fk_po_person_id")
    var person: Person? = null,

    @Column(name = "prosent")
    var prosent: BigDecimal? = null,

    @Column(name = "fom", columnDefinition = "DATE")
    @Convert(converter = YearMonthConverter::class)
    var fom: YearMonth? = null,

    @Column(name = "tom", columnDefinition = "DATE")
    @Convert(converter = YearMonthConverter::class)
    var tom: YearMonth? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "aarsak")
    var årsak: Årsak? = null,

    @Column(name = "avtaletidspunkt_delt_bosted")
    var avtaletidspunktDeltBosted: LocalDate? = null,

    @Column(name = "soknadstidspunkt")
    var søknadstidspunkt: LocalDate? = null,

    @Column(name = "begrunnelse")
    var begrunnelse: String? = null,
) : BaseEntitet() {

    fun overlapperMed(periode: MånedPeriode) = periode.overlapperHeltEllerDelvisMed(this.periode)

    val periode
        get(): MånedPeriode {
            validerUtfyltEndring()
            return MånedPeriode(this.fom!!, this.tom!!)
        }

    fun validerUtfyltEndring(): Boolean {
        if (manglerObligatoriskFelt()
        ) {
            val feilmelding =
                "Person, prosent, fom, tom, årsak, begrunnese og søknadstidspunkt skal være utfylt: $this.tostring()"
            throw FunksjonellFeil(melding = feilmelding, frontendFeilmelding = feilmelding)
        }

        if (fom!! > tom!!) {
            throw FunksjonellFeil(
                melding = "fom må være lik eller komme før tom",
                frontendFeilmelding = "Du kan ikke sette en f.o.m. dato som er etter t.o.m. dato",
            )
        }

        if (årsak == Årsak.DELT_BOSTED && avtaletidspunktDeltBosted == null) {
            throw FunksjonellFeil("Avtaletidspunkt skal være utfylt når årsak er delt bosted: $this.tostring()")
        }

        return true
    }

    fun manglerObligatoriskFelt() = person == null ||
        prosent == null ||
        fom == null ||
        tom == null ||
        årsak == null ||
        søknadstidspunkt == null ||
        (begrunnelse == null || begrunnelse!!.isEmpty())

    fun årsakErDeltBosted() = this.årsak == Årsak.DELT_BOSTED
}

enum class Årsak(val visningsnavn: String) {
    DELT_BOSTED("Delt bosted"),
    ETTERBETALING_3ÅR("Etterbetaling 3 år"),
    ENDRE_MOTTAKER("Foreldrene bor sammen, endret mottaker"),
    ALLEREDE_UTBETALT("Allerede utbetalt"),
}

fun EndretUtbetalingAndelMedAndelerTilkjentYtelse.tilRestEndretUtbetalingAndel() =
    RestEndretUtbetalingAndel(
        id = this.id,
        personIdent = this.aktivtFødselsnummer,
        prosent = this.prosent,
        fom = this.fom,
        tom = this.tom,
        årsak = this.årsak,
        avtaletidspunktDeltBosted = this.avtaletidspunktDeltBosted,
        søknadstidspunkt = this.søknadstidspunkt,
        begrunnelse = this.begrunnelse,
        erTilknyttetAndeler = this.andelerTilkjentYtelse.isNotEmpty(),
    )

fun EndretUtbetalingAndel.fraRestEndretUtbetalingAndel(
    restEndretUtbetalingAndel: RestEndretUtbetalingAndel,
    person: Person,
): EndretUtbetalingAndel {
    this.fom = restEndretUtbetalingAndel.fom
    this.tom = restEndretUtbetalingAndel.tom
    this.prosent = restEndretUtbetalingAndel.prosent ?: BigDecimal(0)
    this.årsak = restEndretUtbetalingAndel.årsak
    this.avtaletidspunktDeltBosted = restEndretUtbetalingAndel.avtaletidspunktDeltBosted
    this.søknadstidspunkt = restEndretUtbetalingAndel.søknadstidspunkt
    this.begrunnelse = restEndretUtbetalingAndel.begrunnelse
    this.person = person
    return this
}

fun hentPersonerForEtterEndretUtbetalingsperiode(
    minimerteEndredeUtbetalingAndeler: List<MinimertRestEndretAndel>,
    fom: LocalDate?,
    endringsaarsaker: Set<Årsak>,
) = minimerteEndredeUtbetalingAndeler.filter { endretUtbetalingAndel ->
    endretUtbetalingAndel.periode.tom.sisteDagIInneværendeMåned()
        .erDagenFør(fom) &&
        endringsaarsaker.contains(endretUtbetalingAndel.årsak)
}.map { it.personIdent }

sealed interface IEndretUtbetalingAndel

data class TomEndretUtbetalingAndel(
    val id: Long,
    val behandlingId: Long,
) : IEndretUtbetalingAndel

sealed interface IUtfyltEndretUtbetalingAndel : IEndretUtbetalingAndel {
    val id: Long
    val behandlingId: Long
    val person: Person
    val prosent: BigDecimal
    val fom: YearMonth
    val tom: YearMonth
    val årsak: Årsak
    val søknadstidspunkt: LocalDate
    val begrunnelse: String
}

data class UtfyltEndretUtbetalingAndel(
    override val id: Long,
    override val behandlingId: Long,
    override val person: Person,
    override val prosent: BigDecimal,
    override val fom: YearMonth,
    override val tom: YearMonth,
    override val årsak: Årsak,
    override val søknadstidspunkt: LocalDate,
    override val begrunnelse: String,
) : IUtfyltEndretUtbetalingAndel

data class UtfyltEndretUtbetalingAndelDeltBosted(
    override val id: Long,
    override val behandlingId: Long,
    override val person: Person,
    override val prosent: BigDecimal,
    override val fom: YearMonth,
    override val tom: YearMonth,
    override val årsak: Årsak,
    override val søknadstidspunkt: LocalDate,
    override val begrunnelse: String,

    val avtaletidspunktDeltBosted: LocalDate,
) : IUtfyltEndretUtbetalingAndel

fun EndretUtbetalingAndel.tilIEndretUtbetalingAndel(): IEndretUtbetalingAndel {
    return if (this.manglerObligatoriskFelt()) {
        TomEndretUtbetalingAndel(
            this.id,
            this.behandlingId,
        )
    } else {
        if (this.årsakErDeltBosted()) {
            UtfyltEndretUtbetalingAndelDeltBosted(
                id = this.id,
                behandlingId = this.behandlingId,
                person = this.person!!,
                prosent = this.prosent!!,
                fom = this.fom!!,
                tom = this.tom!!,
                årsak = this.årsak!!,
                avtaletidspunktDeltBosted = this.avtaletidspunktDeltBosted!!,
                søknadstidspunkt = this.søknadstidspunkt!!,
                begrunnelse = this.begrunnelse!!,
            )
        }

        UtfyltEndretUtbetalingAndel(
            id = this.id,
            behandlingId = this.behandlingId,
            person = this.person!!,
            prosent = this.prosent!!,
            fom = this.fom!!,
            tom = this.tom!!,
            årsak = this.årsak!!,
            søknadstidspunkt = this.søknadstidspunkt!!,
            begrunnelse = this.begrunnelse!!,
        )
    }
}

fun List<IUtfyltEndretUtbetalingAndel>.tilTidslinje() =
    this.map { betalingAndel ->
        Periode(
            fraOgMed = betalingAndel.fom.tilTidspunkt(),
            tilOgMed = betalingAndel.tom.tilTidspunkt(),
            innhold = betalingAndel,
        )
    }.tilTidslinje()
