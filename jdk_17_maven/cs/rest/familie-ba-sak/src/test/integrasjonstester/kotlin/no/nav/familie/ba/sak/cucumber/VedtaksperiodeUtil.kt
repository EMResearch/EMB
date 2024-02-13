package no.nav.familie.ba.sak.cucumber

import io.cucumber.datatable.DataTable
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.defaultFagsak
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagPersonResultat
import no.nav.familie.ba.sak.common.lagVilkårsvurdering
import no.nav.familie.ba.sak.common.randomAktør
import no.nav.familie.ba.sak.common.tilddMMyyyy
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.cucumber.domeneparser.Domenebegrep
import no.nav.familie.ba.sak.cucumber.domeneparser.DomeneparserUtil.groupByBehandlingId
import no.nav.familie.ba.sak.cucumber.domeneparser.VedtaksperiodeMedBegrunnelserParser
import no.nav.familie.ba.sak.cucumber.domeneparser.parseDato
import no.nav.familie.ba.sak.cucumber.domeneparser.parseEnum
import no.nav.familie.ba.sak.cucumber.domeneparser.parseEnumListe
import no.nav.familie.ba.sak.cucumber.domeneparser.parseInt
import no.nav.familie.ba.sak.cucumber.domeneparser.parseList
import no.nav.familie.ba.sak.cucumber.domeneparser.parseLong
import no.nav.familie.ba.sak.cucumber.domeneparser.parseValgfriBoolean
import no.nav.familie.ba.sak.cucumber.domeneparser.parseValgfriDato
import no.nav.familie.ba.sak.cucumber.domeneparser.parseValgfriEnum
import no.nav.familie.ba.sak.cucumber.domeneparser.parseValgfriInt
import no.nav.familie.ba.sak.cucumber.domeneparser.parseValgfriLong
import no.nav.familie.ba.sak.cucumber.domeneparser.parseValgfriString
import no.nav.familie.ba.sak.cucumber.domeneparser.parseValgfriStringList
import no.nav.familie.ba.sak.ekstern.restDomene.BarnMedOpplysninger
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.InternPeriodeOvergangsstønad
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.beregning.domene.slåSammenTidligerePerioder
import no.nav.familie.ba.sak.kjerne.beregning.splittOgSlåSammen
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseAktivitet
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.lagDødsfall
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.EØSStandardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.domene.EØSBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Vedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksbegrunnelseFritekst
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.UtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.tilVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.BehandlingsGrunnlagForVedtaksperioder
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.genererVedtaksperioder
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import java.math.BigDecimal
import java.time.LocalDate

fun Map<Long, Behandling>.finnBehandling(behandlingId: Long) =
    this[behandlingId] ?: error("Finner ikke behandling med id $behandlingId")

fun Map<Long, PersonopplysningGrunnlag>.finnPersonGrunnlagForBehandling(behandlingId: Long): PersonopplysningGrunnlag =
    this[behandlingId] ?: error("Finner ikke persongrunnlag for behandling med id $behandlingId")

fun lagFagsaker(dataTable: DataTable) = dataTable.asMaps().map { rad ->
    Fagsak(
        id = parseLong(Domenebegrep.FAGSAK_ID, rad),
        type = parseValgfriEnum<FagsakType>(Domenebegrep.FAGSAK_TYPE, rad) ?: FagsakType.NORMAL,
        aktør = randomAktør(),
    )
}.associateBy { it.id }

fun lagVedtak(
    dataTable: DataTable,
    behandlinger: MutableMap<Long, Behandling>,
    behandlingTilForrigeBehandling: MutableMap<Long, Long?>,
    vedtaksListe: MutableList<Vedtak>,
    fagsaker: Map<Long, Fagsak>,
) {
    behandlinger.putAll(
        dataTable.asMaps().map { rad ->
            val behandlingId = parseLong(Domenebegrep.BEHANDLING_ID, rad)
            val fagsakId = parseValgfriLong(Domenebegrep.FAGSAK_ID, rad)
            val fagsak = fagsaker[fagsakId] ?: defaultFagsak()
            val behandlingÅrsak = parseValgfriEnum<BehandlingÅrsak>(Domenebegrep.BEHANDLINGSÅRSAK, rad)
            val behandlingResultat = parseValgfriEnum<Behandlingsresultat>(Domenebegrep.BEHANDLINGSRESULTAT, rad)

            lagBehandling(
                fagsak = fagsak,
                årsak = behandlingÅrsak ?: BehandlingÅrsak.SØKNAD,
                resultat = behandlingResultat ?: Behandlingsresultat.IKKE_VURDERT,
            ).copy(id = behandlingId)
        }.associateBy { it.id },
    )
    behandlingTilForrigeBehandling.putAll(
        dataTable.asMaps().associate { rad ->
            parseLong(Domenebegrep.BEHANDLING_ID, rad) to parseValgfriLong(Domenebegrep.FORRIGE_BEHANDLING_ID, rad)
        },
    )
    vedtaksListe.addAll(
        dataTable.groupByBehandlingId()
            .map { no.nav.familie.ba.sak.common.lagVedtak(behandlinger[it.key] ?: error("Finner ikke behandling")) },
    )
}

fun lagPersonresultater(
    persongrunnlagForBehandling: PersonopplysningGrunnlag,
    behandling: Behandling,
) = persongrunnlagForBehandling.personer.map { person ->
    lagPersonResultat(
        vilkårsvurdering = lagVilkårsvurdering(person.aktør, behandling, Resultat.OPPFYLT),
        person = person,
        resultat = Resultat.OPPFYLT,
        personType = person.type,
        lagFullstendigVilkårResultat = true,
        periodeFom = null,
        periodeTom = null,
    )
}.toSet()

fun leggTilVilkårResultatPåPersonResultat(
    personResultatForBehandling: Set<PersonResultat>,
    vilkårResultaterPerPerson: Map<String, List<MutableMap<String, String>>>,
    behandlingId: Long,
) = personResultatForBehandling.map { personResultat ->
    personResultat.vilkårResultater.clear()

    vilkårResultaterPerPerson[personResultat.aktør.aktørId]?.forEach { rad ->
        val vilkårForÉnRad = parseEnumListe<Vilkår>(
            VedtaksperiodeMedBegrunnelserParser.DomenebegrepVedtaksperiodeMedBegrunnelser.VILKÅR,
            rad,
        )

        val utdypendeVilkårsvurderingForÉnRad = parseEnumListe<UtdypendeVilkårsvurdering>(
            VedtaksperiodeMedBegrunnelserParser.DomenebegrepVedtaksperiodeMedBegrunnelser.UTDYPENDE_VILKÅR,
            rad,
        )

        val vurderesEtterForEnRad = parseValgfriEnum<Regelverk>(
            VedtaksperiodeMedBegrunnelserParser.DomenebegrepVedtaksperiodeMedBegrunnelser.VURDERES_ETTER,
            rad,
        ) ?: Regelverk.NASJONALE_REGLER

        val vilkårResultaterForÉnRad = vilkårForÉnRad.map { vilkår ->
            VilkårResultat(
                sistEndretIBehandlingId = behandlingId,
                personResultat = personResultat,
                vilkårType = vilkår,
                resultat = parseEnum(
                    VedtaksperiodeMedBegrunnelserParser.DomenebegrepVedtaksperiodeMedBegrunnelser.RESULTAT,
                    rad,
                ),
                periodeFom = parseValgfriDato(Domenebegrep.FRA_DATO, rad),
                periodeTom = parseValgfriDato(Domenebegrep.TIL_DATO, rad),
                erEksplisittAvslagPåSøknad = parseValgfriBoolean(
                    VedtaksperiodeMedBegrunnelserParser.DomenebegrepVedtaksperiodeMedBegrunnelser.ER_EKSPLISITT_AVSLAG,
                    rad,
                ),
                begrunnelse = "",
                utdypendeVilkårsvurderinger = utdypendeVilkårsvurderingForÉnRad,
                vurderesEtter = vurderesEtterForEnRad,
            )
        }
        personResultat.vilkårResultater.addAll(vilkårResultaterForÉnRad)
    }
    personResultat
}.toSet()

fun lagKompetanser(
    nyeKompetanserPerBarn: MutableList<MutableMap<String, String>>,
    personopplysningGrunnlag: Map<Long, PersonopplysningGrunnlag>,
) =
    nyeKompetanserPerBarn.map { rad ->
        val aktørerForKompetanse = VedtaksperiodeMedBegrunnelserParser.parseAktørIdListe(rad)
        val behandlingId = parseLong(Domenebegrep.BEHANDLING_ID, rad)
        Kompetanse(
            fom = parseValgfriDato(Domenebegrep.FRA_DATO, rad)?.toYearMonth(),
            tom = parseValgfriDato(Domenebegrep.TIL_DATO, rad)?.toYearMonth(),
            barnAktører = personopplysningGrunnlag.finnPersonGrunnlagForBehandling(behandlingId).personer
                .filter { aktørerForKompetanse.contains(it.aktør.aktørId) }
                .map { it.aktør }
                .toSet(),
            søkersAktivitet = parseValgfriEnum<KompetanseAktivitet>(
                VedtaksperiodeMedBegrunnelserParser.DomenebegrepKompetanse.SØKERS_AKTIVITET,
                rad,
            )
                ?: KompetanseAktivitet.ARBEIDER,
            annenForeldersAktivitet =
            parseValgfriEnum<KompetanseAktivitet>(
                VedtaksperiodeMedBegrunnelserParser.DomenebegrepKompetanse.ANNEN_FORELDERS_AKTIVITET,
                rad,
            )
                ?: KompetanseAktivitet.I_ARBEID,
            søkersAktivitetsland = parseValgfriString(
                VedtaksperiodeMedBegrunnelserParser.DomenebegrepKompetanse.SØKERS_AKTIVITETSLAND,
                rad,
            )?.also { validerErLandkode(it) } ?: "PL",
            annenForeldersAktivitetsland = parseValgfriString(
                VedtaksperiodeMedBegrunnelserParser.DomenebegrepKompetanse.ANNEN_FORELDERS_AKTIVITETSLAND,
                rad,
            )?.also { validerErLandkode(it) } ?: "NO",
            barnetsBostedsland = parseValgfriString(
                VedtaksperiodeMedBegrunnelserParser.DomenebegrepKompetanse.BARNETS_BOSTEDSLAND,
                rad,
            )?.also { validerErLandkode(it) } ?: "NO",
            resultat = parseEnum<KompetanseResultat>(
                VedtaksperiodeMedBegrunnelserParser.DomenebegrepKompetanse.RESULTAT,
                rad,
            ),
        ).also { it.behandlingId = behandlingId }
    }.groupBy { it.behandlingId }
        .toMutableMap()

private fun validerErLandkode(it: String) {
    if (it.length != 2) {
        error("$it er ikke en landkode")
    }
}

fun lagEndredeUtbetalinger(
    nyeEndredeUtbetalingAndeler: MutableList<MutableMap<String, String>>,
    persongrunnlag: Map<Long, PersonopplysningGrunnlag>,
) =
    nyeEndredeUtbetalingAndeler.map { rad ->
        val aktørId = VedtaksperiodeMedBegrunnelserParser.parseAktørId(rad)
        val behandlingId = parseLong(Domenebegrep.BEHANDLING_ID, rad)
        EndretUtbetalingAndel(
            behandlingId = behandlingId,
            fom = parseValgfriDato(Domenebegrep.FRA_DATO, rad)?.toYearMonth(),
            tom = parseValgfriDato(Domenebegrep.TIL_DATO, rad)?.toYearMonth(),
            person = persongrunnlag.finnPersonGrunnlagForBehandling(behandlingId).personer.find { aktørId == it.aktør.aktørId },
            prosent = parseValgfriLong(
                VedtaksperiodeMedBegrunnelserParser.DomenebegrepEndretUtbetaling.PROSENT,
                rad,
            )?.toBigDecimal() ?: BigDecimal.valueOf(100),
            årsak = parseValgfriEnum<Årsak>(VedtaksperiodeMedBegrunnelserParser.DomenebegrepEndretUtbetaling.ÅRSAK, rad)
                ?: Årsak.ALLEREDE_UTBETALT,
            søknadstidspunkt = LocalDate.now(),
            begrunnelse = "Fordi at...",
            avtaletidspunktDeltBosted = LocalDate.now(),
        )
    }.groupBy { it.behandlingId }
        .toMutableMap()

fun lagPersonGrunnlag(dataTable: DataTable): Map<Long, PersonopplysningGrunnlag> {
    return dataTable.asMaps().map { rad ->
        val behandlingsIder = parseList(Domenebegrep.BEHANDLING_ID, rad)
        behandlingsIder.map { id ->
            id to tilfeldigPerson(
                personType = parseEnum(
                    VedtaksperiodeMedBegrunnelserParser.DomenebegrepPersongrunnlag.PERSON_TYPE,
                    rad,
                ),
                fødselsdato = parseDato(
                    VedtaksperiodeMedBegrunnelserParser.DomenebegrepPersongrunnlag.FØDSELSDATO,
                    rad,
                ),
                aktør = randomAktør().copy(aktørId = VedtaksperiodeMedBegrunnelserParser.parseAktørId(rad)),
            ).also { person ->
                parseValgfriDato(
                    VedtaksperiodeMedBegrunnelserParser.DomenebegrepPersongrunnlag.DØDSFALLDATO,
                    rad,
                )?.let { person.dødsfall = lagDødsfall(person = person, dødsfallDato = it) }
            }
        }
    }.flatten()
        .groupBy({ it.first }, { it.second })
        .map { (behandlingId, personer) ->
            PersonopplysningGrunnlag(
                behandlingId = behandlingId,
                personer = personer.toMutableSet(),
            )
        }.associateBy { it.behandlingId }
}

fun lagAndelerTilkjentYtelse(
    dataTable: DataTable,
    behandlinger: MutableMap<Long, Behandling>,
    personGrunnlag: Map<Long, PersonopplysningGrunnlag>,
) = dataTable.asMaps().map { rad ->
    val aktørId = VedtaksperiodeMedBegrunnelserParser.parseAktørId(rad)
    val behandlingId = parseLong(Domenebegrep.BEHANDLING_ID, rad)
    val beløp = parseInt(VedtaksperiodeMedBegrunnelserParser.DomenebegrepVedtaksperiodeMedBegrunnelser.BELØP, rad)
    lagAndelTilkjentYtelse(
        fom = parseDato(Domenebegrep.FRA_DATO, rad).toYearMonth(),
        tom = parseDato(Domenebegrep.TIL_DATO, rad).toYearMonth(),
        behandling = behandlinger.finnBehandling(behandlingId),
        person = personGrunnlag.finnPersonGrunnlagForBehandling(behandlingId).personer.find { aktørId == it.aktør.aktørId }!!,
        beløp = beløp,
        ytelseType = parseValgfriEnum<YtelseType>(
            VedtaksperiodeMedBegrunnelserParser.DomenebegrepAndelTilkjentYtelse.YTELSE_TYPE,
            rad,
        ) ?: YtelseType.ORDINÆR_BARNETRYGD,
        prosent = parseValgfriLong(
            VedtaksperiodeMedBegrunnelserParser.DomenebegrepEndretUtbetaling.PROSENT,
            rad,
        )?.toBigDecimal() ?: BigDecimal(100),
        sats = parseValgfriInt(
            VedtaksperiodeMedBegrunnelserParser.DomenebegrepVedtaksperiodeMedBegrunnelser.SATS,
            rad,
        ) ?: beløp,
    )
}.groupBy { it.behandlingId }
    .toMutableMap()

fun lagOvergangsstønad(
    dataTable: DataTable,
    persongrunnlag: Map<Long, PersonopplysningGrunnlag>,
    tidligereBehandlinger: Map<Long, Long?>,
    dagensDato: LocalDate,
): Map<Long, List<InternPeriodeOvergangsstønad>> {
    val overgangsstønadPeriodePåBehandlinger = dataTable.asMaps()
        .groupBy({ rad -> parseLong(Domenebegrep.BEHANDLING_ID, rad) }, { rad ->
            val behandlingId = parseLong(Domenebegrep.BEHANDLING_ID, rad)
            val aktørId = VedtaksperiodeMedBegrunnelserParser.parseAktørId(rad)

            InternPeriodeOvergangsstønad(
                fomDato = parseDato(Domenebegrep.FRA_DATO, rad),
                tomDato = parseDato(Domenebegrep.TIL_DATO, rad),
                personIdent = persongrunnlag[behandlingId]!!.personer.single { it.aktør.aktørId == aktørId }.aktør.aktivFødselsnummer(),
            )
        })

    return overgangsstønadPeriodePåBehandlinger.mapValues { (behandlingId, overgangsstønad) ->
        overgangsstønad.splittOgSlåSammen(
            overgangsstønadPeriodePåBehandlinger[tidligereBehandlinger[behandlingId]]?.slåSammenTidligerePerioder(
                dagensDato,
            ) ?: emptyList(),
            dagensDato,
        )
    }
}

fun lagVedtaksPerioder(
    behandlingId: Long,
    vedtaksListe: List<Vedtak>,
    behandlingTilForrigeBehandling: MutableMap<Long, Long?>,
    personGrunnlag: Map<Long, PersonopplysningGrunnlag>,
    personResultater: Map<Long, Set<PersonResultat>>,
    kompetanser: Map<Long, List<Kompetanse>>,
    endredeUtbetalinger: Map<Long, List<EndretUtbetalingAndel>>,
    andelerTilkjentYtelse: Map<Long, List<AndelTilkjentYtelse>>,
    overstyrteEndringstidspunkt: Map<Long, LocalDate?>,
    overgangsstønad: Map<Long, List<InternPeriodeOvergangsstønad>?>,
    uregistrerteBarn: List<BarnMedOpplysninger>,
    nåDato: LocalDate,
): List<VedtaksperiodeMedBegrunnelser> {
    val vedtak = vedtaksListe.find { it.behandling.id == behandlingId && it.aktiv }
        ?: error("Finner ikke vedtak")

    vedtak.behandling.overstyrtEndringstidspunkt = overstyrteEndringstidspunkt[behandlingId]
    val grunnlagForVedtaksperiode = BehandlingsGrunnlagForVedtaksperioder(
        persongrunnlag = personGrunnlag.finnPersonGrunnlagForBehandling(behandlingId),
        personResultater = personResultater[behandlingId] ?: error("Finner ikke personresultater"),
        fagsakType = vedtak.behandling.fagsak.type,
        kompetanser = kompetanser[behandlingId] ?: emptyList(),
        endredeUtbetalinger = endredeUtbetalinger[behandlingId] ?: emptyList(),
        andelerTilkjentYtelse = andelerTilkjentYtelse[behandlingId] ?: emptyList(),
        perioderOvergangsstønad = overgangsstønad[behandlingId] ?: emptyList(),
        uregistrerteBarn = uregistrerteBarn,
    )

    val forrigeBehandlingId = behandlingTilForrigeBehandling[behandlingId]

    val grunnlagForVedtaksperiodeForrigeBehandling = forrigeBehandlingId?.let {
        val forrigeVedtak = vedtaksListe.find { it.behandling.id == forrigeBehandlingId && it.aktiv }
            ?: error("Finner ikke vedtak")
        BehandlingsGrunnlagForVedtaksperioder(
            persongrunnlag = personGrunnlag.finnPersonGrunnlagForBehandling(forrigeBehandlingId),
            personResultater = personResultater[forrigeBehandlingId] ?: error("Finner ikke personresultater"),
            fagsakType = forrigeVedtak.behandling.fagsak.type,
            kompetanser = kompetanser[forrigeBehandlingId] ?: emptyList(),
            endredeUtbetalinger = endredeUtbetalinger[forrigeBehandlingId] ?: emptyList(),
            andelerTilkjentYtelse = andelerTilkjentYtelse[forrigeBehandlingId] ?: emptyList(),
            perioderOvergangsstønad = overgangsstønad[behandlingId] ?: emptyList(),
            uregistrerteBarn = emptyList(),
        )
    }

    return genererVedtaksperioder(
        vedtak = vedtak,
        grunnlagForVedtakPerioder = grunnlagForVedtaksperiode,
        grunnlagForVedtakPerioderForrigeBehandling = grunnlagForVedtaksperiodeForrigeBehandling,
        nåDato = nåDato,
    )
}

fun leggBegrunnelserIVedtaksperiodene(
    dataTable: DataTable,
    vedtaksperioder: List<UtvidetVedtaksperiodeMedBegrunnelser>,
    vedtak: Vedtak,
) = dataTable.asMaps().map { rad ->
    val fom = parseValgfriDato(Domenebegrep.FRA_DATO, rad)
    val tom = parseValgfriDato(Domenebegrep.TIL_DATO, rad)

    val vedtaksperiode =
        vedtaksperioder.find { it.fom == fom && it.tom == tom }
            ?: throw Feil(
                "Ingen vedtaksperioder med Fom=$fom og Tom=$tom. " +
                    "Vedtaksperiodene var ${vedtaksperioder.map { "\n${it.fom?.tilddMMyyyy()} til ${it.tom?.tilddMMyyyy()}" }}",
            )
    val vedtaksperiodeMedBegrunnelser = vedtaksperiode.tilVedtaksperiodeMedBegrunnelser(
        vedtak,
    )

    val standardbegrunnelser = parseEnumListe<Standardbegrunnelse>(
        VedtaksperiodeMedBegrunnelserParser.DomenebegrepVedtaksperiodeMedBegrunnelser.STANDARDBEGRUNNELSER,
        rad,
    ).map {
        Vedtaksbegrunnelse(
            vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
            standardbegrunnelse = it,
        )
    }.toMutableSet()
    val eøsBegrunnelser = parseEnumListe<EØSStandardbegrunnelse>(
        VedtaksperiodeMedBegrunnelserParser.DomenebegrepVedtaksperiodeMedBegrunnelser.EØSBEGRUNNELSER,
        rad,
    ).map { EØSBegrunnelse(vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser, begrunnelse = it) }
        .toMutableSet()
    val fritekster = parseValgfriStringList(
        VedtaksperiodeMedBegrunnelserParser.DomenebegrepVedtaksperiodeMedBegrunnelser.FRITEKSTER,
        rad,
    ).map {
        VedtaksbegrunnelseFritekst(
            vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
            fritekst = it,
        )
    }.toMutableList()

    vedtaksperiodeMedBegrunnelser
        .copy(begrunnelser = standardbegrunnelser, eøsBegrunnelser = eøsBegrunnelser, fritekster = fritekster)
}
