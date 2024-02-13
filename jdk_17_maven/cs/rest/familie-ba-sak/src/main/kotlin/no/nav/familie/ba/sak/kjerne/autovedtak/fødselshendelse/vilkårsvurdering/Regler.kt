package no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.vilkårsvurdering

import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.common.Periode
import no.nav.familie.ba.sak.common.isBetween
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Evaluering
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.vilkårsvurdering.utfall.VilkårIkkeOppfyltÅrsak
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.vilkårsvurdering.utfall.VilkårKanskjeOppfyltÅrsak
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.vilkårsvurdering.utfall.VilkårOppfyltÅrsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Medlemskap
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.arbeidsforhold.GrArbeidsforhold
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.arbeidsforhold.harLøpendeArbeidsforhold
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.GrBostedsadresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.filtrerGjeldendeNå
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.vurderOmPersonerBorSammen
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.opphold.GrOpphold
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.opphold.gyldigGjeldendeOppholdstillatelseFødselshendelse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.sivilstand.GrSivilstand
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.GrStatsborgerskap
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.hentSterkesteMedlemskap
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import java.time.Duration
import java.time.LocalDate

interface Vilkårsregel {

    fun vurder(): Evaluering
}

data class VurderPersonErBosattIRiket(
    val adresser: List<GrBostedsadresse>,
    val vurderFra: LocalDate,
) : Vilkårsregel {

    override fun vurder(): Evaluering {
        if (adresser.any { !it.harGyldigFom() }) {
            val person = adresser.first().person
            secureLogger.info(
                "Har ugyldige adresser på person (${person?.aktør?.aktivFødselsnummer()}, ${person?.type}): ${
                    adresser.filter { !it.harGyldigFom() }
                        .map { "(${it.periode?.fom}, ${it.periode?.tom}): ${it.toSecureString()}" }
                }",
            )
        }

        if (adresser.isEmpty()) return Evaluering.ikkeOppfylt(VilkårIkkeOppfyltÅrsak.BOR_IKKE_I_RIKET)
        if (harPersonKunAdresserUtenFom(adresser)) {
            return Evaluering.oppfylt(VilkårOppfyltÅrsak.BOR_I_RIKET_KUN_ADRESSER_UTEN_FOM)
        } else if (harPersonBoddPåSisteAdresseMinstFraVurderingstidspunkt(adresser, vurderFra)) {
            return Evaluering.oppfylt(
                VilkårOppfyltÅrsak.BOR_I_RIKET,
            )
        } else if (adresser.filter { !it.harGyldigFom() }.size > 1) return Evaluering.ikkeOppfylt(VilkårIkkeOppfyltÅrsak.BOR_IKKE_I_RIKET_FLERE_ADRESSER_UTEN_FOM)

        val adresserMedGyldigFom = adresser.filter { it.harGyldigFom() }

        /**
         * En person med registrert bostedsadresse er bosatt i Norge.
         * En person som mangler registrert bostedsadresse er utflyttet.
         * See: https://navikt.github.io/pdl/#_utflytting
         */
        return if (adresserMedGyldigFom.isNotEmpty() && erPersonBosattFraVurderingstidspunktet(
                adresserMedGyldigFom,
                vurderFra,
            )
        ) {
            Evaluering.oppfylt(VilkårOppfyltÅrsak.BOR_I_RIKET)
        } else {
            Evaluering.ikkeOppfylt(VilkårIkkeOppfyltÅrsak.BOR_IKKE_I_RIKET)
        }
    }

    /**
     * Bruker har kun adresser uten fom som betyr at vedkommende har bodd på samme
     * adresse hele livet eller flyttet før man innførte fom ved flytting.
     */
    private fun harPersonKunAdresserUtenFom(adresser: List<GrBostedsadresse>): Boolean =
        adresser.all { !it.harGyldigFom() }

    private fun harPersonBoddPåSisteAdresseMinstFraVurderingstidspunkt(
        adresser: List<GrBostedsadresse>,
        vurderFra: LocalDate,
    ): Boolean {
        val sisteAdresse = adresser
            .filter { it.harGyldigFom() }
            .maxByOrNull { it.periode?.fom!! } ?: return false

        return sisteAdresse.periode?.fom!!.toYearMonth()
            .isBefore(vurderFra.toYearMonth()) && sisteAdresse.periode?.tom == null
    }

    private fun erPersonBosattFraVurderingstidspunktet(adresser: List<GrBostedsadresse>, vurderFra: LocalDate) =
        hentMaxAvstandAvDagerMellomPerioder(
            adresser.mapNotNull { it.periode },
            vurderFra,
            LocalDate.now(),
        ) == 0L
}

data class VurderBarnErUnder18(
    val alder: Int,
) : Vilkårsregel {

    override fun vurder(): Evaluering =
        if (alder < 18) {
            Evaluering.oppfylt(VilkårOppfyltÅrsak.ER_UNDER_18_ÅR)
        } else {
            Evaluering.ikkeOppfylt(VilkårIkkeOppfyltÅrsak.ER_IKKE_UNDER_18_ÅR)
        }
}

data class VurderBarnErBosattMedSøker(
    val søkerAdresser: List<GrBostedsadresse>,
    val barnAdresser: List<GrBostedsadresse>,
) : Vilkårsregel {

    override fun vurder(): Evaluering {
        return if (vurderOmPersonerBorSammen(
                adresser = barnAdresser,
                andreAdresser = søkerAdresser,
            )
        ) {
            Evaluering.oppfylt(VilkårOppfyltÅrsak.BARNET_BOR_MED_MOR)
        } else {
            Evaluering.ikkeOppfylt(VilkårIkkeOppfyltÅrsak.BARNET_BOR_IKKE_MED_MOR)
        }
    }
}

data class VurderBarnErUgift(
    val sivilstander: List<GrSivilstand>,
) : Vilkårsregel {

    override fun vurder(): Evaluering {
        val sivilstanderMedGyldigFom = sivilstander.filter { it.harGyldigFom() }

        return when {
            sivilstanderMedGyldigFom.singleOrNull { it.type == SIVILSTAND.UOPPGITT } != null ->
                Evaluering.oppfylt(VilkårOppfyltÅrsak.BARN_MANGLER_SIVILSTAND)
            sivilstanderMedGyldigFom.any { it.type == SIVILSTAND.GIFT || it.type == SIVILSTAND.REGISTRERT_PARTNER } ->
                Evaluering.ikkeOppfylt(VilkårIkkeOppfyltÅrsak.BARN_ER_GIFT_ELLER_HAR_PARTNERSKAP)
            else -> Evaluering.oppfylt(VilkårOppfyltÅrsak.BARN_ER_IKKE_GIFT_ELLER_HAR_PARTNERSKAP)
        }
    }
}

data class VurderBarnHarLovligOpphold(
    val aktør: Aktør,
) : Vilkårsregel {
    override fun vurder(): Evaluering {
        return Evaluering.oppfylt(VilkårOppfyltÅrsak.AUTOMATISK_VURDERING_BARN_LOVLIG_OPPHOLD)
    }
}

data class LovligOppholdFaktaEØS(
    val arbeidsforhold: List<GrArbeidsforhold>,
    val bostedsadresser: List<GrBostedsadresse>,
    val statsborgerskap: List<GrStatsborgerskap>,
)

data class VurderPersonHarLovligOpphold(
    val morLovligOppholdFaktaEØS: LovligOppholdFaktaEØS,
    val annenForelderLovligOppholdFaktaEØS: LovligOppholdFaktaEØS?,
    val opphold: List<GrOpphold>,
) : Vilkårsregel {

    override fun vurder(): Evaluering {
        return when (morLovligOppholdFaktaEØS.statsborgerskap.hentSterkesteMedlemskap()) {
            Medlemskap.NORDEN -> Evaluering.oppfylt(VilkårOppfyltÅrsak.NORDISK_STATSBORGER)
            Medlemskap.TREDJELANDSBORGER -> {
                val morErUkrainskStatsborger = morLovligOppholdFaktaEØS.statsborgerskap.any { it.landkode == "UKR" }
                // Midlertidig regel for Ukrainakonflikten
                if (morErUkrainskStatsborger) {
                    Evaluering.ikkeVurdert(VilkårKanskjeOppfyltÅrsak.LOVLIG_OPPHOLD_MÅ_VURDERE_LENGDEN_PÅ_OPPHOLDSTILLATELSEN)
                } else if (opphold.gyldigGjeldendeOppholdstillatelseFødselshendelse()) {
                    Evaluering.oppfylt(VilkårOppfyltÅrsak.TREDJELANDSBORGER_MED_LOVLIG_OPPHOLD)
                } else {
                    Evaluering.ikkeOppfylt(VilkårIkkeOppfyltÅrsak.TREDJELANDSBORGER_UTEN_LOVLIG_OPPHOLD)
                }
            }
            Medlemskap.EØS -> vurderLovligOppholdForEØSBorger(
                morLovligOppholdFaktaEØS,
                annenForelderLovligOppholdFaktaEØS,
            )
            Medlemskap.STATSLØS, Medlemskap.UKJENT -> {
                if (opphold.gyldigGjeldendeOppholdstillatelseFødselshendelse()) {
                    Evaluering.oppfylt(VilkårOppfyltÅrsak.UKJENT_STATSBORGERSKAP_MED_LOVLIG_OPPHOLD)
                } else {
                    Evaluering.ikkeOppfylt(VilkårIkkeOppfyltÅrsak.STATSLØS)
                }
            }
            else -> Evaluering.ikkeVurdert(VilkårKanskjeOppfyltÅrsak.LOVLIG_OPPHOLD_IKKE_MULIG_Å_FASTSETTE)
        }
    }
}

private fun vurderLovligOppholdForEØSBorger(
    morLovligOppholdFaktaEØS: LovligOppholdFaktaEØS,
    annenForelderLovligOppholdFaktaEØS: LovligOppholdFaktaEØS?,
): Evaluering {
    if (morLovligOppholdFaktaEØS.arbeidsforhold.harLøpendeArbeidsforhold()) {
        return Evaluering.oppfylt(VilkårOppfyltÅrsak.EØS_MED_LØPENDE_ARBEIDSFORHOLD)
    }

    if (annenForelderLovligOppholdFaktaEØS == null) {
        return Evaluering.ikkeOppfylt(VilkårIkkeOppfyltÅrsak.EØS_STATSBORGERSKAP_ANNEN_FORELDER_UKLART)
    }

    if (!vurderOmPersonerBorSammen(
            adresser = morLovligOppholdFaktaEØS.bostedsadresser.filtrerGjeldendeNå(),
            andreAdresser = annenForelderLovligOppholdFaktaEØS.bostedsadresser.filtrerGjeldendeNå(),
        )
    ) {
        return Evaluering.ikkeOppfylt(VilkårIkkeOppfyltÅrsak.EØS_BOR_IKKE_SAMMEN_MED_ANNEN_FORELDER)
    }

    return when (annenForelderLovligOppholdFaktaEØS.statsborgerskap.hentSterkesteMedlemskap()) {
        Medlemskap.NORDEN -> Evaluering.oppfylt(VilkårOppfyltÅrsak.ANNEN_FORELDER_NORDISK)
        Medlemskap.EØS -> {
            if (annenForelderLovligOppholdFaktaEØS.arbeidsforhold.harLøpendeArbeidsforhold()) {
                Evaluering.oppfylt(VilkårOppfyltÅrsak.ANNEN_FORELDER_EØS_MEN_MED_LØPENDE_ARBEIDSFORHOLD)
            } else {
                Evaluering.ikkeOppfylt(VilkårIkkeOppfyltÅrsak.EØS_ANNEN_FORELDER_EØS_MEN_IKKE_MED_LØPENDE_ARBEIDSFORHOLD)
            }
        }
        Medlemskap.TREDJELANDSBORGER -> Evaluering.ikkeOppfylt(VilkårIkkeOppfyltÅrsak.EØS_MEDFORELDER_TREDJELANDSBORGER)
        Medlemskap.STATSLØS, Medlemskap.UKJENT -> Evaluering.ikkeOppfylt(VilkårIkkeOppfyltÅrsak.EØS_MEDFORELDER_STATSLØS)
        else -> Evaluering.ikkeVurdert(VilkårKanskjeOppfyltÅrsak.LOVLIG_OPPHOLD_ANNEN_FORELDER_IKKE_MULIG_Å_FASTSETTE)
    }
}

private fun hentMaxAvstandAvDagerMellomPerioder(
    perioder: List<DatoIntervallEntitet>,
    fom: LocalDate,
    tom: LocalDate,
): Long {
    val perioderMedTilkobletTom =
        perioder.sortedBy { it.fom }
            .fold(mutableListOf()) { acc: MutableList<DatoIntervallEntitet>, datoIntervallEntitet: DatoIntervallEntitet ->
                if (acc.isNotEmpty() && acc.last().tom == null) {
                    val sisteDatoIntervall = acc.last().copy(
                        tom = datoIntervallEntitet.fom?.minusDays(1),
                    )

                    acc.removeLast()
                    acc.add(sisteDatoIntervall)
                }

                acc.add(datoIntervallEntitet)
                acc.sortBy { it.fom }
                acc
            }
            .toList()

    val perioderInnenAngittTidsrom =
        perioderMedTilkobletTom.filter {
            it.tom == null ||
                fom.isBetween(
                    Periode(
                        fom = it.fom!!,
                        tom = it.tom,
                    ),
                ) ||
                tom.isBetween(
                    Periode(
                        fom = it.fom,
                        tom = it.tom,
                    ),
                ) ||
                it.fom >= fom && it.tom <= tom
        }

    if (perioderInnenAngittTidsrom.isEmpty()) return Duration.between(fom.atStartOfDay(), tom.atStartOfDay()).toDays()

    val defaultAvstand = if (perioderInnenAngittTidsrom.first().fom!!.isAfter(fom)) {
        Duration.between(
            fom.atStartOfDay(),
            perioderInnenAngittTidsrom.first().fom!!.atStartOfDay(),
        )
            .toDays()
    } else if (perioderInnenAngittTidsrom.last().tom != null && perioderInnenAngittTidsrom.last().tom!!.isBefore(tom)) {
        Duration.between(
            perioderInnenAngittTidsrom.last().tom!!.atStartOfDay(),
            tom.atStartOfDay(),
        ).toDays()
    } else {
        0L
    }

    return perioderInnenAngittTidsrom
        .zipWithNext()
        .fold(defaultAvstand) { maksimumAvstand, pairs ->
            val avstand =
                Duration.between(pairs.first.tom!!.atStartOfDay().plusDays(1), pairs.second.fom!!.atStartOfDay())
                    .toDays()
            maxOf(avstand, maksimumAvstand)
        }
}
