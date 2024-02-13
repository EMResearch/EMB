package no.nav.familie.ba.sak.statistikk.stønadsstatistikk

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.beregning.beregnUtbetalingsperioderUtenKlassifisering
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.filtrerGjeldendeNå
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakRepository
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.eksterne.kontrakter.BehandlingTypeV2
import no.nav.familie.eksterne.kontrakter.BehandlingÅrsakV2
import no.nav.familie.eksterne.kontrakter.FagsakType
import no.nav.familie.eksterne.kontrakter.KategoriV2
import no.nav.familie.eksterne.kontrakter.Kompetanse
import no.nav.familie.eksterne.kontrakter.KompetanseAktivitet
import no.nav.familie.eksterne.kontrakter.KompetanseResultat
import no.nav.familie.eksterne.kontrakter.PersonDVHV2
import no.nav.familie.eksterne.kontrakter.UnderkategoriV2
import no.nav.familie.eksterne.kontrakter.UtbetalingsDetaljDVHV2
import no.nav.familie.eksterne.kontrakter.UtbetalingsperiodeDVHV2
import no.nav.familie.eksterne.kontrakter.VedtakDVHV2
import no.nav.familie.eksterne.kontrakter.YtelseType.ORDINÆR_BARNETRYGD
import no.nav.familie.eksterne.kontrakter.YtelseType.SMÅBARNSTILLEGG
import no.nav.familie.eksterne.kontrakter.YtelseType.UTVIDET_BARNETRYGD
import no.nav.fpsak.tidsserie.LocalDateInterval
import no.nav.fpsak.tidsserie.LocalDateSegment
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.util.UUID

@Service
class StønadsstatistikkService(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val persongrunnlagService: PersongrunnlagService,
    private val vedtakService: VedtakService,
    private val personopplysningerService: PersonopplysningerService,
    private val vedtakRepository: VedtakRepository,
    private val kompetanseService: KompetanseService,
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService,
) {

    fun hentVedtakV2(behandlingId: Long): VedtakDVHV2 {
        val vedtak = vedtakService.hentAktivForBehandling(behandlingId)
        val behandling = vedtak?.behandling ?: behandlingHentOgPersisterService.hent(behandlingId)
        val persongrunnlag = persongrunnlagService.hentAktivThrows(behandlingId)
        // DVH ønsker tidspunkt med klokkeslett

        var datoVedtak = vedtak?.vedtaksdato

        if (datoVedtak == null) {
            datoVedtak = vedtakRepository.finnVedtakForBehandling(behandlingId).singleOrNull()?.vedtaksdato
                ?: error("Fant ikke vedtaksdato for behandling $behandlingId")
        }

        val tidspunktVedtak = datoVedtak
        return VedtakDVHV2(
            fagsakId = behandling.fagsak.id.toString(),
            fagsakType = FagsakType.valueOf(behandling.fagsak.type.name),
            behandlingsId = behandlingId.toString(),
            tidspunktVedtak = tidspunktVedtak.atZone(TIMEZONE),
            personV2 = hentSøkerV2(persongrunnlag),
            ensligForsørger = utledEnsligForsørger(behandlingId), // TODO implementere støtte for dette
            kategoriV2 = KategoriV2.valueOf(behandling.kategori.name),
            underkategoriV2 = when (behandling.underkategori) {
                BehandlingUnderkategori.ORDINÆR -> UnderkategoriV2.ORDINÆR
                BehandlingUnderkategori.UTVIDET -> UnderkategoriV2.UTVIDET
            },
            behandlingTypeV2 = BehandlingTypeV2.valueOf(behandling.type.name),
            utbetalingsperioderV2 = hentUtbetalingsperioderV2(behandling, persongrunnlag),
            funksjonellId = UUID.randomUUID().toString(),
            kompetanseperioder = hentKompetanse(BehandlingId(behandlingId)),
            behandlingÅrsakV2 = BehandlingÅrsakV2.valueOf(behandling.opprettetÅrsak.name),
        )
    }

    private fun hentKompetanse(behandlingId: BehandlingId): List<Kompetanse> {
        val kompetanser = kompetanseService.hentKompetanser(behandlingId)

        return kompetanser.filter { it.resultat != null }.map { kompetanse ->
            Kompetanse(
                barnsIdenter = kompetanse.barnAktører.map { aktør -> aktør.aktivFødselsnummer() },
                annenForeldersAktivitet = if (kompetanse.annenForeldersAktivitet != null) {
                    KompetanseAktivitet.valueOf(
                        kompetanse.annenForeldersAktivitet.name,
                    )
                } else {
                    null
                },
                annenForeldersAktivitetsland = kompetanse.annenForeldersAktivitetsland,
                barnetsBostedsland = kompetanse.barnetsBostedsland,
                fom = kompetanse.fom!!,
                tom = kompetanse.tom,
                resultat = KompetanseResultat.valueOf(kompetanse.resultat!!.name),
                sokersaktivitet = if (kompetanse.søkersAktivitet != null) KompetanseAktivitet.valueOf(kompetanse.søkersAktivitet.name) else null,
                sokersAktivitetsland = kompetanse.søkersAktivitetsland,
            )
        }
    }

    private fun hentSøkerV2(persongrunnlag: PersonopplysningGrunnlag): PersonDVHV2 {
        val søker = persongrunnlag.søker
        return lagPersonDVHV2(søker)
    }

    private fun hentUtbetalingsperioderV2(
        behandling: Behandling,
        persongrunnlag: PersonopplysningGrunnlag,
    ): List<UtbetalingsperiodeDVHV2> {
        val andelerTilkjentYtelse = andelerTilkjentYtelseOgEndreteUtbetalingerService
            .finnAndelerTilkjentYtelseMedEndreteUtbetalinger(behandling.id)

        if (andelerTilkjentYtelse.isEmpty()) return emptyList()

        val utbetalingsPerioder = beregnUtbetalingsperioderUtenKlassifisering(andelerTilkjentYtelse)

        val søkerOgBarn = persongrunnlag.søkerOgBarn
        return utbetalingsPerioder.toSegments()
            .sortedWith(compareBy<LocalDateSegment<Int>>({ it.fom }, { it.value }, { it.tom }))
            .map { segment ->
                val andelerForSegment = andelerTilkjentYtelse.filter {
                    segment.localDateInterval.overlaps(
                        LocalDateInterval(
                            it.stønadFom.førsteDagIInneværendeMåned(),
                            it.stønadTom.sisteDagIInneværendeMåned(),
                        ),
                    )
                }
                mapTilUtbetalingsperiodeV2(
                    segment,
                    andelerForSegment,
                    behandling,
                    søkerOgBarn,
                )
            }
    }

    private fun utledEnsligForsørger(behandlingId: Long): Boolean {
        val andelerTilkjentYtelse = andelerTilkjentYtelseOgEndreteUtbetalingerService
            .finnAndelerTilkjentYtelseMedEndreteUtbetalinger(behandlingId)
        if (andelerTilkjentYtelse.isEmpty()) {
            return false
        }

        return andelerTilkjentYtelse.find { it.type == YtelseType.UTVIDET_BARNETRYGD } != null
    }

    private fun mapTilUtbetalingsperiodeV2(
        segment: LocalDateSegment<Int>,
        andelerForSegment: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
        behandling: Behandling,
        søkerOgBarn: List<Person>,
    ): UtbetalingsperiodeDVHV2 {
        return UtbetalingsperiodeDVHV2(
            hjemmel = "Ikke implementert",
            stønadFom = segment.fom,
            stønadTom = segment.tom,
            utbetaltPerMnd = segment.value,
            utbetalingsDetaljer = andelerForSegment.filter { it.erAndelSomSkalSendesTilOppdrag() }.map { andel ->
                val personForAndel =
                    søkerOgBarn.find { person -> andel.aktør == person.aktør }
                        ?: throw IllegalStateException("Fant ikke personopplysningsgrunnlag for andel")
                UtbetalingsDetaljDVHV2(
                    person = lagPersonDVHV2(
                        personForAndel,
                        andel.prosent.intValueExact(),
                    ),
                    klassekode = andel.type.klassifisering,
                    ytelseType = when (andel.type) {
                        YtelseType.ORDINÆR_BARNETRYGD -> ORDINÆR_BARNETRYGD
                        YtelseType.UTVIDET_BARNETRYGD -> UTVIDET_BARNETRYGD
                        YtelseType.SMÅBARNSTILLEGG -> SMÅBARNSTILLEGG
                    },
                    utbetaltPrMnd = andel.kalkulertUtbetalingsbeløp,
                    delytelseId = behandling.fagsak.id.toString() + andel.periodeOffset,
                )
            },
        )
    }

    private fun lagPersonDVHV2(person: Person, delingsProsentYtelse: Int = 0): PersonDVHV2 {
        return PersonDVHV2(
            rolle = person.type.name,
            statsborgerskap = hentStatsborgerskap(person),
            bostedsland = hentLandkode(person),
            delingsprosentYtelse = if (delingsProsentYtelse == 50) delingsProsentYtelse else 0,
            personIdent = person.aktør.aktivFødselsnummer(),
        )
    }

    private fun hentStatsborgerskap(person: Person): List<String> {
        return if (person.statsborgerskap.isNotEmpty()) {
            person.statsborgerskap.filtrerGjeldendeNå().map { it.landkode }
        } else {
            listOf(personopplysningerService.hentGjeldendeStatsborgerskap(person.aktør).land)
        }
    }

    private fun hentLandkode(person: Person): String = if (person.bostedsadresser.isNotEmpty()) {
        "NO"
    } else if (personopplysningerService.hentPersoninfoEnkel(person.aktør).bostedsadresser.isNotEmpty()) {
        "NO"
    } else {
        val landKode = personopplysningerService.hentLandkodeAlpha2UtenlandskBostedsadresse(person.aktør)

        if (landKode == PersonopplysningerService.UKJENT_LANDKODE) {
            logger.info("Sender landkode ukjent til DVH")
            secureLogger.info("Ukjent land sendt til DVH for person ${person.aktør.aktivFødselsnummer()}")
        }
        landKode
    }

    companion object {

        private val logger = LoggerFactory.getLogger(StønadsstatistikkService::class.java)
        private val TIMEZONE = ZoneId.of("Europe/Paris")
    }
}
