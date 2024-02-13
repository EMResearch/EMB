package no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser

import com.fasterxml.jackson.annotation.JsonValue
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.NullablePeriode
import no.nav.familie.ba.sak.kjerne.brev.domene.BrevBegrunnelseGrunnlagMedPersoner
import no.nav.familie.ba.sak.kjerne.brev.domene.RestBehandlingsgrunnlagForBrev
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vedtak.domene.MinimertRestPerson
import no.nav.familie.ba.sak.kjerne.vedtak.domene.hentRelevanteEndringsperioderForBegrunnelse

val hjemlerTilhørendeFritekst = setOf(2, 4, 11)

enum class Standardbegrunnelse : IVedtakBegrunnelse {
    INNVILGET_BOSATT_I_RIKTET {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetBosattIRiket"
    },
    INNVILGET_BOSATT_I_RIKTET_LOVLIG_OPPHOLD {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetBosattIRiketLovligOpphold"
    },
    INNVILGET_LOVLIG_OPPHOLD_OPPHOLDSTILLATELSE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetLovligOppholdOppholdstillatelse"
    },
    INNVILGET_LOVLIG_OPPHOLD_EØS_BORGER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetLovligOppholdEOSBorger"
    },
    INNVILGET_LOVLIG_OPPHOLD_EØS_BORGER_SKJØNNSMESSIG_VURDERING {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetLovligOppholdEOSBorgerSkjonnsmessigVurdering"
    },
    INNVILGET_LOVLIG_OPPHOLD_SKJØNNSMESSIG_VURDERING_TREDJELANDSBORGER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetLovligOppholdSkjonnsmessigVurderingTredjelandsborger"
    },
    INNVILGET_LOVLIG_OPPHOLD_SKJØNNSMESSIG_VURDERING_TREDJELANDSBORGER_SØKER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetLovligOppholdSkjonnsmessigVurderingTredjelandsborgerSoker"
    },
    INNVILGET_TREDJELANDSBORGER_LOVLIG_OPPHOLD_FOR_BOSATT_I_NORGE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetTredjelandsborgerLovligOppholdForBosattINorge"
    },
    INNVILGET_OMSORG_FOR_BARN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetOmsorgForBarn"
    },
    INNVILGET_BOR_HOS_SØKER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetBorHosSoker"
    },
    INNVILGET_BOR_HOS_SØKER_SKJØNNSMESSIG {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetBorHosSokerSkjonnsmessig"
    },
    INNVILGET_FAST_OMSORG_FOR_BARN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetFastOmsorgForBarn"
    },
    INNVILGET_NYFØDT_BARN_FØRSTE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetNyfodtBarnForste"
    },
    INNVILGET_NYFØDT_BARN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetNyfodtBarn"
    },
    INNVILGET_FØDSELSHENDELSE_NYFØDT_BARN_FØRSTE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetFodselshendelseNyfodtBarnForste"
    },
    INNVILGET_FØDSELSHENDELSE_NYFØDT_BARN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetFodselshendelseNyfodtBarn"
    },
    INNVILGET_MEDLEM_I_FOLKETRYGDEN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetMedlemIFolketrygden"
    },
    INNVILGET_BARN_BOR_SAMMEN_MED_MOTTAKER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetBarnBorSammenMedMottaker"
    },
    INNVILGET_BEREDSKAPSHJEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetBeredskapshjem"
    },
    INNVILGET_HELE_FAMILIEN_TRYGDEAVTALE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetHeleFamilienTrygdeavtale"
    },
    INNVILGET_HELE_FAMILIEN_PLIKTIG_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetHeleFamilienPliktigMedlem"
    },
    INNVILGET_SØKER_OG_BARN_PLIKTIG_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetSokerOgBarnPliktigMedlem"
    },
    INNVILGET_ENIGHET_OM_OPPHØR_AV_AVTALE_OM_DELT_BOSTED {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetEnighetOmAtAvtalenOmDeltBostedErOpphort"
    },
    INNVILGET_VURDERING_HELE_FAMILIEN_FRIVILLIG_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetVurderingHeleFamilienFrivilligMedlem"
    },
    INNVILGET_UENIGHET_OM_OPPHØR_AV_AVTALE_OM_DELT_BOSTED {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetUenighetOmOpphorAvAvtaleOmDeltBosted"
    },
    INNVILGET_HELE_FAMILIEN_FRIVILLIG_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetHeleFamilienFrivilligMedlem"
    },
    INNVILGET_VURDERING_HELE_FAMILIEN_PLIKTIG_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetVurderingHeleFamilienPliktigMedlem"
    },
    INNVILGET_SØKER_OG_BARN_OPPHOLD_I_UTLANDET_IKKE_MER_ENN_3_MÅNEDER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetSokerOgBarnOppholdIUtlandetIkkeMerEnn3Maneder"
    },
    INNVILGET_OPPHOLD_I_UTLANDET_IKKE_MER_ENN_3_MÅNEDER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetOppholdIUtlandetIkkeMerEnnTreMaaneder"
    },
    INNVILGET_SØKER_OG_BARN_FRIVILLIG_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetSokerOgBarnFrivilligMedlem"
    },
    INNVILGET_VURDERING_SØKER_OG_BARN_FRIVILLIG_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetVurderingSokerOgBarnFrivilligMedlem"
    },
    INNVILGET_ETTERBETALING_3_ÅR {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetEtterbetaling3Aar"
    },
    INNVILGET_SØKER_OG_BARN_TRYGDEAVTALE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetSokerOgBarnTrygdeavtale"
    },
    INNVILGET_ALENE_FRA_FØDSEL {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetAleneFraFodsel"
    },
    INNVILGET_VURDERING_SØKER_OG_BARN_PLIKTIG_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetVurderingSokerOgBarnPliktigMedlem"
    },
    INNVILGET_BARN_OPPHOLD_I_UTLANDET_IKKE_MER_ENN_3_MÅNEDER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetBarnOppholdIUtlandetIkkeMerEnn3Maneder"
    },
    INNVILGET_SATSENDRING {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetSatsendring"
    },
    INNVILGET_FLYTTET_ETTER_SEPARASJON {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetFlyttetEtterSeparasjon"
    },
    INNVILGET_SEPARERT {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetSeparert"
    },
    INNVILGET_VARETEKTSFENGSEL_SAMBOER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetVaretektsfengselSamboer"
    },
    INNVILGET_AVTALE_DELT_BOSTED_FÅR_FRA_FLYTTETIDSPUNKT {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetAvtaleDeltBostedFaarFraFlyttetidspunkt"
    },
    INNVILGET_TVUNGENT_PSYKISK_HELSEVERN_GIFT {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetTvungentPsykiskHelsevernGift"
    },
    INNVILGET_TVUNGENT_PSYKISK_HELSEVERN_SAMBOER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetTvungentPsykiskHelsevernSamboer"
    },
    INNVILGET_FENGSEL_GIFT {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetFengselGift"
    },
    INNVILGET_VURDERING_EGEN_HUSHOLDNING {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetVurderingEgenHusholdning"
    },
    INNVILGET_FORSVUNNET_SAMBOER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetForsvunnetSamboer"
    },
    INNVILGET_AVTALE_DELT_BOSTED_FÅR_FRA_AVTALETIDSPUNKT {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetAvtaleDeltBostedFaarFraAvtaletidspunkt"
    },
    INNVILGET_FORVARING_GIFT {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetForvaringGift"
    },
    INNVILGET_MEKLINGSATTEST_OG_VURDERING_EGEN_HUSHOLDNING {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetMeklingsattestOgVurderingEgenHusholdning"
    },
    INNVILGET_FENGSEL_SAMBOER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetFengselSamboer"
    },
    INNVILGET_FLYTTING_ETTER_MEKLINGSATTEST {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetFlyttingEtterMeklingsattest"
    },
    INNVILGET_FORVARING_SAMBOER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetForvaringSamboer"
    },
    INNVILGET_SEPARERT_OG_VURDERING_EGEN_HUSHOLDNING {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetSeparertOgVurderingEgenHusholdning"
    },
    INNVILGET_BARN_16_ÅR {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetBarn16Ar"
    },
    INNVILGET_SAMBOER_DØD {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetSamboerDod"
    },
    INNVILGET_MEKLINGSATTEST {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetMeklingsattest"
    },
    INNVILGET_FLYTTET_ETTER_SKILT {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetFlyttetEtterSkilt"
    },
    INNVILGET_ENSLIG_MINDREÅRIG_FLYKTNING {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetEnsligMindrearigFlyktning"
    },
    INNVILGET_VARETEKTSFENGSEL_GIFT {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetVaretektsfengselGift"
    },
    INNVILGET_SAMBOER_UTEN_FELLES_BARN_OG_VURDERING_EGEN_HUSHOLDNING {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetSamboerUtenFellesBarnOgVurderingEgenHusholdning"
    },
    INNVILGET_FORSVUNNET_EKTEFELLE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetForsvunnetEktefelle"
    },
    INNVILGET_FAKTISK_SEPARASJON {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetFaktiskSeparasjon"
    },
    INNVILGET_SAMBOER_UTEN_FELLES_BARN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetSamboerUtenFellesBarn"
    },
    INNVILGET_VURDERING_AVTALE_DELT_BOSTED_FØLGES {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetVurderingAvtaleDeltBostedFolges"
    },
    INNVILGET_SKILT_OG_VURDERING_EGEN_HUSHOLDNING {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetSkiltOgVurderingEgenHusholdning"
    },
    INNVILGET_BOR_ALENE_MED_BARN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetBorAleneMedBarn"
    },
    INNVILGET_SKILT {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetSkilt"
    },
    INNVILGET_RETTSAVGJØRELSE_DELT_BOSTED {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetRettsavgjorelseDeltBosted"
    },
    INNVILGET_EKTEFELLE_DØD {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetEktefelleDod"
    },
    INNVILGET_SMÅBARNSTILLEGG {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetSmaabarnstillegg"
    },
    INNVILGET_ANNEN_FORELDER_IKKE_SØKT_DELT_BARNETRYGD_ENKELTBARN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetAnnenForelderIkkeSoktDeltBarnetrygdEnkeltbarn"
    },
    INNVILGET_ANNEN_FORELDER_IKKE_SØKT_DELT_BARNETRYGD_ALLE_BARNA {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetAnnenForelderIkkeSoktDeltBarnetrygdAlleBarna"
    },
    INNVILGET_TILLEGGSTEKST_SAMSBOER_12_AV_SISTE_18 {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetTilleggstekstSamboer12AvSiste18"
    },
    INNVILGET_ERKLÆRING_OM_MOTREGNING {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetErklaeringOmMotregning"
    },
    INNVILGET_TILLEGGSTEKST_TRANSPORTERKLÆRING_HELE_ETTERBETALINGEN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetTilleggstekstTransporterklaeringHeleEtterbetalingen"
    },
    INNVILGET_TILLEGGSTEKST_TRANSPORTERKLÆRING_DELER_AV_ETTERBETALINGEN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetTilleggstekstTransporterklaeringDelerAvEtterbetalingen"
    },
    INNVILGET_EØS_BORGER_JOBBER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetEosBorgerJobber"
    },
    INNVILGET_EØS_BORGER_UTBETALING_FRA_NAV {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetEosBorgerUtbetalingFraNAV"
    },
    INNVILGET_EØS_BORGER_EKTEFELLE_JOBBER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetEosBorgerEktefelleJobber"
    },
    INNVILGET_EØS_BORGER_SAMBOER_JOBBER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetEosBorgerSamboerJobber"
    },
    INNVILGET_EØS_BORGER_EKTEFELLE_UTBETALING_FRA_NAV {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetEosBorgerEktefelleUtbetalingFraNav"
    },
    INNVILGET_EØS_BORGER_SAMBOER_UTBETALING_FRA_NAV {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetEosBorgerSamboerUtbetalingFraNav"
    },
    INNVILGET_FAKTISK_SEPARASJON_SEPARERT_ETTERPÅ {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetFaktiskSeparasjonSeparertEtterpaa"
    },
    INNVILGET_BARN_16ÅR_UTVIDET_FRA_FLYTTING {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetBarn16AarUtvidetFraFlytting"
    },
    INNVILGET_TILLEGGSTEKST_OPPHØR_UTVIDET_NYFØDT_BARN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetTilleggstekstOpphorUtvidetNyfoedtBarn"
    },
    INNVILGET_TILLEGGSTEKST_SAMBOER_UNDER_12_MÅNEDER_FØR_GIFT {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetTillleggstekstSamboerUnder12MaanederForGift"
    },
    INNVILGET_TILLEGGSTEKST_SAMBOER_UNDER_12_MÅNEDER_FØR_NYTT_BARN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetTilleggstekstSamboerUnder12MaanederForNyttBarn"
    },
    INNVILGET_TILLEGGSTEKST_SAMBOER_UNDER_12_MÅNEDER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetTilleggstekstSamboerUnder12Maaneder"
    },
    INNVILGET_TILLEGGSTEKST_EØS_BORGER_JOBBER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetTilleggstekstEosBorgerJobber"
    },
    INNVILGET_TILLEGGSTEKST_EØS_BORGER_UTBETALING_NAV {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetTilleggstekstEosBorgerUtbetalingNav"
    },
    INNVILGET_TILLEGGSTEKST_EØS_BORGER_EKTEFELLE_JOBBER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetTilleggstekstEosBorgerEktefelleJobber"
    },
    INNVILGET_TILLEGGSTEKST_EØS_BORGER_SAMBOER_JOBBER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetTilleggstekstEosBorgerSamboerJobber"
    },
    INNVILGET_TILLEGGSTEKST_EØS_BORGER_EKTEFELLE_UTBETALING_NAV {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetTilleggstekstEosBorgerEktefelleUtbetalingNav"
    },
    INNVILGET_TILLEGGSTEKST_EØS_BORGER_SAMBOER_UTBETALING_NAV {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetTilleggstekstEosBorgerSamboerUtbetalingNav"
    },
    INNVILGET_TILLEGGSTEKST_TREDJELANDSBORGER_OPPHOLDSTILLATELSE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetTilleggstekstTredjelandsborgerOppholdstillatelse"
    },
    INNVILGET_TILLEGGSTEKST_TREDJELANDSBORGER_OPPHOLDSTILLATELSE_SØKER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetTilleggstekstTredjelandsborgerOppholdstillatelseSoker"
    },
    INNVILGET_MEDLEM_AV_FOLKETRYGDEN_UTEN_DATO {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetMedlemAvFolketrygdenUtenDato"
    },
    INNVILGET_GYLDIG_KONTONUMMER_REGISTRERT {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetGyldigKontonummerRegistrert"
    },
    INNVILGET_FULL_UTBETALING_AVTALE_DELT_BOSTED_ANNEN_OMSORGSPERSON {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetFullUtbetalingAvtaleDeltBostedAnnenOmsorgsperson"
    },
    INNVILGET_FULL_UTBETALING_ANNEN_FORELDER_ØNSKER_IKKE_DELT_BARNETRYGD {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetFullUtbetalingAnnenForelderOnskerIkkeDeltBarnetrygd"
    },
    INNVILGET_OVERGANG_EØS_TIL_NASJONAL_NORSK_NORDISK_FAMILIE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetOvergangEosTilNasjonalNorskNordiskFamilie"
    },
    INNVILGET_OVERGANG_EØS_TIL_NASJONAL_SEPARASJONSAVTALEN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetOvergangEosTilNasjonalSeparasjonsavtalen"
    },
    INNVILGET_FÅR_ETTERBETALT_UTVIDET_FOR_PRAKTISERT_DELT_BOSTED {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetFaarEtterbetaltUtvidetForPraktisertDeltBosted"
    },
    INNVILGET_DATO_SKRIFTLIG_AVTALE_DELT_BOSTED {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetDatoSkriftligAvtaleDeltBosted"
    },
    INNVILGET_DELT_FRA_SKRIFTLIG_AVTALE_HAR_SØKT_FOR_PRAKTISERT_DELT_BOSTED {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetDeltFraSkriftligAvtaleHarSoktForPraktisertDeltBosted"
    },
    INNVILGET_OPPHOLD_PAA_SVALBARD {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetOppholdPaaSvalbard"
    },
    REDUKSJON_BOSATT_I_RIKTET {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonBosattIRiket"
    },
    REDUKSJON_LOVLIG_OPPHOLD_OPPHOLDSTILLATELSE_BARN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonLovligOppholdOppholdstillatelseBarn"
    },
    REDUKSJON_FLYTTET_BARN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonFlyttetBarn"
    },
    REDUKSJON_BARN_DØD {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonBarnDod"
    },
    REDUKSJON_FAST_OMSORG_FOR_BARN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonFastOmsorgForBarn"
    },
    REDUKSJON_UNDER_18_ÅR {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonUnder18Aar"
    },
    REDUKSJON_UNDER_6_ÅR {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonUnder6Aar"
    },
    REDUKSJON_UNDER_18_ÅR_AUTOVEDTAK {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonAutovedtakBarn18Aar"
    },
    REDUKSJON_UNDER_6_ÅR_AUTOVEDTAK {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonAutovedtakBarn6Aar"
    },
    REDUKSJON_DELT_BOSTED_ENIGHET {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonDeltBostedEnighet"
    },
    REDUKSJON_DELT_BOSTED_UENIGHET {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonDeltBostedUenighet"
    },
    REDUKSJON_ENDRET_MOTTAKER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonEndretMottaker"
    },
    REDUKSJON_ANNEN_FORELDER_IKKE_LENGER_FRIVILLIG_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonAnnenForelderIkkeLengerFrivilligMedlem"
    },
    REDUKSJON_ANNEN_FORELDER_IKKE_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonAnnenForelderIkkeMedlem"
    },
    REDUKSJON_ANNEN_FORELDER_IKKE_LENGER_MEDLEM_TRYGDEAVTALE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonAnnenForelderIkkeLengerMedlemTrygdeavtale"
    },
    REDUKSJON_ANNEN_FORELDER_IKKE_LENGER_PLIKTIG_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonAnnenForelderIkkeLengerPliktigMedlem"
    },
    REDUKSJON_VURDERING_BARN_FLERE_KORTE_OPPHOLD_I_UTLANDET_SISTE_ÅRENE_ {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonVurderingBarnFlereKorteOppholdIUtlandetSisteArene"
    },
    REDUKSJON_VURDERING_BARN_FLERE_KORTE_OPPHOLD_I_UTLANDET_SISTE_TO_ÅR_ {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonVurderingBarnFlereKorteOppholdIUtlandetSisteToAr"
    },
    REDUKSJON_SATSENDRING {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonSatsendring"
    },
    REDUKSJON_NYFØDT_BARN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonNyfodtBarn"
    },
    REDUKSJON_VURDERING_SØKER_GIFTET_SEG {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonVurderingSokerGiftetSeg"
    },
    REDUKSJON_VURDERING_SAMBOER_MER_ENN_12_MÅNEDER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonVurderingSamboerMerEnn12Maaneder"
    },
    REDUKSJON_AVTALE_FAST_BOSTED {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonAvtaleFastBosted"
    },
    REDUKSJON_EKTEFELLE_IKKE_I_FENGSEL {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonEktefelleIkkeIFengsel"
    },
    REDUKSJON_SAMBOER_MER_ENN_12_MÅNEDER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonSamboerMerEnn12Maaneder"
    },
    REDUKSJON_SAMBOER_IKKE_I_TVUNGENT_PSYKISK_HELSEVERN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonSamboerIkkeITvungentPsykiskHelsevern"
    },
    REDUKSJON_SAMBOER_IKKE_EGEN_HUSHOLDNING {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonSamboerIkkeEgenHusholdning"
    },
    REDUKSJON_SAMBOER_IKKE_I_FENGSEL {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonSamboerIkkeIFengsel"
    },
    REDUKSJON_VURDERING_FLYTTET_SAMMEN_MED_EKTEFELLE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonVurderingFlyttetSammenMedEktefelle"
    },
    REDUKSJON_VURDERING_FORELDRENE_BOR_SAMMEN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonVurderingForeldreneBorSammen"
    },
    REDUKSJON_SAMBOER_IKKE_I_FORVARING {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonSamboerIkkeIForvaring"
    },
    REDUKSJON_EKTEFELLE_IKKE_I_TVUNGENT_PSYKISK_HELSEVERN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonEktefelleIkkeITvungentPsykiskHelsevern"
    },
    REDUKSJON_EKTEFELLE_IKKE_I_FORVARING {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonEktefelleIkkeIForvaring"
    },
    REDUKSJON_FORELDRENE_BOR_SAMMEN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonForeldreneBorSammen"
    },
    REDUKSJON_EKTEFELLE_IKKE_LENGER_FORSVUNNET {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonEktefelleIkkeLengerForsvunnet"
    },
    REDUKSJON_RETTSAVGJØRELSE_FAST_BOSTED {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonRettsavgjorelseFastBosted"
    },
    REDUKSJON_FLYTTET_SAMMEN_MED_ANNEN_FORELDER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonFlyttetSammenMedAnnenForelder"
    },
    REDUKSJON_GIFT_IKKE_EGEN_HUSHOLDNING {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonGiftIkkeEgenHusholdning"
    },
    REDUKSJON_FLYTTET_SAMMEN_MED_EKTEFELLE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonFlyttetSammenMedEktefelle"
    },
    REDUKSJON_IKKE_AVTALE_DELT_BOSTED {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonIkkeAvtaleDeltBosted"
    },
    REDUKSJON_SØKER_GIFTER_SEG {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonSokerGifterSeg"
    },
    REDUKSJON_SAMBOER_IKKE_LENGER_FORSVUNNET {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonSamboerIkkeLengerForsvunnet"
    },
    REDUKSJON_VURDERING_FLYTTET_SAMMEN_MED_ANNEN_FORELDER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonVurderingFlyttetSammenMedAnnenForelder"
    },
    REDUKSJON_SMÅBARNSTILLEGG_IKKE_LENGER_BARN_UNDER_TRE_ÅR {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonSmaabarnstilleggIkkeLengerBarnUnderTreAar"
    },
    REDUKSJON_SMÅBARNSTILLEGG_IKKE_LENGER_FULL_OVERGANGSSTØNAD {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonSmaabarnstilleggIkkeLengerFullOvergangsstonad"
    },
    REDUKSJON_DELT_BARNETRYGD_ANNEN_FORELDER_SØKT {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonDeltBarnetrygdAnnenForelderSokt"
    },
    REDUKSJON_DELT_BARNETRYGD_HASTEVEDTAK {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonDeltBarnetrygdHastevedtak"
    },
    REDUKSJON_IKKE_BOSATT_I_NORGE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonIkkeBosattINorge"
    },
    REDUKSJON_BARN_BOR_IKKE_MED_SØKER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonBarnBoddeIkkeMedSoker"
    },
    REDUKSJON_IKKE_OPPHOLDSTILLATELSE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonIkkeOppholdstillatelse"
    },
    REDUKSJON_AVTALE_DELT_BOSTED_IKKE_GYLDIG {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonAvtaleOmDeltBostedIkkeGyldig"
    },
    REDUKSJON_AVTALE_DELT_BOSTED_FØLGES_IKKE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonAvtaleDeltBostedFolgesIkke"
    },
    REDUKSJON_FORELDRENE_BODDE_SAMMEN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonForeldreneBoddeSammen"
    },
    REDUKSJON_VURDERING_FORELDRENE_BODDE_SAMMEN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonVurderingForeldreneBoddeSammen"
    },
    REDUKSJON_VAR_IKKE_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonVarIkkeMedlem"
    },
    REDUKSJON_VURDERING_VAR_IKKE_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonVurderingVarIkkeMedlem"
    },
    REDUKSJON_ANDRE_FORELDER_VAR_IKKE_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonDenAndreForelderenVarIkkeMedlem"
    },
    REDUKSJON_VURDERING_ANDRE_FORELDER_VAR_IKKE_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonVurderingDenAndreForelderenVarIkkeMedlem"
    },
    REDUKSJON_DELT_BOSTED_GENERELL {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonDeltBostedGenerell"
    },
    REDUKSJON_BARN_DØDE_SAMME_MÅNED_SOM_FØDT {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonBarnDodeSammeMaanedSomFoedt"
    },
    REDUKSJON_MANGLER_MEKLINGSATTEST {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonManglerMeklingsattest"
    },
    REDUKSJON_FORELDRENE_BOR_SAMMEN_ANNEN_FORELDER_SØKT {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonForeldreneBorSammenAnnenForelderSokt"
    },
    REDUKSJON_SØKER_BER_OM_OPPHØR {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonSokerBerOmOpphor"
    },
    SMÅBARNSTILLEGG_HADDE_IKKE_FULL_OVERGANGSSTØNAD {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonSmaabarnstilleggHaddeIkkeFullOvergangsstonad"
    },
    REDUKSJON_BARN_MED_SAMBOER_FØR_BODD_SAMMEN_12_MND {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonBarnMedSamboerForBoddSammen12Mnd"
    },
    REDUKSJON_SMÅBARNSTILLEGG_HAR_IKKE_UTVIDET_BARNETRYGD {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonSmaabarnstilleggHarIkkeUtvidetBarnetrygd"
    },
    REDUKSJON_SØKER_ER_GIFT {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonSoekerErGift"
    },
    REDUKSJON_SØKER_BER_OM_OPPHØR_UTVIDET {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonSoekerBerOmOpphoerUtvidet"
    },
    REDUKSJON_DELT_BOSTED_SØKER_BER_OM_OPPHØR {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonDeltBostedSoekerBerOmOpphoer"
    },
    REDUKSJON_FAST_BOSTED_AVTALE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonFastBostedAvtale"
    },
    REDUKSJON_BEGGE_FORELDRE_FÅTT_BARNETRYGD {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonBeggeForeldreFaattBarnetrygd"
    },
    REDUKSJON_BARN_BOR_I_INSTITUSJON {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonBarnBorIInstitusjon"
    },
    AVSLAG_BOSATT_I_RIKET {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagBosattIRiket"
    },
    AVSLAG_LOVLIG_OPPHOLD_TREDJELANDSBORGER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagLovligOppholdTredjelandsborger"
    },
    AVSLAG_BOR_HOS_SØKER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagBorHosSoker"
    },
    AVSLAG_OMSORG_FOR_BARN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagOmsorgForBarn"
    },
    AVSLAG_LOVLIG_OPPHOLD_EØS_BORGER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagLovligOppholdEosBorger"
    },
    AVSLAG_LOVLIG_OPPHOLD_SKJØNNSMESSIG_VURDERING_TREDJELANDSBORGER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagLovligOppholdSkjonnsmessigVurderingTredjelandsborger"
    },
    AVSLAG_MEDLEM_I_FOLKETRYGDEN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagMedlemIFolketrygden"
    },
    AVSLAG_FORELDRENE_BOR_SAMMEN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagForeldreneBorSammen"
    },
    AVSLAG_UNDER_18_ÅR {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagUnder18Aar"
    },
    AVSLAG_UGYLDIG_AVTALE_OM_DELT_BOSTED {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagUgyldigAvtaleOmDeltBosted"
    },
    AVSLAG_IKKE_AVTALE_OM_DELT_BOSTED {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagIkkeAvtaleOmDeltBosted"
    },
    AVSLAG_SÆRKULLSBARN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagSaerkullsbarn"
    },
    AVSLAG_UREGISTRERT_BARN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagUregistrertBarn"
    },
    AVSLAG_IKKE_DOKUMENTERT_BOSATT_I_NORGE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagIkkeDokumentertBosattINorge"
    },
    AVSLAG_IKKE_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagIkkeMedlem"
    },
    AVSLAG_VURDERING_FLERE_KORTE_OPPHOLD_I_UTLANDET_SISTE_ÅRENE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagVurderingFlereKorteOppholdIUtlandetSisteArene"
    },
    AVSLAG_VURDERING_ANNEN_FORELDER_IKKE_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagVurderingAnnenForelderIkkeMedlem"
    },
    AVSLAG_IKKE_FRIVILLIG_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagIkkeFrivilligMedlem"
    },
    AVSLAG_IKKE_PLIKTIG_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagIkkePliktigMedlem"
    },
    AVSLAG_ANNEN_FORELDER_IKKE_MEDLEM_ETTER_TRYGDEAVTALE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagAnnenForelderIkkeMedlemEtterTrygdeavtale"
    },
    AVSLAG_ANNEN_FORELDER_IKKE_PLIKTIG_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagAnnenForelderIkkePliktigMedlem"
    },
    AVSLAG_VURDERING_IKKE_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagVurderingIkkeMedlem"
    },
    AVSLAG_ANNEN_FORELDER_IKKE_FRIVILLIG_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagAnnenForelderIkkeFrivilligMedlem"
    },
    AVSLAG_IKKE_MEDLEM_ETTER_TRYGDEAVTALE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagIkkeMedlemEtterTrygdeavtale"
    },
    AVSLAG_VURDERING_FLERE_KORTE_OPPHOLD_I_UTLANDET_SISTE_TO_ÅR {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagVurderingFlereKorteOppholdIUtlandetSisteToAar"
    },
    AVSLAG_SAMBOER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagSamboer"
    },
    AVSLAG_SAMBOER_IKKE_FLYTTET_FRA_HVERANDRE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagSamboerIkkeFlyttetFraHverandre"
    },
    AVSLAG_BARN_HAR_FAST_BOSTED {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagBarnHarFastBosted"
    },
    AVSLAG_IKKE_EGEN_HUSHOLDNING_SAMBOER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagIkkeEgenHusholdningSamboer"
    },
    AVSLAG_GIFT_MIDLERTIDIG_ADSKILLELSE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagGiftMidlertidigAdskillelse"
    },
    AVSLAG_IKKE_EGEN_HUSHOLDNING_GIFT {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagIkkeEgenHusholdningGift"
    },
    AVSLAG_MANGLER_AVTALE_DELT_BOSTED {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagManglerAvtaleDeltBosted"
    },
    AVSLAG_VURDERING_IKKE_FLYTTET_FRA_EKTEFELLE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagVurderingIkkeFlyttetFraEktefelle"
    },
    AVSLAG_RETTSAVGJØRELSE_SAMVÆR {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagRettsavgjorelseSamver"
    },
    AVSLAG_IKKE_SEPARERT {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagIkkeSeparert"
    },
    AVSLAG_FENGSEL_UNDER_6_MÅNEDER_EKTEFELLE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagFengselUnder6MaanederEktefelle"
    },
    AVSLAG_IKKE_DOKUMENTERT_SKILT {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagIkkeDokumentertSkilt"
    },
    AVSLAG_VURDERING_IKKE_MEKLINGSATTEST {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagVurderingIkkeMeklingsattest"
    },
    AVSLAG_FORVARING_UNDER_6_MÅNEDER_EKTEFELLE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagForvaringUnder6MaanederEktefelle"
    },
    AVSLAG_EKTEFELLE_FORSVUNNET_MINDRE_ENN_6_MÅNEDER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagEktefelleForsvunnetMindreEnn6Maaneder"
    },
    AVSLAG_VURDERING_FORELDRENE_BOR_SAMMEN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagVurderingForeldreneBorSammen"
    },
    AVSLAG_SAMBOER_MIDLERTIDIG_ADSKILLELSE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagSamboerMidlertidigAdskillelse"
    },
    AVSLAG_IKKE_FLYTTET_FRA_EKTEFELLE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagIkkeFlyttetFraEktefelle"
    },
    AVSLAG_IKKE_MEKLINGSATTEST {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagIkkeMeklingsattest"
    },
    AVSLAG_FORVARING_UNDER_6_MÅNEDER_SAMBOER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagForvaringUnder6MaanederSamboer"
    },
    AVSLAG_IKKE_DOKUMENTERT_EKTEFELLE_DØD {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagIkkeDokumentertEktefelleDod"
    },
    AVSLAG_VURDERING_IKKE_TVUNGENT_PSYKISK_HELSEVERN_EKTEFELLE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagVurderingIkkeTvungentPsykiskHelsevernEktefelle"
    },
    AVSLAG_VURDERING_IKKE_SEPARERT {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagVurderingIkkeSeparert"
    },
    AVSLAG_GIFT {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagGift"
    },
    AVSLAG_SAMBOER_FORSVUNNET_MINDRE_ENN_6_MÅNEDER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagSamboerForsvunnetMindreEnn6Maaneder"
    },
    AVSLAG_VURDERING_IKKE_TVUNGENT_PSYKISK_HELSEVERN_SAMBOER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagVurderingIkkeTvungentPsykiskHelsevernSamboer"
    },
    AVSLAG_VURDERING_SAMBOER_IKKE_FLYTTET_FRA_HVERANDRE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagVurderingSamboerIkkeFlyttetFraHverandre"
    },
    AVSLAG_ENSLIG_MINDREÅRIG_FLYKTNING {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagEnsligMindreaarigFlyktning"
    },
    AVSLAG_IKKE_DELT_FORELDRENE_BOR_SAMMEN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagIkkeDeltForeldreneBorSammen"
    },
    AVSLAG_IKKE_GYLDIG_AVTALE_DELT_BOSTED {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagIkkeGyldigAvtaleDeltBosted"
    },
    AVSLAG_FENGSEL_UNDER_6_MÅNEDER_SAMBOER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagFengselUnder6MaanederSamboer"
    },
    AVSLAG_IKKE_DOKUMENTERT_SAMBOER_DØD {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagIkkeDokumentertSamboerDod"
    },
    AVSLAG_VURDERING_BOSATT_UNDER_12_MÅNEDER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagVurderingBosattUnder12Maaneder"
    },
    AVSLAG_IKKE_FLYTTET_FRA_TIDLIGERE_EKTEFELLE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagIkkeFlyttetFraTidligereEktefelle"
    },
    AVSLAG_VURDERING_IKKE_FLYTTET_FRA_TIDLIGERE_EKTEFELLE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagVurderingIkkeFlyttetFraTidligereEktefelle"
    },
    AVSLAG_AVTALE_OM_DELT_BOSTED_FØLGES_FORTSATT {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagAvtaleOmDeltBostedFolgesFortsatt"
    },
    AVSLAG_IKKE_OPPHOLDSTILLATELSE_MER_ENN_12_MÅNEDER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagIkkeOppholdstillatelseMerEnn12Maaneder"
    },
    AVSLAG_BOR_IKKE_FAST_MED_BARNET {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagBorIkkeFastMedBarnet"
    },
    AVSLAG_ENSLIG_MINDREÅRIG_FLYKTNING_BOR_I_INSTITUSJON {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagEnsligMindreaarigFlyktningBorIInstitusjon"
    },
    OPPHØR_BARN_FLYTTET_FRA_SØKER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorBarnBorIkkeMedSoker"
    },
    OPPHØR_UTVANDRET {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorFlyttetFraNorge"
    },
    OPPHØR_BARN_DØD {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorEtBarnErDodt"
    },
    OPPHØR_FLERE_BARN_DØD {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorFlereBarnErDode"
    },
    OPPHØR_SØKER_HAR_IKKE_FAST_OMSORG {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorSokerHarIkkeFastOmsorg"
    },
    OPPHØR_HAR_IKKE_OPPHOLDSTILLATELSE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorHarIkkeOppholdstillatelse"
    },
    OPPHØR_DELT_BOSTED_OPPHØRT_ENIGHET {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorDeltBostedOpphortEnighet"
    },
    OPPHØR_DELT_BOSTED_OPPHØRT_UENIGHET {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorDeltBostedOpphortUenighet"
    },
    OPPHØR_UNDER_18_ÅR {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorUnder18Aar"
    },
    OPPHØR_ENDRET_MOTTAKER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorEndretMottaker"
    },
    OPPHØR_ANNEN_FORELDER_IKKE_LENGER_PLIKTIG_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorAnnenForelderIkkeLengerPliktigMedlem"
    },
    OPPHØR_SØKER_OG_BARN_IKKE_LENGER_PLIKTIG_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorSokerOgBarnIkkeLengerPliktigMedlem"
    },
    OPPHØR_BOSATT_I_NORGE_UNNTATT_MEDLEMSKAP {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorBosattINorgeUnntattMedlemskap"
    },
    OPPHØR_ANNEN_FORELDER_IKKE_LENGER_MEDLEM_TRYGDEAVTALE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorAnnenForelderIkkeLengerMedlemTrygdeavtale"
    },
    OPPHØR_SØKER_OG_BARN_IKKE_LENGER_MEDLEM_TRYGDEAVTALE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorSokerOgBarnIkkeLengerMedlemTrygdeavtale"
    },
    OPPHØR_SØKER_OG_BARN_IKKE_LENGER_FRIVILLIG_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorSokerOgBarnIkkeLengerFrivilligMedlem"
    },
    OPPHØR_VURDERING_ANNEN_FORELDER_IKKE_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorVurderingAnnenForelderIkkeMedlem"
    },
    OPPHØR_VURDERING_FLERE_KORTE_OPPHOLD_I_UTLANDET_SISTE_TO_ÅR {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorVurderingFlereKorteOppholdIUtlandetSisteToAr"
    },
    OPPHØR_VURDERING_SØKER_OG_BARN_IKKE_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorVurderingSokerOgBarnIkkeMedlem"
    },
    OPPHØR_SØKER_OG_BARN_IKKE_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorSokerOgBarnIkkeMedlem"
    },
    OPPHØR_VURDERING_FLERE_KORTE_OPPHOLD_I_UTLANDET_SISTE_ÅRENE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorVurderingFlereKorteOppholdIUtlandetSisteArene"
    },
    OPPHØR_ANNEN_FORELDER_IKKE_LENGER_FRIVILLIG_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorAnnenForelderIkkeLengerFrivilligMedlem"
    },
    OPPHØR_FORELDRENE_BOR_SAMMEN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorForeldreneBorSammen"
    },
    OPPHØR_AVTALE_OM_FAST_BOSTED {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorAvtaleOmFastBosted"
    },
    OPPHØR_RETTSAVGJØRELSE_FAST_BOSTED {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorRettsavgjorelseFastBosted"
    },
    OPPHØR_IKKE_AVTALE_OM_DELT_BOSTED {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorIkkeAvtaleOmDeltBosted"
    },
    OPPHØR_VURDERING_FORELDRENE_BOR_SAMMEN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorVurderingForeldreneBorSammen"
    },
    OPPHØR_FORELDRENE_BODD_SAMMEN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorForeldreneBoddSammen"
    },
    OPPHØR_IKKE_OPPHOLDSTILLATELSE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorIkkeOppholdstillatelse"
    },
    OPPHØR_VURDERING_FORELDRENE_BODDE_SAMMEN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorVurderingForeldreneBoddeSammen"
    },
    OPPHØR_IKKE_BOSATT_I_NORGE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorIkkeBosattINorge"
    },
    OPPHØR_BARN_BODDE_IKKE_MED_SØKER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorBarnBoddeIkkeMedSoker"
    },
    OPPHØR_AVTALE_DELT_BOSTED_IKKE_GYLDIG {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorAvtaleDeltBostedIkkeGyldig"
    },
    OPPHØR_VURDERING_VAR_IKKE_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorVurderingVarIkkeMedlem"
    },
    OPPHØR_VAR_IKKE_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorVarIkkeMedlem"
    },
    OPPHØR_VURDERING_DEN_ANDRE_FORELDEREN_VAR_IKKE_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorVurderingDenAndreForelderenVarIkkeMedlem"
    },
    OPPHØR_AVTALE_DELT_BOSTED_FØLGES_IKKE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorAvtaleDeltBostedFolgesIkke"
    },
    OPPHØR_DEN_ANDRE_FORELDEREN_VAR_IKKE_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorDenAndreForelderenVarIkkeMedlem"
    },
    OPPHØR_IKKE_OPPHOLDSRETT_EØS_BORGER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorIkkeOppholdsrettEosBorger"
    },
    OPPHØR_BOSATT_I_NORGE_VAR_IKKE_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorBosattINorgeVarIkkeMedlem"
    },
    OPPHØR_BARN_DØD_SAMME_MÅNED_SOM_FØDT {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorBarnDodSammeMaanedSomFoedt"
    },
    OPPHØR_UGYLDIG_KONTONUMMER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorUgyldigKontonummer"
    },
    OPPHØR_FORELDRENE_BOR_SAMMEN_ENDRE_MOTTAKER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorForeldreneBorSammenEndretMottaker"
    },
    OPPHØR_SØKER_BER_OM_OPPHØR {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorSokerBerOmOpphor"
    },
    OPPHØR_IKKE_OPPHOLDSTILLATELSE_MER_ENN_12_MÅNEDER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorIkkeOppholdstillatelseMerEnn12Maaneder"
    },
    OPPHØR_DELT_BOSTED_SØKER_BER_OM_OPPHØR {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphordeltBostedSoekerBerOmOpphoer"
    },
    OPPHØR_FAST_BOSTED_AVTALE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorFastBostedAvtale"
    },
    OPPHØR_BEGGE_FORELDRE_FÅTT_BARNETRYGD {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorBeggeForeldreFaattBarnetrygd"
    },
    OPPHOR_BARNET_BOR_I_INSTITUSJON {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorBarnetBorIInstitusjon"
    },
    OPPHØR_BARN_BOR_IKKE_MED_SØKER_ETTER_DELT_BOSTED {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorBarnBorIkkeMedSokerEtterDeltBosted"
    },
    OPPHØR_VURDERING_IKKE_BOSATT_I_NORGE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorVurderingIkkeBosattINorge"
    },
    FORTSATT_INNVILGET_SØKER_OG_BARN_BOSATT_I_RIKET {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetSokerOgBarnBosattIRiket"
    },
    FORTSATT_INNVILGET_SØKER_BOSATT_I_RIKET {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetSokerBosattIRiket"
    },
    FORTSATT_INNVILGET_BARN_BOSATT_I_RIKET {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetBarnBosattIRiket"
    },
    FORTSATT_INNVILGET_BARN_OG_SØKER_LOVLIG_OPPHOLD_OPPHOLDSTILLATELSE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetBarnOgSokerLovligOppholdOppholdstillatelse"
    },
    FORTSATT_INNVILGET_SØKER_LOVLIG_OPPHOLD_OPPHOLDSTILLATELSE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetSokerLovligOppholdOppholdstillatelse"
    },
    FORTSATT_INNVILGET_BARN_LOVLIG_OPPHOLD_OPPHOLDSTILLATELSE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetBarnLovligOppholdOppholdstillatelse"
    },
    FORTSATT_INNVILGET_BOR_MED_SØKER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetBorMedSoker"
    },
    FORTSATT_INNVILGET_FAST_OMSORG {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetFastOmsorg"
    },
    FORTSATT_INNVILGET_LOVLIG_OPPHOLD_EØS {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetLovligOppholdEOS"
    },
    FORTSATT_INNVILGET_LOVLIG_OPPHOLD_TREDJELANDSBORGER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetLovligOppholdTredjelandsborger"
    },
    FORTSATT_INNVILGET_UENDRET_TRYGD {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetUendretTrygd"
    },
    FORTSATT_INNVILGET_OPPHOLD_I_UTLANDET_IKKE_MER_ENN_3_MÅNEDER_SØKER_OG_BARN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetOppholdIUtlandetIkkeMerEnn3ManederSokerOgBarn"
    },
    FORTSATT_INNVILGET_HELE_FAMILIEN_MEDLEM_ETTER_TRYGDEAVTALE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetHeleFamilienMedlemEtterTrygdeavtale"
    },
    FORTSATT_INNVILGET_OPPHOLD_I_UTLANDET_IKKE_MER_ENN_3_MÅNEDER_BARN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetOppholdIUtlandetIkkeMerEnn3ManederBarn"
    },
    FORTSATT_INNVILGET_DELT_BOSTED_PRAKTISERES_FORTSATT {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetDeltBostedPraktiseresFortsatt"
    },
    FORTSATT_INNVILGET_VURDERING_HELE_FAMILIEN_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetVurderingHeleFamilienMedlem"
    },
    FORTSATT_INNVILGET_SØKER_OG_BARN_MEDLEM_ETTER_TRYGDEAVTALE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetSokerOgBarnMedlemEtterTrygdeavtale"
    },
    FORTSATT_INNVILGET_ANNEN_FORELDER_IKKE_SØKT_OM_DELT_BARNETRYGD {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetAnnenForelderIkkeSokt"
    },
    FORTSATT_INNVILGET_VURDERING_SØKER_OG_BARN_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetVurderingSokerOgBarnMedlem"
    },
    FORTSATT_INNVILGET_MEDLEM_I_FOLKETRYGDEN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetMedlemIFolketrygden"
    },
    FORTSATT_INNVILGET_TVUNGENT_PSYKISK_HELSEVERN_GIFT {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetTvungentPsykiskHelsevernGift"
    },
    FORTSATT_INNVILGET_FENGSEL_GIFT {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetFengselGift"
    },
    FORTSATT_INNVILGET_VURDERING_BOR_ALENE_MED_BARN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetVurderingBorAleneMedBarn"
    },
    FORTSATT_INNVILGET_SEPARERT {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetSeparert"
    },
    FORTSATT_INNVILGET_FORTSATT_RETTSAVGJØRELSE_OM_DELT_BOSTED {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetFortsattRettsavgjorelseOmDeltBosted"
    },
    FORTSATT_INNVILGET_BOR_ALENE_MED_BARN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetBorAleneMedBarn"
    },
    FORTSATT_INNVILGET_TVUNGENT_PSYKISK_HELSEVERN_SAMBOER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetTvungentPsykiskHelsevernSamboer"
    },
    FORTSATT_INNVILGET_FORVARING_SAMBOER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetForvaringSamboer"
    },
    FORTSATT_INNVILGET_FORTSATT_AVTALE_OM_DELT_BOSTED {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetFortsattAVtaleOmDeltBosted"
    },
    FORTSATT_INNVILGET_VARETEKTSFENGSEL_SAMBOER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetVaretektsfengselSamboer"
    },
    FORTSATT_INNVILGET_FENGSEL_SAMBOER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetFengselSamboer"
    },
    FORTSATT_INNVILGET_FORVARING_GIFT {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetForvaringGift"
    },
    FORTSATT_INNVILGET_VAREKTEKTSFENGSEL_GIFT {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetVaretektsfengselGift"
    },
    FORTSATT_INNVILGET_FORSVUNNET_SAMBOER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetForsvunnetSamboer"
    },
    FORTSATT_INNVILGET_FORSVUNNET_EKTEFELLE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetForsvunnetEktefelle"
    },
    FORTSATT_INNVILGET_BRUKER_ER_BLITT_NORSK_STATSBORGER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetBrukerErBlittNorskStatsborger"
    },
    FORTSATT_INNVILGET_BRUKER_OG_BARN_ER_BLITT_NORSKE_STATSBORGERE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetBrukerOgBarnErBlittNorskeStatsborgere"
    },
    FORTSATT_INNVILGET_ET_BARN_ER_BLITT_NORSK_STATSBORGER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetEtBarnErBlittNorskStatsborger"
    },
    FORTSATT_INNVILGET_FLERE_BARN_ER_BLITT_NORSKE_STATSBORGERE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetFlereBarnErBlittNorskeStatsborgere"
    },
    FORTSATT_INNVILGET_OPPDATERT_KONTO_OPPLYSNINGER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetOppdatertKontoOpplysninger"
    },
    FORTSATT_INNVILGET_ADRESSE_REGISTRERT {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetAdresseRegistrert"
    },
    FORTSATT_INNVILGET_VARIG_OPPHOLDSTILLATELSE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetVarigOppholdstillatelse"
    },
    FORTSATT_INNVILGET_VARIG_OPPHOLDSRETT_EØS_BORGER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetVarigOppholdsrettEosBorger"
    },
    FORTSATT_INNVILGET_GENERELL_BOR_SAMMEN_MED_BARN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetGenerellBorSammenMedBarn"
    },
    ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_INGEN_UTBETALING_NY {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.ENDRET_UTBETALING
        override val sanityApiNavn = "endretUtbetalingDeltBostedIngenUtbetaling"
    },
    ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_FULL_UTBETALING_FØR_SOKNAD_NY {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.ENDRET_UTBETALING
        override val sanityApiNavn = "endretUtbetalingNyDeltBostedFullUtbetalingForSoknad"
    },
    ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_KUN_ETTERBETALT_UTVIDET_NY {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.ENDRET_UTBETALING
        override val sanityApiNavn = "endretUtbetalingDeltBostedFaarKunEtterbetaltUtvidet"
    },
    ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_MOTTATT_FULL_ORDINÆR_ETTERBETALT_UTVIDET_NY {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.ENDRET_UTBETALING
        override val sanityApiNavn = "endretUtbetalingMottattFullOrdinaerFaarEtterbetaltUtvidet"
    },
    ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_ENDRET_UTBETALING {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.ENDRET_UTBETALING
        override val sanityApiNavn = "endretUtbetalingDeltBostedEndretUtbetaling"
        override val kanDelesOpp: Boolean = true
    },
    ENDRET_UTBETALING_SEKUNDÆR_DELT_BOSTED_FULL_UTBETALING_FØR_SØKNAD {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.ENDRET_UTBETALING
        override val sanityApiNavn = "endretUtbetalingSekundaerDeltBostedFullUtbetalingFoerSoeknad"
    },
    ENDRET_UTBETALING_ETTERBETALT_UTVIDET_DEL_FRA_AVTALETIDSPUNKT_SØKT_FOR_PRAKTISERT_DELT {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.ENDRET_UTBETALING
        override val sanityApiNavn = "endretUtbetalingEtterbetaltUtvidetDelFraSkriftligAvtaleSokerPraktisert"
    },
    ENDRET_UTBETALING_ALLEREDE_UTBETALT_FORELDRE_BOR_SAMMEN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.ENDRET_UTBETALING
        override val sanityApiNavn = "endretUtbetalingAlleredeUtbetalt"
    },
    ENDRET_UTBETALING_ETTERBETALING_UTVIDET_EØS {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.ENDRET_UTBETALING
        override val sanityApiNavn = "endretUtbetalingEtterbetalingUtvidetEos"
    },
    ENDRET_UTBETALING_OPPHØR_ENDRE_MOTTAKER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.ENDRET_UTBETALING
        override val sanityApiNavn = "endretUtbetalingOpphorEndreMottaker"
    },
    ENDRET_UTBETALING_REDUKSJON_ENDRE_MOTTAKER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.ENDRET_UTBETALING
        override val sanityApiNavn = "endretUtbetalingReduksjonEndreMottaker"
    },
    ENDRET_UTBETALING_ETTERBETALING_TRE_ÅR_TILBAKE_I_TID {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.ENDRET_UTBETALING
        override val sanityApiNavn = "endretUtbetalingEtterbetalingTreAarTilbakeITid"
    },
    ENDRET_UTBETALING_ETTERBETALING_TRE_ÅR_TILBAKE_I_TID_SED {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.ENDRET_UTBETALING
        override val sanityApiNavn = "endretUtbetalingEtterbetalingTreAarTilbakeITidSED"
    },
    ENDRET_UTBETALING_ETTERBETALING_TRE_ÅR_TILBAKE_I_TID_KUN_UTVIDET_DEL_UTBETALING {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.ENDRET_UTBETALING
        override val sanityApiNavn = "endretUtbetalingEtterbetalingTreAarTilbakeITidKunUtvidetDelUtbetaling"
    },
    ENDRET_UTBETALING_ETTERBETALING_TRE_ÅR_TILBAKE_I_TID_SED_UTBETALING {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.ENDRET_UTBETALING
        override val sanityApiNavn = "endretUtbetalingEtterbetalingTreAarTilbakeITidSedUtbetaling"
    },
    ENDRET_UTBETALING_TRE_ÅR_TILBAKE_I_TID_UTBETALING {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.ENDRET_UTBETALING
        override val sanityApiNavn = "endretUtbetalingTreAarTilbakeITidUtbetaling"
    },
    ETTER_ENDRET_UTBETALING_RETTSAVGJØRELSE_DELT_BOSTED {
        override val sanityApiNavn = "etterEndretUtbetalingRettsavgjorelseDeltBosted"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.ETTER_ENDRET_UTBETALING
    },
    ETTER_ENDRET_UTBETALING_AVTALE_DELT_BOSTED_FØLGES {
        override val sanityApiNavn = "etterEndretUtbetalingVurderingAvtaleDeltBostedFolges"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.ETTER_ENDRET_UTBETALING
    },
    ETTER_ENDRET_UTBETALING_HAR_AVTALE_DELT_BOSTED {
        override val sanityApiNavn = "etterEndretUtbetalingAvtaleDeltBosted"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.ETTER_ENDRET_UTBETALING
    },
    ETTER_ENDRET_UTBETALING_ETTERBETALING {
        override val sanityApiNavn = "etterEndretUtbetalingEtterbetalingTreAarTilbakeITid"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.ETTER_ENDRET_UTBETALING
    },
    ETTER_ENDRET_UTBETALING_ETTERBETALING_UTVIDET {
        override val sanityApiNavn = "etterEndretUtbetalingEtterbetalingTreAarTilbakeITidKunUtvidetDel"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.ETTER_ENDRET_UTBETALING
    },
    ETTER_ENDRET_UTBETALING_ETTERBETALING_SED {
        override val sanityApiNavn = "etterEndretUtbetalingEtterbetalingTreAarTilbakeITidSed"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.ETTER_ENDRET_UTBETALING
    },
    ETTER_ENDRET_UTBETALING_ETTERBETALING_TRE_AAR {
        override val sanityApiNavn = "etterEndretUtbetalingEtterbetalingTreAar"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.ETTER_ENDRET_UTBETALING
    },
    ETTER_ENDRET_UTBETALING_EØS_BARNETRYGD_ALLEREDE_UTBETALT {
        override val sanityApiNavn = "etterEndretUtbetalingEosBarnetrygdAlleredeUtbetalt"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.ETTER_ENDRET_UTBETALING
    },
    ETTER_ENDRET_UTBETALING_ETTERBETALING_TRE_AAR_KUN_UTVIDET_DEL {
        override val sanityApiNavn = "etterEndretUtbetalingEtterbetalingTreAarKunUtvidetDel"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.ETTER_ENDRET_UTBETALING
    },

    // Begrunnelser for institusjon
    INNVILGET_BOR_FAST_I_INSTITUSJON {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INSTITUSJON_INNVILGET
        override val sanityApiNavn = "innvilgetBorFastIInstitusjon"
    },
    INNVILGET_SATSENDRING_INSTITUSJON {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INSTITUSJON_INNVILGET
        override val sanityApiNavn = "innvilgetSatsendringInstitusjon"
    },
    REDUKSJON_BARN_6_ÅR_INSTITUSJON {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INSTITUSJON_REDUKSJON
        override val sanityApiNavn = "reduksjonBarn6AarInstitusjon"
    },
    REDUKSJON_SATSENDRING_INSTITUSJON {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INSTITUSJON_REDUKSJON
        override val sanityApiNavn = "reduksjonSatsendringInstitusjon"
    },
    AVSLAG_IKKE_BOSATT_I_INSTITUSJON {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INSTITUSJON_AVSLAG
        override val sanityApiNavn = "avslagIkkeBosattIInstitusjon"
    },
    AVSLAG_IKKE_OPPHOLDSTILLATELSE_INSTITUSJON {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INSTITUSJON_AVSLAG
        override val sanityApiNavn = "avslagIkkeOppholdstillatelseInstitusjon"
    },
    OPPHØR_FLYTTET_FRA_INSTITUSJON {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INSTITUSJON_OPPHØR
        override val sanityApiNavn = "opphorFlyttetFraInstitusjon"
    },
    OPPHØR_BARN_DØD_INSTITUSJON {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INSTITUSJON_OPPHØR
        override val sanityApiNavn = "opphorBarnDodInstitusjon"
    },
    OPPHØR_BARN_BODDE_IKKE_FAST_I_INSTITUSJON {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INSTITUSJON_OPPHØR
        override val sanityApiNavn = "opphorBarnBoddeIkkeFastIInstitusjon"
    },
    OPPHØR_BARN_HADDE_IKKE_OPPHOLDSTILLATELSE_INSTITUSJON {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INSTITUSJON_OPPHØR
        override val sanityApiNavn = "opphorBarnHaddeIkkeOppholdstillatelseInstitusjon"
    },
    OPPHØR_OPPHOLDSTILLATELSE_UTLØPT_INSTITUSJON {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INSTITUSJON_OPPHØR
        override val sanityApiNavn = "opphorOppholdstillatelseUtloptInstitusjon"
    },
    OPPHØR_BARNET_ER_18_ÅR_INSTITUSJON {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INSTITUSJON_OPPHØR
        override val sanityApiNavn = "opphorBarnetEr18AarInstitusjon"
    },
    FORTSATT_INNVILGET_BOSATT_I_INSTITUSJON {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INSTITUSJON_FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetBosattIInstitusjon"
    },
    FORTSATT_INNVILGET_OPPHOLDSTILLATELSE_INSTITUSJON {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INSTITUSJON_FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetOppholdstillatelseInstitusjon"
    },
    FORTSATT_INNVILGET_VARIG_OPPHOLDSTILLATELSE_INSTITUSJON {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INSTITUSJON_FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetVarigOppholdstillatelseInstitusjon"
    },
    FORTSATT_INNVILGET_NORSK_STATSBORGER_INSTITUSJON {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INSTITUSJON_FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetNorskStatsborgerInstitusjon"
    }, ;

    override val kanDelesOpp: Boolean = false

    @JsonValue
    override fun enumnavnTilString(): String =
        Standardbegrunnelse::class.simpleName + "$" + this.name

    override fun delOpp(
        restBehandlingsgrunnlagForBrev: RestBehandlingsgrunnlagForBrev,
        triggesAv: TriggesAv,
        periode: NullablePeriode,
    ): List<BrevBegrunnelseGrunnlagMedPersoner> {
        if (!this.kanDelesOpp) {
            throw Feil("Begrunnelse $this kan ikke deles opp.")
        }
        return when (this) {
            Standardbegrunnelse.ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_ENDRET_UTBETALING -> {
                val deltBostedEndringsperioder = this.hentRelevanteEndringsperioderForBegrunnelse(
                    minimerteRestEndredeAndeler = restBehandlingsgrunnlagForBrev.minimerteEndredeUtbetalingAndeler,
                    vedtaksperiode = periode,
                )
                    .filter { it.årsak == Årsak.DELT_BOSTED }
                    .filter { endringsperiode ->
                        endringsperiodeGjelderBarn(
                            personerPåBehandling = restBehandlingsgrunnlagForBrev.personerPåBehandling,
                            personIdentFraEndringsperiode = endringsperiode.personIdent,
                        )
                    }
                val deltBostedEndringsperioderGruppertPåAvtaledato =
                    deltBostedEndringsperioder.groupBy { it.avtaletidspunktDeltBosted }

                deltBostedEndringsperioderGruppertPåAvtaledato.map {
                    BrevBegrunnelseGrunnlagMedPersoner(
                        standardbegrunnelse = this,
                        vedtakBegrunnelseType = this.vedtakBegrunnelseType,
                        triggesAv = triggesAv,
                        personIdenter = it.value.map { endringsperiode -> endringsperiode.personIdent },
                        avtaletidspunktDeltBosted = it.key,
                    )
                }
            }

            else -> throw Feil("Oppdeling av begrunnelse $this er ikke støttet.")
        }
    }
}

private fun endringsperiodeGjelderBarn(
    personerPåBehandling: List<MinimertRestPerson>,
    personIdentFraEndringsperiode: String,
) = personerPåBehandling.find { person -> person.personIdent == personIdentFraEndringsperiode }?.type == PersonType.BARN

val endretUtbetalingsperiodeBegrunnelser: Set<Standardbegrunnelse> = setOf(
    Standardbegrunnelse.ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_INGEN_UTBETALING_NY,
    Standardbegrunnelse.ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_FULL_UTBETALING_FØR_SOKNAD_NY,
    Standardbegrunnelse.ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_KUN_ETTERBETALT_UTVIDET_NY,
    Standardbegrunnelse.ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_MOTTATT_FULL_ORDINÆR_ETTERBETALT_UTVIDET_NY,
    Standardbegrunnelse.ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_ENDRET_UTBETALING,
    Standardbegrunnelse.ENDRET_UTBETALING_SEKUNDÆR_DELT_BOSTED_FULL_UTBETALING_FØR_SØKNAD,
    Standardbegrunnelse.ENDRET_UTBETALING_ETTERBETALT_UTVIDET_DEL_FRA_AVTALETIDSPUNKT_SØKT_FOR_PRAKTISERT_DELT,
    Standardbegrunnelse.ENDRET_UTBETALING_ALLEREDE_UTBETALT_FORELDRE_BOR_SAMMEN,
    Standardbegrunnelse.ENDRET_UTBETALING_ETTERBETALING_UTVIDET_EØS,
)
