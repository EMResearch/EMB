package no.nav.familie.ba.sak.integrasjoner.økonomi

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiUtils.andelerTilOpphørMedDato
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiUtils.andelerTilOpprettelse
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiUtils.sisteAndelPerKjede
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiUtils.sisteBeståendeAndelPerKjede
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.task.dto.FAGSYSTEM
import no.nav.familie.felles.utbetalingsgenerator.Utbetalingsgenerator
import no.nav.familie.felles.utbetalingsgenerator.domain.AndelDataLongId
import no.nav.familie.felles.utbetalingsgenerator.domain.Behandlingsinformasjon
import no.nav.familie.felles.utbetalingsgenerator.domain.BeregnetUtbetalingsoppdragLongId
import no.nav.familie.felles.utbetalingsgenerator.domain.Fagsystem
import no.nav.familie.felles.utbetalingsgenerator.domain.IdentOgType
import no.nav.familie.kontrakter.felles.oppdrag.Opphør
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.YearMonth

@Component
class UtbetalingsoppdragGenerator(
    private val beregningService: BeregningService,
) {

    fun lagUtbetalingsoppdrag(
        saksbehandlerId: String,
        vedtak: Vedtak,
        forrigeTilkjentYtelse: TilkjentYtelse?,
        nyTilkjentYtelse: TilkjentYtelse,
        sisteAndelPerKjede: Map<IdentOgType, AndelTilkjentYtelse>,
        erSimulering: Boolean,
        endretMigreringsDato: YearMonth? = null,
    ): BeregnetUtbetalingsoppdragLongId {
        return Utbetalingsgenerator().lagUtbetalingsoppdrag(
            behandlingsinformasjon = Behandlingsinformasjon(
                saksbehandlerId = saksbehandlerId,
                behandlingId = vedtak.behandling.id.toString(),
                eksternBehandlingId = vedtak.behandling.id,
                eksternFagsakId = vedtak.behandling.fagsak.id,
                fagsystem = FagsystemBA.BARNETRYGD,
                personIdent = vedtak.behandling.fagsak.aktør.aktivFødselsnummer(),
                vedtaksdato = vedtak.vedtaksdato?.toLocalDate() ?: LocalDate.now(),
                opphørAlleKjederFra = finnOpphørsdatoForAlleKjeder(
                    forrigeTilkjentYtelse = forrigeTilkjentYtelse,
                    sisteAndelPerKjede = sisteAndelPerKjede,
                    endretMigreringsDato = endretMigreringsDato,
                ),
                utbetalesTil = hentUtebetalesTil(vedtak.behandling.fagsak),
                opphørKjederFraFørsteUtbetaling = if (endretMigreringsDato != null) false else erSimulering, // Ved simulering når migreringsdato er endret, skal vi opphøre fra den nye datoen og ikke fra første utbetaling per kjede.
            ),
            forrigeAndeler = forrigeTilkjentYtelse?.tilAndelData() ?: emptyList(),
            nyeAndeler = nyTilkjentYtelse.tilAndelData(),
            sisteAndelPerKjede = sisteAndelPerKjede.mapValues { it.value.tilAndelDataLongId() },
        )
    }

    private fun TilkjentYtelse.tilAndelData(): List<AndelDataLongId> =
        this.andelerTilkjentYtelse.map { it.tilAndelDataLongId() }

    private fun AndelTilkjentYtelse.tilAndelDataLongId(): AndelDataLongId =
        AndelDataLongId(
            id = id,
            fom = periode.fom,
            tom = periode.tom,
            beløp = kalkulertUtbetalingsbeløp,
            personIdent = aktør.aktivFødselsnummer(),
            type = type.tilYtelseType(),
            periodeId = periodeOffset,
            forrigePeriodeId = forrigePeriodeOffset,
            kildeBehandlingId = kildeBehandlingId,
        )

    private fun finnOpphørsdatoForAlleKjeder(
        forrigeTilkjentYtelse: TilkjentYtelse?,
        sisteAndelPerKjede: Map<IdentOgType, AndelTilkjentYtelse>,
        endretMigreringsDato: YearMonth?,
    ): YearMonth? {
        if (forrigeTilkjentYtelse == null || sisteAndelPerKjede.isEmpty()) return null
        if (endretMigreringsDato != null) return endretMigreringsDato
        return null
    }

    private fun hentUtebetalesTil(fagsak: Fagsak): String {
        return when (fagsak.type) {
            FagsakType.INSTITUSJON -> {
                fagsak.institusjon?.tssEksternId
                    ?: error("Fagsak ${fagsak.id} er av type institusjon og mangler informasjon om institusjonen")
            }

            else -> {
                fagsak.aktør.aktivFødselsnummer()
            }
        }
    }

    /**
     * Lager utbetalingsoppdrag med kjedede perioder av andeler.
     * Ved opphør sendes kun siste utbetalingsperiode (med opphørsdato).
     *
     * @param[saksbehandlerId] settes på oppdragsnivå
     * @param[vedtak] for å hente fagsakid, behandlingid, vedtaksdato, ident, og evt opphørsdato
     * @param[erFørsteBehandlingPåFagsak] for å sette aksjonskode på oppdragsnivå og bestemme om vi skal telle fra start
     * @param[forrigeKjeder] Et sett med kjeder som var gjeldende for forrige behandling på fagsaken
     * @param[sisteAndelPerIdent] Siste iverksatte andel mot økonomi per ident.
     * @param[oppdaterteKjeder] Et sett med andeler knyttet til en person (dvs en kjede), hvor andeler er helt nye,
     * @param[erSimulering] flag for om beregnet er en simulering, da skal komplett nytt betlaingsoppdrag generes
     *                      og ny tilkjentytelse skal ikke persisteres,
     * @param[endretMigreringsDato] Satt betyr at en endring skjedd fra før den eksisterende migreringsdatoen, som en konsekevens
     *                              skal hele betalingsoppdraget opphøre.
     * flag for om beregnet er en simulering, da skal komplett nytt betlaingsoppdrag generes
     *                      og ny tilkjentytelse skal ikke persisteres,
     * har endrede datoer eller må bygges opp igjen pga endringer før i kjeden
     * @return Utbetalingsoppdrag for vedtak
     */
    fun lagUtbetalingsoppdragOgOppdaterTilkjentYtelse(
        saksbehandlerId: String,
        vedtak: Vedtak,
        erFørsteBehandlingPåFagsak: Boolean,
        forrigeKjeder: Map<IdentOgYtelse, List<AndelTilkjentYtelseForUtbetalingsoppdrag>> = emptyMap(),
        sisteAndelPerIdent: Map<IdentOgYtelse, AndelTilkjentYtelseForUtbetalingsoppdrag> = emptyMap(),
        oppdaterteKjeder: Map<IdentOgYtelse, List<AndelTilkjentYtelseForUtbetalingsoppdrag>> = emptyMap(),
        erSimulering: Boolean = false,
        endretMigreringsDato: YearMonth? = null,
    ): Utbetalingsoppdrag {
        // Hos økonomi skiller man på endring på oppdragsnivå 110 og på linjenivå 150 (periodenivå).
        // Da de har opplevd å motta
        // UEND på oppdrag som skulle vært ENDR anbefaler de at kun ENDR brukes når sak
        // ikke er ny, så man slipper å forholde seg til om det er endring over 150-nivå eller ikke.
        val aksjonskodePåOppdragsnivå =
            if (erFørsteBehandlingPåFagsak) Utbetalingsoppdrag.KodeEndring.NY else Utbetalingsoppdrag.KodeEndring.ENDR

        // endretMigreringsDato satt betyr at endring skjedd for migreringsdato og som en
        // konsekvens så skal hele betalingsoppdraget opphøre.
        val erEndretMigreringsDato = endretMigreringsDato != null

        // Generer et komplett nytt eller bare endringer på et eksisterende betalingsoppdrag.
        val sisteBeståenAndelIHverKjede = if (erSimulering || erEndretMigreringsDato) {
            // Gjennom å sette andeler til null markeres at alle perioder i kjeden skal opphøres.
            sisteAndelPerKjede(forrigeKjeder, oppdaterteKjeder)
        } else {
            // For å kunne behandling alle forlengelser/forkortelser av perioder likt har vi valgt å konsekvent opphøre og erstatte.
            // Det vil si at vi alltid gjenoppbygger kjede fra første endring, selv om vi i realiteten av og til kun endrer datoer
            // på en eksisterende linje (endring på 150 linjenivå).
            sisteBeståendeAndelPerKjede(forrigeKjeder, oppdaterteKjeder)
        }

        val andelerTilOpphør =
            andelerTilOpphørMedDato(
                forrigeKjeder,
                sisteBeståenAndelIHverKjede,
                endretMigreringsDato,
                sisteAndelPerIdent,
            )
        val andelerTilOpprettelse: List<List<AndelTilkjentYtelseForUtbetalingsoppdrag>> =
            andelerTilOpprettelse(oppdaterteKjeder, sisteBeståenAndelIHverKjede)

        val opprettes: List<Utbetalingsperiode> = if (andelerTilOpprettelse.isNotEmpty()) {
            val sisteOffsetIKjedeOversikt =
                sisteAndelPerIdent.map { it.key to it.value.periodeOffset!!.toInt() }.toMap()
            lagUtbetalingsperioderForOpprettelseOgOppdaterTilkjentYtelse(
                andeler = andelerTilOpprettelse,
                erFørsteBehandlingPåFagsak = erFørsteBehandlingPåFagsak,
                vedtak = vedtak,
                sisteOffsetIKjedeOversikt = sisteOffsetIKjedeOversikt,
                sisteOffsetPåFagsak = sisteOffsetIKjedeOversikt.maxOfOrNull { it.value },
                skalOppdatereTilkjentYtelse = !erSimulering,
            )
        } else {
            emptyList()
        }

        val opphøres: List<Utbetalingsperiode> = if (andelerTilOpphør.isNotEmpty()) {
            lagUtbetalingsperioderForOpphør(
                andeler = andelerTilOpphør,
                vedtak = vedtak,
            )
        } else {
            emptyList()
        }

        return Utbetalingsoppdrag(
            saksbehandlerId = saksbehandlerId,
            kodeEndring = aksjonskodePåOppdragsnivå,
            fagSystem = FAGSYSTEM,
            saksnummer = vedtak.behandling.fagsak.id.toString(),
            aktoer = vedtak.behandling.fagsak.aktør.aktivFødselsnummer(),
            utbetalingsperiode = listOf(opphøres, opprettes).flatten(),
        )
    }

    private fun lagUtbetalingsperioderForOpphør(
        andeler: List<Pair<AndelTilkjentYtelseForUtbetalingsoppdrag, YearMonth>>,
        vedtak: Vedtak,
    ): List<Utbetalingsperiode> {
        val utbetalingsperiodeMal = UtbetalingsperiodeMal(
            vedtak = vedtak,
            erEndringPåEksisterendePeriode = true,
        )

        return andeler.map { (sisteAndelIKjede, opphørKjedeFom) ->
            utbetalingsperiodeMal.lagPeriodeFraAndel(
                andel = sisteAndelIKjede,
                periodeIdOffset = sisteAndelIKjede.periodeOffset!!.toInt(),
                forrigePeriodeIdOffset = sisteAndelIKjede.forrigePeriodeOffset?.toInt(),
                opphørKjedeFom = opphørKjedeFom,
            )
        }
    }

    private fun lagUtbetalingsperioderForOpprettelseOgOppdaterTilkjentYtelse(
        andeler: List<List<AndelTilkjentYtelseForUtbetalingsoppdrag>>,
        vedtak: Vedtak,
        erFørsteBehandlingPåFagsak: Boolean,
        sisteOffsetIKjedeOversikt: Map<IdentOgYtelse, Int>,
        sisteOffsetPåFagsak: Int? = null,
        skalOppdatereTilkjentYtelse: Boolean,
    ): List<Utbetalingsperiode> {
        var offset =
            if (!erFørsteBehandlingPåFagsak) {
                sisteOffsetPåFagsak?.plus(1)
                    ?: throw IllegalStateException("Skal finnes offset når ikke første behandling på fagsak")
            } else {
                0
            }

        val utbetalingsperiodeMal = UtbetalingsperiodeMal(
            vedtak = vedtak,
        )

        val utbetalingsperioder = andeler.filter { kjede -> kjede.isNotEmpty() }
            .flatMap { kjede: List<AndelTilkjentYtelseForUtbetalingsoppdrag> ->
                val ident = kjede.first().aktør.aktivFødselsnummer()
                val ytelseType = kjede.first().type
                var forrigeOffsetIKjede: Int? = null
                if (!erFørsteBehandlingPåFagsak) {
                    forrigeOffsetIKjede = sisteOffsetIKjedeOversikt[IdentOgYtelse(ident, ytelseType)]
                }
                kjede.sortedBy { it.stønadFom }.mapIndexed { index, andel ->
                    val forrigeOffset = if (index == 0) forrigeOffsetIKjede else offset - 1
                    utbetalingsperiodeMal.lagPeriodeFraAndel(andel, offset, forrigeOffset).also {
                        andel.periodeOffset = offset.toLong()
                        andel.forrigePeriodeOffset = forrigeOffset?.toLong()
                        andel.kildeBehandlingId =
                            andel.behandlingId // Trengs for å finne tilbake ved konsistensavstemming
                        offset++
                    }
                }
            }

        // TODO Vi bør se om vi kan flytte ut denne side effecten
        if (skalOppdatereTilkjentYtelse) {
            val oppdatertTilkjentYtelse = andeler.flatten().firstOrNull()?.tilkjentYtelse ?: throw Feil(
                "Andeler mangler ved generering av utbetalingsperioder. Får tom liste.",
            )
            beregningService.lagreTilkjentYtelseMedOppdaterteAndeler(oppdatertTilkjentYtelse)
        }

        return utbetalingsperioder
    }
}

abstract class AndelTilkjentYtelseForUtbetalingsoppdrag(private val andelTilkjentYtelse: AndelTilkjentYtelse) {
    val behandlingId: Long? = andelTilkjentYtelse.behandlingId
    val tilkjentYtelse: TilkjentYtelse = andelTilkjentYtelse.tilkjentYtelse
    val kalkulertUtbetalingsbeløp: Int = andelTilkjentYtelse.kalkulertUtbetalingsbeløp
    val stønadFom: YearMonth = andelTilkjentYtelse.stønadFom
    val stønadTom: YearMonth = andelTilkjentYtelse.stønadTom
    val aktør: Aktør = andelTilkjentYtelse.aktør
    val type: YtelseType = andelTilkjentYtelse.type
    abstract var periodeOffset: Long?
    abstract var forrigePeriodeOffset: Long?
    abstract var kildeBehandlingId: Long?

    override fun equals(other: Any?): Boolean {
        return if (other is AndelTilkjentYtelseForUtbetalingsoppdrag) {
            this.andelTilkjentYtelse.equals(other.andelTilkjentYtelse)
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        return andelTilkjentYtelse.hashCode()
    }
}

interface AndelTilkjentYtelseForUtbetalingsoppdragFactory {
    fun pakkInnForUtbetaling(andelerTilkjentYtelse: Collection<AndelTilkjentYtelse>): List<AndelTilkjentYtelseForUtbetalingsoppdrag>
}

class AndelTilkjentYtelseForSimuleringFactory : AndelTilkjentYtelseForUtbetalingsoppdragFactory {
    override fun pakkInnForUtbetaling(andelerTilkjentYtelse: Collection<AndelTilkjentYtelse>): List<AndelTilkjentYtelseForUtbetalingsoppdrag> =
        andelerTilkjentYtelse.map { AndelTilkjentYtelseForSimulering(it) }

    private class AndelTilkjentYtelseForSimulering(
        andelTilkjentYtelse: AndelTilkjentYtelse,
    ) : AndelTilkjentYtelseForUtbetalingsoppdrag(andelTilkjentYtelse) {
        override var periodeOffset: Long? = andelTilkjentYtelse.periodeOffset
        override var forrigePeriodeOffset: Long? = andelTilkjentYtelse.forrigePeriodeOffset
        override var kildeBehandlingId: Long? = andelTilkjentYtelse.kildeBehandlingId
    }
}

class AndelTilkjentYtelseForIverksettingFactory : AndelTilkjentYtelseForUtbetalingsoppdragFactory {
    override fun pakkInnForUtbetaling(andelerTilkjentYtelse: Collection<AndelTilkjentYtelse>): List<AndelTilkjentYtelseForUtbetalingsoppdrag> =
        andelerTilkjentYtelse.map { AndelTilkjentYtelseForIverksetting(it) }

    private class AndelTilkjentYtelseForIverksetting(
        private val andelTilkjentYtelse: AndelTilkjentYtelse,
    ) : AndelTilkjentYtelseForUtbetalingsoppdrag(andelTilkjentYtelse) {
        override var periodeOffset: Long?
            get() = andelTilkjentYtelse.periodeOffset
            set(value) {
                andelTilkjentYtelse.periodeOffset = value
            }

        override var forrigePeriodeOffset: Long?
            get() = andelTilkjentYtelse.forrigePeriodeOffset
            set(value) {
                andelTilkjentYtelse.forrigePeriodeOffset = value
            }

        override var kildeBehandlingId: Long?
            get() = andelTilkjentYtelse.kildeBehandlingId
            set(value) {
                andelTilkjentYtelse.kildeBehandlingId = value
            }
    }
}

fun Collection<AndelTilkjentYtelse>.pakkInnForUtbetaling(
    andelTilkjentYtelseForUtbetalingsoppdragFactory: AndelTilkjentYtelseForUtbetalingsoppdragFactory,
) = andelTilkjentYtelseForUtbetalingsoppdragFactory.pakkInnForUtbetaling(this)

enum class YtelsetypeBA(
    override val klassifisering: String,
    override val satsType: no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsperiode.SatsType = no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsperiode.SatsType.MND,
) : no.nav.familie.felles.utbetalingsgenerator.domain.Ytelsestype {
    ORDINÆR_BARNETRYGD("BATR"),
    UTVIDET_BARNETRYGD("BATR"),
    SMÅBARNSTILLEGG("BATRSMA"),
}

enum class FagsystemBA(
    override val kode: String,
    override val gyldigeSatstyper: Set<YtelsetypeBA>,
) : Fagsystem {
    BARNETRYGD(
        "BA",
        setOf(YtelsetypeBA.ORDINÆR_BARNETRYGD, YtelsetypeBA.UTVIDET_BARNETRYGD, YtelsetypeBA.SMÅBARNSTILLEGG),
    ),
}

fun no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsoppdrag.tilRestUtbetalingsoppdrag(): Utbetalingsoppdrag =
    Utbetalingsoppdrag(
        kodeEndring = Utbetalingsoppdrag.KodeEndring.valueOf(this.kodeEndring.name),
        fagSystem = this.fagSystem,
        saksnummer = this.saksnummer,
        aktoer = this.aktoer,
        saksbehandlerId = this.saksbehandlerId,
        avstemmingTidspunkt = this.avstemmingTidspunkt,
        utbetalingsperiode = this.utbetalingsperiode.map { it.tilRestUtbetalingsperiode() },
        gOmregning = this.gOmregning,
    )

fun no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsperiode.tilRestUtbetalingsperiode(): Utbetalingsperiode =
    Utbetalingsperiode(
        erEndringPåEksisterendePeriode = this.erEndringPåEksisterendePeriode,
        opphør = this.opphør?.let { Opphør(it.opphørDatoFom) },
        periodeId = this.periodeId,
        forrigePeriodeId = this.forrigePeriodeId,
        datoForVedtak = this.datoForVedtak,
        klassifisering = this.klassifisering,
        vedtakdatoFom = this.vedtakdatoFom,
        vedtakdatoTom = this.vedtakdatoTom,
        sats = this.sats,
        satsType = Utbetalingsperiode.SatsType.valueOf(this.satsType.name),
        utbetalesTil = this.utbetalesTil,
        behandlingId = this.behandlingId,
        utbetalingsgrad = this.utbetalingsgrad,
    )
