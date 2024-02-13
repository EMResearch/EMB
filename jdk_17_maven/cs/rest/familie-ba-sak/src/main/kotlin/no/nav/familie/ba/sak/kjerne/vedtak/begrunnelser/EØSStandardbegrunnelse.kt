package no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser

import com.fasterxml.jackson.annotation.JsonValue
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.NullablePeriode
import no.nav.familie.ba.sak.kjerne.brev.domene.BrevBegrunnelseGrunnlagMedPersoner
import no.nav.familie.ba.sak.kjerne.brev.domene.RestBehandlingsgrunnlagForBrev
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityEØSBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.eøs.EØSBegrunnelseMedTriggere

enum class EØSStandardbegrunnelse : IVedtakBegrunnelse {
    INNVILGET_PRIMÆRLAND_UK_STANDARD {
        override val sanityApiNavn = "innvilgetPrimarlandUKStandard"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_PRIMÆRLAND_BARNET_BOR_I_NORGE {
        override val sanityApiNavn = "innvilgetPrimarlandBarnetBorINorge"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_PRIMÆRLAND_BARNETRYGD_ALLEREDE_UTBETALT {
        override val sanityApiNavn = "innvilgetPrimarlandBarnetrygdAlleredeUtbetalt"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_PRIMÆRLAND_UK_BARNETRYGD_ALLEREDEUTBETALT {
        override val sanityApiNavn = "innvilgetPrimarlandUkBarnetrygdAlleredeUtbetalt"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE {
        override val sanityApiNavn = "innvilgetPrimarlandBeggeForeldreBosattINorge"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_PRIMÆRLAND_UK_OG_UTLAND_STANDARD {
        override val sanityApiNavn = "innvilgetPrimarlandUKOgUtlandStandard"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_PRIMÆRLAND_SÆRKULLSBARN_ANDRE_BARN_OVERTATT_ANSVAR {
        override val sanityApiNavn = "innvilgetPrimarlandSaerkullsbarnAndreBarnOvertattAnsvar"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_PRIMÆRLAND_UK_TO_ARBEIDSLAND_NORGE_UTBETALER {
        override val sanityApiNavn = "innvilgetPrimarlandUkToArbeidslandNorgeUtbetaler"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_PRIMÆRLAND_TO_ARBEIDSLAND_NORGE_UTBETALER {
        override val sanityApiNavn = "innvilgetPrimarlandToArbeidslandNorgeUtbetaler"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_PRIMÆRLAND_STANDARD {
        override val sanityApiNavn = "innvilgetPrimarlandStandard"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_PRIMÆRLAND_UK_ALENEANSVAR {
        override val sanityApiNavn = "innvilgetPrimarlandUKAleneansvar"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_TILLEGGSBEGRUNNELSE_UTBETALING_TIL_ANNEN_FORELDER {
        override val sanityApiNavn = "innvilgetTilleggsbegrunnelseUtbetalingTilAnnenForelder"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_PRIMÆRLAND_BARNET_FLYTTET_TIL_NORGE {
        override val sanityApiNavn = "innvilgetPrimarlandBarnetFlyttetTilNorge"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_JOBBER_I_NORGE {
        override val sanityApiNavn = "innvilgetPrimarlandBeggeForeldreJobberINorge"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_PRIMÆRLAND_TO_ARBEIDSLAND_ANNET_LAND_UTBETALER {
        override val sanityApiNavn = "innvilgetPrimarlandToArbeidslandAnnetLandUtbetaler"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_PRIMÆRLAND_SÆRKULLSBARN_ANDRE_BARN {
        override val sanityApiNavn = "innvilgetPrimarlandSarkullsbarnAndreBarn"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_PRIMÆRLAND_UK_TO_ARBEIDSLAND_ANNET_LAND_UTBETALER {
        override val sanityApiNavn = "innvilgetPrimarlandUkToArbeidslandAnnetLandUtbetaler"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_PRIMÆRLAND_ALENEANSVAR {
        override val sanityApiNavn = "innvilgetPrimarlandAleneansvar"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_SEKUNDÆRLAND_STANDARD {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
        override val sanityApiNavn = "innvilgetSekundaerlandStandard"
    },
    INNVILGET_SEKUNDÆRLAND_ALENEANSVAR {
        override val sanityApiNavn = "innvilgetSekundaerlandAleneansvar"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_TILLEGGSTEKST_NULLUTBETALING {
        override val sanityApiNavn = "innvilgetTilleggstekstNullutbetaling"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_SEKUNDÆRLAND_UK_STANDARD {
        override val sanityApiNavn = "innvilgetSekundaerlandUkStandard"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_SEKUNDÆRLAND_UK_ALENEANSVAR {
        override val sanityApiNavn = "innvilgetSekundaerlandUkAleneansvar"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_SEKUNDÆRLAND_UK_OG_UTLAND_STANDARD {
        override val sanityApiNavn = "innvilgetSekundaerlandUkOgUtlandStandard"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_SEKUNDÆRLAND_TO_ARBEIDSLAND_NORGE_UTBETALER {
        override val sanityApiNavn = "innvilgetSekundaerlandToArbeidslandNorgeUtbetaler"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_SEKUNDÆRLAND_UK_TO_ARBEIDSLAND_NORGE_UTBETALER {
        override val sanityApiNavn = "innvilgetSekundaerlandUkToArbeidslandNorgeUtbetaler"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_TILLEGGSTEKST_SATSENDRING {
        override val sanityApiNavn = "innvilgetTilleggstekstSatsendring"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_TILLEGGSTEKST_VALUTAJUSTERING {
        override val sanityApiNavn = "innvilgetTilleggstekstValutajustering"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_TILLEGGSTEKST_SATSENDRING_OG_VALUTAJUSTERING {
        override val sanityApiNavn = "innvilgetTilleggstekstSatsendringOgValutajustering"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_TILLEGGSTEKST_SEKUNDÆR_DELT_BOSTED_ANNEN_FORELDER_IKKE_SØKT {
        override val sanityApiNavn = "innvilgetTilleggstekstSekundaerDeltBostedAnnenForelderIkkeSoekt"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_PRIMÆRLAND_TILLEGGSTEKST_VEDTAK_FØR_SED {
        override val sanityApiNavn = "innvilgetPrimaerlandTilleggstekstVedtakFoerSed"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_SELVSTENDIG_RETT_PRIMÆRLAND_FÅR_YTELSE_I_UTLANDET {
        override val sanityApiNavn = "innvilgetSelvstendigRettPrimaerlandFaarYtelseIUtlandet"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_SELVSTENDIG_RETT_SEKUNDÆRLAND_FÅR_YTELSE_I_UTLANDET {
        override val sanityApiNavn = "innvilgetSelvstendigRettSekundaerlandFaarYtelseIUtlandet"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_SEKUNDÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE {
        override val sanityApiNavn = "innvilgetSekundaerlandBeggeForeldreBosattINorge"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_TILLEGGSTEKST_PRIMÆR_DELT_BOSTED_ANNEN_FORELDER_IKKE_RETT {
        override val sanityApiNavn = "innvilgetTilleggstekstPrimaerDeltBostedAnnenForelderIkkeRett"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_TILLEGGSTEKST_SEKUNDÆR_FULL_UTBETALING {
        override val sanityApiNavn = "innvilgetTilleggstekstSekundaerFullUtbetaling"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_TILLEGGSTEKST_SEKUNDÆR_AVTALE_DELT_BOSTED {
        override val sanityApiNavn = "innvilgetTilleggstekstSekundaerAvtaleDeltBosted"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_TILLEGGSTEKST_SEKUNDÆR_DELT_BOSTED_ANNEN_FORELDER_IKKE_RETT {
        override val sanityApiNavn = "innvilgetTilleggstekstsekundaerDeltBostedAnnenForelderIkkeRett"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_GYLDIG_KONTONUMMER_REGISTRERT_EØS {
        override val sanityApiNavn = "innvilgetGyldigKontonummerRegistrertEos"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_TILLEGGSTEKST_SEKUNDÆR_IKKE_FÅTT_SVAR_PÅ_SED {
        override val sanityApiNavn = "innvilgetTilleggstekstSekundaerIkkeFaattSvarPaaSed"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_TILLEGGESTEKST_UK_FULL_ETTERBETALING {
        override val sanityApiNavn = "innvilgetTilleggestekstUkFullEtterbetaling"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_PRIMÆRLAND_DEN_ANDRE_FORELDEREN_UTSENDT_ARBEIDSTAKER {
        override val sanityApiNavn = "innvilgetPrimaerlandDenAndreForelderenUtsendtArbeidstaker"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_SELVSTENDIG_RETT_PRIMÆRLAND_STANDARD {
        override val sanityApiNavn = "innvilgetSelvstendigRettPrimaerlandStandard"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_SELVSTENDIG_RETT_PRIMÆRLAND_UK_STANDARD {
        override val sanityApiNavn = "innvilgetSelvstendigRettPrimaerlandUkStandard"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_SELVSTENDIG_RETT_PRIMÆRLAND_UK_OG_STANDARD {
        override val sanityApiNavn = "innvilgetSelvstendigRettPrimaerlandUkOgStandard"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_SELVSTENDIG_RETT_PRIMÆRLAND_UTSENDT_ARBEIDSTAKER {
        override val sanityApiNavn = "innvilgetSelvstendigRettPrimaerlandUtsendtArbeidstaker"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_SELVSTENDIG_RETT_SEKUNDÆRLAND_STANDARD {
        override val sanityApiNavn = "innvilgetSelvstendigRettSekundaerlandStandard"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_SELVSTENDIG_RETT_SEKUNDÆRLAND_UK_STANDARD {
        override val sanityApiNavn = "innvilgetSelvstendigRettSekundaerlandUkStandard"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_SELVSTENDIG_RETT_SEKUNDÆRLAND_UK_OG_UTLAND_STANDARD {
        override val sanityApiNavn = "innvilgetSelvstendigRettSekundaerlandUkOgUtlandStandard"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    OPPHØR_EØS_STANDARD {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_OPPHØR
        override val sanityApiNavn = "opphorEosStandard"
    },
    OPPHØR_EØS_SØKER_BER_OM_OPPHØR {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_OPPHØR
        override val sanityApiNavn = "opphorEosSokerBerOmOpphor"
    },
    OPPHØR_BARN_BOR_IKKE_I_EØS_LAND {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_OPPHØR
        override val sanityApiNavn = "opphorBorIkkeIEtEOSland"
    },
    OPPHØR_IKKE_STATSBORGER_I_EØS_LAND {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_OPPHØR
        override val sanityApiNavn = "opphorIkkeStatsborgerIEosLand"
    },
    OPPHØR_SENTRUM_FOR_LIVSINTERESSE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_OPPHØR
        override val sanityApiNavn = "opphorSentrumForLivsinteresse"
    },
    OPPHØR_IKKE_ANSVAR_FOR_BARN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_OPPHØR
        override val sanityApiNavn = "opphorIkkeAnsvarForBarn"
    },
    OPPHØR_IKKE_OPPHOLDSRETT_SOM_FAMILIEMEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_OPPHØR
        override val sanityApiNavn = "opphorIkkeOppholdsrettSomFamiliemedlem"
    },
    OPPHØR_SEPARASJONSAVTALEN_GJELDER_IKKE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_OPPHØR
        override val sanityApiNavn = "opphorSeparasjonsavtaleGjelderIkke"
    },
    OPPHØR_SØKER_OG_BARN_BOR_IKKE_I_EØS_LAND {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_OPPHØR
        override val sanityApiNavn = "opphorSoekerOgBarnBorIkkeIEosLand"
    },
    OPPHØR_SØKER_BOR_IKKE_I_EØS_LAND {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_OPPHØR
        override val sanityApiNavn = "opphorSoekerBorIkkeIEosLand"
    },
    OPPHØR_ARBEIDER_MER_ENN_25_PROSENT_I_ANNET_EØS_LAND {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_OPPHØR
        override val sanityApiNavn = "opphorArbeiderMerEnn25ProsentIAnnetEosLand"
    },
    OPPHØR_UTSENDT_ARBEIDSTAKER_FRA_EØS_LAND {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_OPPHØR
        override val sanityApiNavn = "opphorUtsendtArbeidstakerFraEosLand"
    },
    OPPHOR_UGYLDIG_KONTONUMMER_EØS {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_OPPHØR
        override val sanityApiNavn = "opphorUgyldigKontonummerEos"
    },
    OPPHOR_ETT_BARN_DØD_EØS {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_OPPHØR
        override val sanityApiNavn = "opphorEttBarnDodEos"
    },
    OPPHOR_FLERE_BARN_DØDE_EØS {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_OPPHØR
        override val sanityApiNavn = "opphorFlereBarnErDodeEos"
    },
    OPPHØR_SELVSTENDIG_RETT_OPPHØR {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_OPPHØR
        override val sanityApiNavn = "opphorSelvstendigRettOpphoer"
    },
    OPPHØR_SELVSTENDIG_RETT_UTSENDT_ARBEIDSTAKER_FRA_ANNET_EØS_LAND {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_OPPHØR
        override val sanityApiNavn = "opphorSelvstendigRettUtsendtArbedstakerFraAnnetEosLand"
    },
    AVSLAG_EØS_IKKE_EØS_BORGER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_AVSLAG
        override val sanityApiNavn = "avslagEosIkkeEosBorger"
    },
    AVSLAG_EØS_IKKE_BOSATT_I_EØS_LAND {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_AVSLAG
        override val sanityApiNavn = "avslagEosIkkeBosattIEosLand"
    },
    AVSLAG_EØS_JOBBER_IKKE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_AVSLAG
        override val sanityApiNavn = "avslagEosJobberIkke"
    },
    AVSLAG_EØS_UTSENDT_ARBEIDSTAKER_FRA_ANNET_EØS_LAND {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_AVSLAG
        override val sanityApiNavn = "avslagEosUtsendtArbeidstakerFraAnnetEosLand"
    },
    AVSLAG_EØS_ARBEIDER_MER_ENN_25_PROSENT_I_ANNET_EØS_LAND {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_AVSLAG
        override val sanityApiNavn = "avslagEosArbeiderMerEnn25ProsentIAnnetEosLand"
    },
    AVSLAG_EØS_KUN_KORTE_USAMMENHENGENDE_ARBEIDSPERIODER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_AVSLAG
        override val sanityApiNavn = "avslagEosKunKorteUsammenhengendeArbeidsperioder"
    },
    AVSLAG_EØS_IKKE_PENGER_FRA_NAV_SOM_ERSTATTER_LØNN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_AVSLAG
        override val sanityApiNavn = "avslagEosIkkePengerFraNavSomErstatterLoenn"
    },
    AVSLAG_EØS_SEPARASJONSAVTALEN_GJELDER_IKKE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_AVSLAG
        override val sanityApiNavn = "avslagEosSeparasjonsavtalenGjelderIkke"
    },
    AVSLAG_EØS_IKKE_LOVLIG_OPPHOLD_SOM_EØS_BORGER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_AVSLAG
        override val sanityApiNavn = "avslagEosIkkeLovligOppholdSomEosBorger"
    },
    AVSLAG_EØS_IKKE_OPPHOLDSRETT_SOM_FAMILIEMEDLEM_AV_EØS_BORGER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_AVSLAG
        override val sanityApiNavn = "avslagEosIkkeOppholdsrettSomFamiliemedlemAvEosBorger"
    },
    AVSLAG_EØS_IKKE_STUDENT {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_AVSLAG
        override val sanityApiNavn = "avslagEosIkkeStudent"
    },
    AVSLAG_EØS_IKKE_ANSVAR_FOR_BARN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_AVSLAG
        override val sanityApiNavn = "avslagEosIkkeAnsvarForBarn"
    },
    AVSLAG_EØS_VURDERING_IKKE_ANSVAR_FOR_BARN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_AVSLAG
        override val sanityApiNavn = "avslagEosVurderingIkkeAnsvarForBarn"
    },
    AVSLAG_FAAR_DAGPENGER_FRA_ANNET_EOS_LAND {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_AVSLAG
        override val sanityApiNavn = "avslagFaarDagpengerFraAnnetEosLand"
    },
    AVSLAG_SELVSTENDIG_NAERINGSDRIVENDE_NORGE_ARBEIDSTAKER_I_ANNET_EOS_LAND {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_AVSLAG
        override val sanityApiNavn = "avslagSelvstendigNaeringsdrivendeNorgeArbeidstakerIAnnetEosLand"
    },
    AVSLAG_EØS_UREGISTRERT_BARN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_AVSLAG
        override val sanityApiNavn = "avslagEosUregistrertBarn"
    },
    AVSLAG_SELVSTENDIG_RETT_STANDARD_AVSLAG {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_AVSLAG
        override val sanityApiNavn = "avslagSelvstendigRettStandardAvslag"
    },
    AVSLAG_SELVSTENDIG_RETT_UTSENDT_ARBEIDSTAKER_FRA_ANNET_EØS_LAND {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_AVSLAG
        override val sanityApiNavn = "avslagSelvstendigRettUtsendtArbeidstakerFraAnnetEosLand"
    },
    AVSLAG_SELVSTENDIG_RETT_BOR_IKKE_FAST_MED_BARNET {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_AVSLAG
        override val sanityApiNavn = "avslagSelvstendigRettBorIkkeFastMedBarnet"
    },
    FORTSATT_INNVILGET_PRIMÆRLAND_STANDARD {
        override val sanityApiNavn = "fortsattInnvilgetPrimaerlandStandard"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },
    FORTSATT_INNVILGET_PRIMÆRLAND_ALENEANSVAR {
        override val sanityApiNavn = "fortsattInnvilgetPrimaerlandAleneansvar"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },
    FORTSATT_INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE {
        override val sanityApiNavn = "fortsattInnvilgetPrimaerlandBeggeForeldreBosattINorge"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },
    FORTSATT_INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_JOBBER_I_NORGE {
        override val sanityApiNavn = "fortsattInnvilgetPrimaerlandBeggeForeldreJobberINorge"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },
    FORTSATT_INNVILGET_PRIMÆRLAND_UK_STANDARD {
        override val sanityApiNavn = "fortsattInnvilgetPrimaerlandUkStandard"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },
    FORTSATT_INNVILGET_PRIMÆRLAND_UK_ALENEANSVAR {
        override val sanityApiNavn = "fortsattInnvilgetPrimaerlandUkAleneansvar"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },
    FORTSATT_INNVILGET_PRIMÆRLAND_UK_OG_UTLAND_STANDARD {
        override val sanityApiNavn = "fortsattInnvilgetPrimaerlandUkOgUtlandStandard"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },
    FORTSATT_INNVILGET_PRIMÆRLAND_BARNET_BOR_I_NORGE {
        override val sanityApiNavn = "fortsattInnvilgetPrimaerlandBarnetBorINorge"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },
    FORTSATT_INNVILGET_PRIMÆRLAND_SÆRKULLSBARN_ANDRE_BARN_OVERTATT_ANSVAR {
        override val sanityApiNavn = "fortsattInnvilgetPrimaerlandSaerkullsbarnAndreBarn"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },
    FORTSATT_INNVILGET_PRIMÆRLAND_TO_ARBEIDSLAND_NORGE_UTBETALER {
        override val sanityApiNavn = "fortsattInnvilgetPrimaerlandToArbeidslandNorgeUtbetaler"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },
    FORTSATT_INNVILGET_PRIMÆRLAND_TO_ARBEIDSLAND_ANNET_LAND_UTBETALER {
        override val sanityApiNavn = "fortsattInnvilgetPrimaerlandToArbeidslandAnnetLandUtbetaler"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },

    FORTSATT_INNVILGET_PRIMÆRLAND_UK_TO_ARBEIDSLAND_NORGE_UTBETALER {
        override val sanityApiNavn = "fortsattInnvilgetPrimaerlandUkToArbeidslandNorgeUtbetaler"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },
    FORTSATT_INNVILGET_PRIMÆRLAND_UK_TO_ARBEIDSLAND_ANNET_LAND_UTBETALER {
        override val sanityApiNavn = "fortsattInnvilgetPrimaerlandUkToArbeidslandAnnetLandUtbetaler"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },

    FORTSATT_INNVILGET_SELVSTENDIG_RETT_PRIMÆRLAND_FÅR_YTELSE_I_UTLANDET {
        override val sanityApiNavn = "fortsattInnvilgetSelvstendigRettPrimaerlandFaarYtelseIUtlandet"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },

    FORTSATT_INNVILGET_TILLEGGSBEGRUNNELSE_UTBETALING_TIL_ANNEN_FORELDER {
        override val sanityApiNavn = "fortsattInnvilgetTilleggsbegrunnelseUtbetalingTilAnnenForelder"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },
    FORTSETT_INNVILGET_PRIMÆRLAND_TILLEGGSTEKST_VEDTAK_FØR_SED {
        override val sanityApiNavn = "fortsattInnvilgetTilleggsbegrunnelseVedtakForSed"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },
    FORTSETT_INNVILGET_SEKUNDÆRLAND_STANDARD {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetSekundaerlandStandard"
    },
    FORTSETT_INNVILGET_TILLEGGSTEKST_NULLUTBETALING {
        override val sanityApiNavn = "fortsattInnvilgetTilleggstekstNullutbetaling"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },
    FORTSETT_INNVILGET_SEKUNDÆRLAND_ALENEANSVAR {
        override val sanityApiNavn = "fortsattInnvilgetSekundaerlandAleneansvar"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },
    FORTSETT_INNVILGET_SEKUNDÆRLAND_UK_STANDARD {
        override val sanityApiNavn = "fortsattInnvilgetSekundaerlandUkStandard"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },

    FORTSATT_INNVILGET_SELVSTENDIG_RETT_SEKUNDÆRLAND_FÅR_YTELSE_I_UTLANDET {
        override val sanityApiNavn = "fortsattInnvilgetSelvstendigRettSekundaerlandFaarYtelseIUtlandet"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },

    FORTSETT_INNVILGET_SEKUNDÆRLAND_UK_ALENEANSVAR {
        override val sanityApiNavn = "fortsattInnvilgetSekundaerlandUkAleneansvar"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },
    FORTSETT_INNVILGET_SEKUNDÆRLAND_UK_OG_UTLAND_STANDARD {
        override val sanityApiNavn = "fortsattInnvilgetSekundaerlandUkOgUtland"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },
    FORTSETT_INNVILGET_SEKUNDÆRLAND_TO_ARBEIDSLAND_NORGE_UTBETALER {
        override val sanityApiNavn = "fortsattInnvilgetSekundaerlandToArbeidslandNorgeUtbetaler"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },
    FORTSETT_INNVILGET_SEKUNDÆRLAND_UK_TO_ARBEIDSLAND_NORGE_UTBETALER {
        override val sanityApiNavn = "fortsattInnvilgetSekundaerlandUkToArbeidslandNorgeUtbetaler"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },
    FORTSATT_INNVILGET_SEKUNDÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE {
        override val sanityApiNavn = "fortsattInnvilgetSekundaerlandBeggeForeldreBosattINorge"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },
    FORTSATT_INNVILGET_TILLEGGSTEKST_SEKUNDÆR_FULL_UTBETALING {
        override val sanityApiNavn = "fortsattInnvilgetTilleggstekstSekundaerFullUtbetaling"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },
    FORTSATT_INNVILGET_TILLEGGSTEKST_SEKUNDÆR_IKKE_FÅTT_SVAR_PÅ_SED {
        override val sanityApiNavn = "fortsattInnvilgetTilleggsteksterSekundaerIkkeFaattSvarPaaSed"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },
    FORTSATT_INNVILGET_TILLEGSTEKST_UK_FULL_UTBETALING {
        override val sanityApiNavn = "fortsattInnvilgetTilleggstekstUkFullUtbetaling"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },
    FORTSATT_INNVILGET_SELVSTENDIG_RETT_PRIMÆRLAND_STANDARD {
        override val sanityApiNavn = "fortsattInnvilgetSelvstendigRettPrimaerlandStandard"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },
    FORTSATT_INNVILGET_SELVSTENDIG_RETT_PRIMÆRLAND_UK_STANDARD {
        override val sanityApiNavn = "fortsattInnvilgetSelvstendigRettPrimaerlandUkStandard"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },
    FORTSATT_INNVILGET_SELVSTENDIG_RETT_PRIMÆRLAND_UK_OG_UTLAND_STANDARD {
        override val sanityApiNavn = "fortsattInnvilgetSelvstendigRettPrimaerlandUkOgUtlandStandard"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },
    FORTSATT_INNVILGET_SELVSTENDIG_RETT_SEKUNDÆRLAND_STANDARD {
        override val sanityApiNavn = "fortsattInnvilgetSelvstendigRettSekundaerlandStandard"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },
    FORTSATT_INNVILGET_SELVSTENDIG_RETT_SEKUNDÆRLAND_UK_STANDARD {
        override val sanityApiNavn = "fortsattInnvilgetSelvstendigRettSekundaerlandUkStandard"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },
    FORTSATT_INNVILGET_SELVSTENDIG_RETT_SEKUNDAERLAND_UK_OG_UTLAND_STANDARD {
        override val sanityApiNavn = "fortsattInnvilgetSelvstendigRettSekundaerlandUkOgUtlandStandard"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },
    REDUKSJON_BARN_DØD_EØS {
        override val sanityApiNavn = "reduksjonBarnDoedEos"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_REDUKSJON
    },
    REDUKSJON_SØKER_BER_OM_OPPHØR_EØS {
        override val sanityApiNavn = "reduksjonSokerBerOmOpphoer"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_REDUKSJON
    },
    REDUKSJON_BARN_BOR_IKKE_I_EØS {
        override val sanityApiNavn = "reduksjonBarnBorIkkeIEosLand"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_REDUKSJON
    },
    REDUKSJON_IKKE_ANSVAR_FOR_BARN {
        override val sanityApiNavn = "reduksjonIkkeAnsvarForBarn"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_REDUKSJON
    }, ;

    override val kanDelesOpp: Boolean = false

    override fun delOpp(
        restBehandlingsgrunnlagForBrev: RestBehandlingsgrunnlagForBrev,
        triggesAv: TriggesAv,
        periode: NullablePeriode,
    ): List<BrevBegrunnelseGrunnlagMedPersoner> {
        throw Feil("Begrunnelse $this kan ikke deles opp.")
    }

    @JsonValue
    override fun enumnavnTilString(): String = EØSStandardbegrunnelse::class.simpleName + "$" + this.name

    fun tilEØSBegrunnelseMedTriggere(sanityEØSBegrunnelser: Map<EØSStandardbegrunnelse, SanityEØSBegrunnelse>): EØSBegrunnelseMedTriggere? {
        val sanityEØSBegrunnelse = sanityEØSBegrunnelser[this] ?: return null
        return EØSBegrunnelseMedTriggere(
            eøsBegrunnelse = this,
            sanityEØSBegrunnelse = sanityEØSBegrunnelse,
        )
    }

    companion object {
        fun eøsPraksisendringBegrunnelser(): Set<EØSStandardbegrunnelse> = setOf(
            EØSStandardbegrunnelse.INNVILGET_SELVSTENDIG_RETT_PRIMÆRLAND_STANDARD,
            EØSStandardbegrunnelse.INNVILGET_SELVSTENDIG_RETT_PRIMÆRLAND_UK_STANDARD,
            EØSStandardbegrunnelse.INNVILGET_SELVSTENDIG_RETT_PRIMÆRLAND_UK_OG_STANDARD,
            EØSStandardbegrunnelse.INNVILGET_SELVSTENDIG_RETT_PRIMÆRLAND_UTSENDT_ARBEIDSTAKER,
            EØSStandardbegrunnelse.INNVILGET_SELVSTENDIG_RETT_PRIMÆRLAND_FÅR_YTELSE_I_UTLANDET,
            EØSStandardbegrunnelse.INNVILGET_SELVSTENDIG_RETT_SEKUNDÆRLAND_FÅR_YTELSE_I_UTLANDET,
            EØSStandardbegrunnelse.INNVILGET_SELVSTENDIG_RETT_SEKUNDÆRLAND_STANDARD,
            EØSStandardbegrunnelse.INNVILGET_SELVSTENDIG_RETT_SEKUNDÆRLAND_UK_STANDARD,
            EØSStandardbegrunnelse.INNVILGET_SELVSTENDIG_RETT_SEKUNDÆRLAND_UK_OG_UTLAND_STANDARD,
            EØSStandardbegrunnelse.OPPHØR_SELVSTENDIG_RETT_OPPHØR,
            EØSStandardbegrunnelse.OPPHØR_SELVSTENDIG_RETT_UTSENDT_ARBEIDSTAKER_FRA_ANNET_EØS_LAND,
            EØSStandardbegrunnelse.AVSLAG_SELVSTENDIG_RETT_STANDARD_AVSLAG,
            EØSStandardbegrunnelse.AVSLAG_SELVSTENDIG_RETT_UTSENDT_ARBEIDSTAKER_FRA_ANNET_EØS_LAND,
            EØSStandardbegrunnelse.AVSLAG_SELVSTENDIG_RETT_BOR_IKKE_FAST_MED_BARNET,
            EØSStandardbegrunnelse.FORTSATT_INNVILGET_SELVSTENDIG_RETT_PRIMÆRLAND_STANDARD,
            EØSStandardbegrunnelse.FORTSATT_INNVILGET_SELVSTENDIG_RETT_PRIMÆRLAND_UK_STANDARD,
            EØSStandardbegrunnelse.FORTSATT_INNVILGET_SELVSTENDIG_RETT_PRIMÆRLAND_UK_OG_UTLAND_STANDARD,
            EØSStandardbegrunnelse.FORTSATT_INNVILGET_SELVSTENDIG_RETT_SEKUNDÆRLAND_STANDARD,
            EØSStandardbegrunnelse.FORTSATT_INNVILGET_SELVSTENDIG_RETT_SEKUNDÆRLAND_UK_STANDARD,
            EØSStandardbegrunnelse.FORTSATT_INNVILGET_SELVSTENDIG_RETT_SEKUNDAERLAND_UK_OG_UTLAND_STANDARD,
            EØSStandardbegrunnelse.FORTSATT_INNVILGET_SELVSTENDIG_RETT_PRIMÆRLAND_FÅR_YTELSE_I_UTLANDET,
            EØSStandardbegrunnelse.FORTSATT_INNVILGET_SELVSTENDIG_RETT_SEKUNDÆRLAND_FÅR_YTELSE_I_UTLANDET,
        )
    }
}
