package no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Evaluering
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler.utfall.FiltreringsregelIkkeOppfylt
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler.utfall.FiltreringsregelOppfylt
import java.time.temporal.ChronoUnit
import kotlin.math.abs

enum class Filtreringsregel(val vurder: FiltreringsreglerFakta.() -> Evaluering) {
    MOR_GYLDIG_FNR(vurder = { FiltreringsregelEvaluering.morHarGyldigFnr(this) }),
    BARN_GYLDIG_FNR(vurder = { FiltreringsregelEvaluering.barnHarGyldigFnr(this) }),
    MOR_LEVER(vurder = { FiltreringsregelEvaluering.morLever(this) }),
    BARN_LEVER(vurder = { FiltreringsregelEvaluering.barnLever(this) }),
    MER_ENN_5_MND_SIDEN_FORRIGE_BARN(vurder = {
        FiltreringsregelEvaluering.merEnn5mndEllerMindreEnnFemDagerSidenForrigeBarn(
            this,
        )
    }),
    MOR_ER_OVER_18_ÅR(vurder = { FiltreringsregelEvaluering.morErOver18år(this) }),
    MOR_HAR_IKKE_VERGE(vurder = { FiltreringsregelEvaluering.morHarIkkeVerge(this) }),
    MOR_MOTTAR_IKKE_LØPENDE_UTVIDET(vurder = { FiltreringsregelEvaluering.morMottarIkkeLøpendeUtvidet(this) }),
    MOR_HAR_IKKE_LØPENDE_EØS_BARNETRYGD(vurder = { FiltreringsregelEvaluering.morHarIkkeLøpendeEøsBarnetrygd(this) }),
    FAGSAK_IKKE_MIGRERT_UT_AV_INFOTRYGD_ETTER_BARN_FØDT(vurder = {
        FiltreringsregelEvaluering.fagsakIkkeMigrertEtterBarnBleFødt(
            this,
        )
    }),
    LØPER_IKKE_BARNETRYGD_FOR_BARNET(vurder = { FiltreringsregelEvaluering.løperIkkeBarnetrygdPåAnnenForelder(this) }),
    MOR_HAR_IKKE_OPPFYLT_UTVIDET_VILKÅR_VED_FØDSELSDATO(vurder = {
        FiltreringsregelEvaluering.morOppfyllerIkkeVilkårForUtvidetBarnetrygd(
            this,
        )
    }),
    MOR_HAR_IKKE_OPPHØRT_BARNETRYGD(vurder = { FiltreringsregelEvaluering.morHarIkkeOpphørtBarnetrygd(this) }),
}

object FiltreringsregelEvaluering {
    fun evaluerFiltreringsregler(fakta: FiltreringsreglerFakta) = Filtreringsregel.values()
        .fold(mutableListOf<Evaluering>()) { acc, filtreringsregel ->
            if (acc.any { it.resultat == Resultat.IKKE_OPPFYLT }) {
                acc.add(
                    Evaluering(
                        resultat = Resultat.IKKE_VURDERT,
                        identifikator = filtreringsregel.name,
                        begrunnelse = "Ikke vurdert",
                        evalueringÅrsaker = emptyList(),
                    ),
                )
            } else {
                acc.add(filtreringsregel.vurder(fakta).copy(identifikator = filtreringsregel.name))
            }

            acc
        }

    fun morHarGyldigFnr(fakta: FiltreringsreglerFakta): Evaluering {
        val erMorFnrGyldig =
            (!erBostNummer(fakta.mor.aktør.aktivFødselsnummer()) && !erFDatnummer(fakta.mor.aktør.aktivFødselsnummer()))

        return if (erMorFnrGyldig) {
            Evaluering.oppfylt(FiltreringsregelOppfylt.MOR_HAR_GYLDIG_FNR)
        } else {
            Evaluering.ikkeOppfylt(
                FiltreringsregelIkkeOppfylt.MOR_HAR_UGYLDIG_FNR,
            )
        }
    }

    fun barnHarGyldigFnr(fakta: FiltreringsreglerFakta): Evaluering {
        val erbarnFnrGyldig =
            fakta.barnaFraHendelse.all { (!erBostNummer(it.aktør.aktivFødselsnummer()) && !erFDatnummer(it.aktør.aktivFødselsnummer())) }

        return if (erbarnFnrGyldig) {
            Evaluering.oppfylt(FiltreringsregelOppfylt.BARN_HAR_GYLDIG_FNR)
        } else {
            Evaluering.ikkeOppfylt(
                FiltreringsregelIkkeOppfylt.BARN_HAR_UGYLDIG_FNR,
            )
        }
    }

    fun morErOver18år(fakta: FiltreringsreglerFakta): Evaluering = if (fakta.mor.hentAlder() >= 18) {
        Evaluering.oppfylt(
            FiltreringsregelOppfylt.MOR_ER_OVER_18_ÅR,
        )
    } else {
        Evaluering.ikkeOppfylt(FiltreringsregelIkkeOppfylt.MOR_ER_UNDER_18_ÅR)
    }

    fun morLever(fakta: FiltreringsreglerFakta): Evaluering =
        if (fakta.morLever) {
            Evaluering.oppfylt(FiltreringsregelOppfylt.MOR_LEVER)
        } else {
            Evaluering.ikkeOppfylt(
                FiltreringsregelIkkeOppfylt.MOR_LEVER_IKKE,
            )
        }

    fun barnLever(fakta: FiltreringsreglerFakta): Evaluering =
        if (fakta.barnaLever) {
            Evaluering.oppfylt(FiltreringsregelOppfylt.BARNET_LEVER)
        } else {
            Evaluering.ikkeOppfylt(
                FiltreringsregelIkkeOppfylt.BARNET_LEVER_IKKE,
            )
        }

    fun morHarIkkeVerge(fakta: FiltreringsreglerFakta): Evaluering = if (!fakta.morHarVerge) {
        Evaluering.oppfylt(
            FiltreringsregelOppfylt.MOR_ER_MYNDIG,
        )
    } else {
        Evaluering.ikkeOppfylt(
            FiltreringsregelIkkeOppfylt.MOR_ER_UNDER_VERGEMÅL,
        )
    }

    fun morMottarIkkeLøpendeUtvidet(fakta: FiltreringsreglerFakta): Evaluering =
        if (!fakta.morMottarLøpendeUtvidet) {
            Evaluering.oppfylt(
                FiltreringsregelOppfylt.MOR_MOTTAR_IKKE_LØPENDE_UTVIDET,
            )
        } else {
            Evaluering.ikkeOppfylt(
                FiltreringsregelIkkeOppfylt.MOR_MOTTAR_LØPENDE_UTVIDET,
            )
        }

    fun morOppfyllerIkkeVilkårForUtvidetBarnetrygd(fakta: FiltreringsreglerFakta): Evaluering =
        if (!fakta.morOppfyllerVilkårForUtvidetBarnetrygdVedFødselsdato) {
            Evaluering.oppfylt(FiltreringsregelOppfylt.MOR_OPPFYLLER_IKKE_VILKÅR_FOR_UTVIDET_BARNETRYGD_VED_FØDSELSDATO)
        } else {
            Evaluering.ikkeOppfylt(FiltreringsregelIkkeOppfylt.MOR_OPPFYLLER_VILKÅR_FOR_UTVIDET_BARNETRYGD_VED_FØDSELSDATO)
        }

    fun morHarIkkeLøpendeEøsBarnetrygd(fakta: FiltreringsreglerFakta): Evaluering =
        if (!fakta.morMottarEøsBarnetrygd) {
            Evaluering.oppfylt(
                FiltreringsregelOppfylt.MOR_HAR_IKKE_LØPENDE_EØS_BARNETRYGD,
            )
        } else {
            Evaluering.ikkeOppfylt(
                FiltreringsregelIkkeOppfylt.MOR_HAR_LØPENDE_EØS_BARNETRYGD,
            )
        }

    fun fagsakIkkeMigrertEtterBarnBleFødt(fakta: FiltreringsreglerFakta): Evaluering =
        if (!fakta.erFagsakenMigrertEtterBarnFødt) {
            Evaluering.oppfylt(
                FiltreringsregelOppfylt.FAGSAK_IKKE_MIGRERT_UT_AV_INFOTRYGD_ETTER_BARN_FØDT,
            )
        } else {
            Evaluering.ikkeOppfylt(
                FiltreringsregelIkkeOppfylt.FAGSAK_MIGRERT_UT_AV_INFOTRYGD_ETTER_BARN_FØDT,
            )
        }

    fun løperIkkeBarnetrygdPåAnnenForelder(fakta: FiltreringsreglerFakta): Evaluering =
        if (!fakta.løperBarnetrygdForBarnetPåAnnenForelder) {
            Evaluering.oppfylt(
                FiltreringsregelOppfylt.LØPER_IKKE_BARNETRYGD_FOR_BARNET,
            )
        } else {
            Evaluering.ikkeOppfylt(
                FiltreringsregelIkkeOppfylt.LØPER_ALLEREDE_FOR_ANNEN_FORELDER,
            )
        }

    fun morHarIkkeOpphørtBarnetrygd(fakta: FiltreringsreglerFakta): Evaluering = if (fakta.morHarIkkeOpphørtBarnetrygd) {
        Evaluering.oppfylt(FiltreringsregelOppfylt.MOR_HAR_IKKE_OPPHØRT_BARNETRYGD)
    } else {
        Evaluering.ikkeOppfylt(FiltreringsregelIkkeOppfylt.MOR_HAR_OPPHØRT_BARNETRYGD)
    }

    fun merEnn5mndEllerMindreEnnFemDagerSidenForrigeBarn(fakta: FiltreringsreglerFakta): Evaluering {
        return when (
            fakta.barnaFraHendelse.all { barnFraHendelse ->
                fakta.restenAvBarna.all {
                    abs(ChronoUnit.MONTHS.between(barnFraHendelse.fødselsdato, it.fødselsdato)) > 5 ||
                        abs(ChronoUnit.DAYS.between(barnFraHendelse.fødselsdato, it.fødselsdato)) <= 6
                }
            }
        ) {
            true -> Evaluering.oppfylt(FiltreringsregelOppfylt.MER_ENN_5_MND_SIDEN_FORRIGE_BARN_UTFALL)
            false -> Evaluering.ikkeOppfylt(FiltreringsregelIkkeOppfylt.MINDRE_ENN_5_MND_SIDEN_FORRIGE_BARN_UTFALL)
        }
    }
}

internal fun erFDatnummer(personIdent: String): Boolean {
    return personIdent.substring(6).toInt() == 0
}

/**
 * BOST-nr har måned mellom 21 og 32
 */
internal fun erBostNummer(personIdent: String): Boolean {
    personIdent.substring(2, 4).toInt().also { måned ->
        return måned in 21..32
    }
}
