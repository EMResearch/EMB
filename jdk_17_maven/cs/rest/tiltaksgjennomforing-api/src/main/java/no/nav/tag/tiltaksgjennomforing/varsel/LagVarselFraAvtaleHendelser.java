package no.nav.tag.tiltaksgjennomforing.varsel;

import java.util.List;
import lombok.RequiredArgsConstructor;
import no.nav.tag.tiltaksgjennomforing.avtale.Avtalerolle;
import no.nav.tag.tiltaksgjennomforing.avtale.HendelseType;
import no.nav.tag.tiltaksgjennomforing.avtale.events.*;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LagVarselFraAvtaleHendelser {
    private final VarselRepository varselRepository;

    @EventListener
    public void avtaleOpprettet(AvtaleOpprettetAvVeileder event) {
        VarselFactory factory = new VarselFactory(event.getAvtale(), Avtalerolle.VEILEDER, event.getUtfortAv(), HendelseType.OPPRETTET);
        varselRepository.saveAll(factory.alleParter());
    }

    @EventListener
    public void avtaleOpprettetAvArbeidsgiver(AvtaleOpprettetAvArbeidsgiver event) {
        VarselFactory factory = new VarselFactory(event.getAvtale(), Avtalerolle.ARBEIDSGIVER, event.getAvtale().getBedriftNr(), HendelseType.OPPRETTET_AV_ARBEIDSGIVER);
        varselRepository.saveAll(factory.alleParter());
    }

    @EventListener
    public void avtaleDeltMedAvtalepart(AvtaleDeltMedAvtalepart event) {
        if (event.getAvtalepart() == Avtalerolle.ARBEIDSGIVER) {
            VarselFactory factory = new VarselFactory(event.getAvtale(), Avtalerolle.VEILEDER, null, HendelseType.DELT_MED_ARBEIDSGIVER);
            varselRepository.saveAll(List.of(factory.veileder(), factory.arbeidsgiver()));
        } else if (event.getAvtalepart() == Avtalerolle.DELTAKER) {
            VarselFactory factory = new VarselFactory(event.getAvtale(), Avtalerolle.VEILEDER, null, HendelseType.DELT_MED_DELTAKER);
            varselRepository.saveAll(List.of(factory.veileder(), factory.deltaker()));
        } else if (event.getAvtalepart() == Avtalerolle.MENTOR) {
            VarselFactory factory = new VarselFactory(event.getAvtale(), Avtalerolle.VEILEDER, null, HendelseType.DELT_MED_MENTOR);
            varselRepository.saveAll(List.of(factory.veileder(), factory.mentor()));
        }
    }

    //TODO: Hent IDENTEN til beslutter her og ikke veileder
    @EventListener
    public void tilskuddsperiodeAvslått(TilskuddsperiodeAvslått event) {
        VarselFactory factory = new VarselFactory(event.getAvtale(), Avtalerolle.BESLUTTER, event.getUtfortAv(), HendelseType.TILSKUDDSPERIODE_AVSLATT);
        varselRepository.saveAll(List.of(factory.veileder()));
    }

    //TODO: Hent IDENTEN til beslutter her og ikke veileder
    @EventListener
    public void tilskuddsperiodeGodkjent(TilskuddsperiodeGodkjent event) {
        VarselFactory factory = new VarselFactory(event.getAvtale(), Avtalerolle.BESLUTTER, event.getUtfortAv(), HendelseType.TILSKUDDSPERIODE_GODKJENT);
        varselRepository.saveAll(List.of(factory.veileder()));
    }

    @EventListener
    public void avtaleEndret(AvtaleEndret event) {
        VarselFactory factory = new VarselFactory(event.getAvtale(), event.getUtfortAvRolle(), event.getUtfortAv(), HendelseType.ENDRET);
        varselRepository.saveAll(factory.alleParter());
    }

    @EventListener
    public void godkjenningerOpphevetAvArbeidsgiver(GodkjenningerOpphevetAvArbeidsgiver event) {
        VarselFactory factory = new VarselFactory(event.getAvtale(), Avtalerolle.ARBEIDSGIVER, event.getAvtale().getBedriftNr(), HendelseType.GODKJENNINGER_OPPHEVET_AV_ARBEIDSGIVER);
        varselRepository.saveAll(factory.alleParter());
    }

    @EventListener
    public void godkjenningerOpphevetAvVeileder(GodkjenningerOpphevetAvVeileder event) {
        VarselFactory factory = new VarselFactory(event.getAvtale(), Avtalerolle.VEILEDER, null, HendelseType.GODKJENNINGER_OPPHEVET_AV_VEILEDER);
        varselRepository.saveAll(factory.alleParter());
    }
    @EventListener
    public void godkjentAvDeltaker(GodkjentAvDeltaker event) {
        VarselFactory factory = new VarselFactory(event.getAvtale(), Avtalerolle.DELTAKER, event.getUtfortAv(), HendelseType.GODKJENT_AV_DELTAKER);
        varselRepository.saveAll(factory.alleParter());
    }

    @EventListener
    public void signertAvMentor(SignertAvMentor event) {
        VarselFactory factory = new VarselFactory(event.getAvtale(), Avtalerolle.MENTOR, event.getUtfortAv(), HendelseType.SIGNERT_AV_MENTOR);
        varselRepository.saveAll(factory.alleParter());
    }

    @EventListener
    public void godkjentAvArbeidsgiver(GodkjentAvArbeidsgiver event) {
        VarselFactory factory = new VarselFactory(event.getAvtale(), Avtalerolle.ARBEIDSGIVER, event.getUtfortAv(), HendelseType.GODKJENT_AV_ARBEIDSGIVER);
        varselRepository.saveAll(factory.alleParter());
    }

    @EventListener
    public void godkjentAvVeileder(GodkjentAvVeileder event) {
        VarselFactory factory = new VarselFactory(event.getAvtale(), Avtalerolle.VEILEDER, event.getUtfortAv(), HendelseType.GODKJENT_AV_VEILEDER);
        varselRepository.save(factory.veileder());
    }

    @EventListener
    public void godkjentPaVegneAv(GodkjentPaVegneAvDeltaker event) {
        VarselFactory factory = new VarselFactory(event.getAvtale(), Avtalerolle.VEILEDER, event.getUtfortAv(), HendelseType.GODKJENT_PAA_VEGNE_AV);
        varselRepository.save(factory.veileder());
    }

    @EventListener
    public void godkjentPaVegneAvArbeidsgiver(GodkjentPaVegneAvArbeidsgiver event) {
        VarselFactory factory = new VarselFactory(event.getAvtale(), Avtalerolle.VEILEDER, event.getUtfortAv(), HendelseType.GODKJENT_PAA_VEGNE_AV_ARBEIDSGIVER);
        varselRepository.save(factory.veileder());
    }

    @EventListener
    public void godkjentPaVegneAvDeltakerOgArbeidsgiver(GodkjentPaVegneAvDeltakerOgArbeidsgiver event) {
        VarselFactory factory = new VarselFactory(event.getAvtale(), Avtalerolle.VEILEDER, event.getUtfortAv(), HendelseType.GODKJENT_PAA_VEGNE_AV_DELTAKER_OG_ARBEIDSGIVER);
        varselRepository.save(factory.veileder());
    }

    @EventListener
    public void godkjentForEtterregistrering(GodkjentForEtterregistrering event) {
        VarselFactory factory = new VarselFactory (event.getAvtale(), Avtalerolle.BESLUTTER, event.getUtfortAv(), HendelseType.GODKJENT_FOR_ETTERREGISTRERING);
        varselRepository.save(factory.veileder());
    }

    @EventListener
    public void fjernetEtterregistrering(FjernetEtterregistrering event) {
        VarselFactory factory = new VarselFactory (event.getAvtale(), Avtalerolle.BESLUTTER, event.getUtfortAv(), HendelseType.FJERNET_ETTERREGISTRERING);
        varselRepository.save(factory.veileder());
    }

    @EventListener
    public void avtaleInngått(AvtaleInngått event) {
        VarselFactory factory = new VarselFactory(event.getAvtale(), event.getUtførtAvRolle(), event.getUtførtAv(), HendelseType.AVTALE_INNGÅTT);
        varselRepository.saveAll(factory.alleParter());
    }

    @EventListener
    public void nyVeileder(AvtaleNyVeileder event) {
        VarselFactory factory = new VarselFactory(event.getAvtale(), Avtalerolle.VEILEDER, event.getAvtale().getVeilederNavIdent(), HendelseType.NY_VEILEDER);
        varselRepository.saveAll(factory.alleParter());
    }

    @EventListener
    public void fordelt(AvtaleOpprettetAvArbeidsgiverErFordelt event) {
        VarselFactory factory = new VarselFactory(event.getAvtale(), Avtalerolle.VEILEDER, event.getAvtale().getVeilederNavIdent(), HendelseType.AVTALE_FORDELT);
        varselRepository.saveAll(factory.alleParter());
    }

    @EventListener
    public void avbrutt(AvbruttAvVeileder event) {
        VarselFactory factory = new VarselFactory(event.getAvtale(), Avtalerolle.VEILEDER, event.getUtfortAv(), HendelseType.AVBRUTT);
        varselRepository.saveAll(factory.alleParter());
    }

    @EventListener
    public void annullert(AnnullertAvVeileder event) {
        VarselFactory factory = new VarselFactory(event.getAvtale(), Avtalerolle.VEILEDER, event.getUtfortAv(), HendelseType.ANNULLERT);
        varselRepository.saveAll(factory.alleParter());
    }

    @EventListener
    public void låstOpp(AvtaleLåstOpp event) {
        VarselFactory factory = new VarselFactory(event.getAvtale(), Avtalerolle.VEILEDER, null, HendelseType.LÅST_OPP);
        varselRepository.saveAll(factory.alleParter());
    }

    @EventListener
    public void gjenopprettet(AvtaleGjenopprettet event) {
        VarselFactory factory = new VarselFactory(event.getAvtale(), Avtalerolle.VEILEDER, event.getUtfortAv(), HendelseType.GJENOPPRETTET);
        varselRepository.saveAll(factory.alleParter());
    }

    @EventListener
    public void forkortAvtale(AvtaleForkortet event) {
        VarselFactory factory = new VarselFactory(event.getAvtale(), Avtalerolle.VEILEDER, event.getUtførtAv(), HendelseType.AVTALE_FORKORTET);
        varselRepository.saveAll(factory.alleParter());
    }

    @EventListener
    public void forlengAvtale(AvtaleForlenget event) {
        VarselFactory factory = new VarselFactory(event.getAvtale(), Avtalerolle.VEILEDER, event.getUtførtAv(), HendelseType.AVTALE_FORLENGET);
        varselRepository.saveAll(factory.alleParter());
    }

    @EventListener
    public void målEndret(MålEndret event) {
        VarselFactory factory = new VarselFactory(event.getAvtale(), Avtalerolle.VEILEDER, event.getUtførtAv(), HendelseType.MÅL_ENDRET);
        varselRepository.saveAll(factory.alleParter());
    }

    @EventListener
    public void inkluderingstilskuddEndret(InkluderingstilskuddEndret event) {
        VarselFactory factory = new VarselFactory(event.getAvtale(), Avtalerolle.VEILEDER, event.getUtførtAv(), HendelseType.INKLUDERINGSTILSKUDD_ENDRET);
        varselRepository.saveAll(factory.alleParter());
    }

    @EventListener
    public void omMentorEndret(OmMentorEndret event) {
        VarselFactory factory = new VarselFactory(event.getAvtale(), Avtalerolle.VEILEDER, event.getUtførtAv(), HendelseType.OM_MENTOR_ENDRET);
        varselRepository.saveAll(factory.alleParter());
    }

    @EventListener
    public void endreTilskuddsberegning(TilskuddsberegningEndret event) {
        VarselFactory factory = new VarselFactory(event.getAvtale(), Avtalerolle.VEILEDER, event.getUtførtAv(), HendelseType.TILSKUDDSBEREGNING_ENDRET);
        varselRepository.saveAll(factory.alleParter());
    }

    @EventListener
    public void endreKontaktInformasjon(KontaktinformasjonEndret event) {
        VarselFactory factory = new VarselFactory(event.getAvtale(), Avtalerolle.VEILEDER, event.getUtførtAv(), HendelseType.KONTAKTINFORMASJON_ENDRET);
        varselRepository.saveAll(factory.alleParter());
    }

    @EventListener
    public void endreStillingbeskrivelse(StillingsbeskrivelseEndret event) {
        VarselFactory factory = new VarselFactory(event.getAvtale(), Avtalerolle.VEILEDER, event.getUtførtAv(), HendelseType.STILLINGSBESKRIVELSE_ENDRET);
        varselRepository.saveAll(factory.alleParter());
    }

    @EventListener
    public void endreOppfølgingOgTilretteleggingInformasjon(OppfølgingOgTilretteleggingEndret event) {
        VarselFactory factory = new VarselFactory(event.getAvtale(), Avtalerolle.VEILEDER, event.getUtførtAv(), HendelseType.OPPFØLGING_OG_TILRETTELEGGING_ENDRET);
        varselRepository.saveAll(factory.alleParter());
    }
}
