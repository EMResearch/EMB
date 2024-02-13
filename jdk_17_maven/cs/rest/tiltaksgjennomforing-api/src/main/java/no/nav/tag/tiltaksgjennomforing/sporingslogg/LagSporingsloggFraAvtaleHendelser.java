package no.nav.tag.tiltaksgjennomforing.sporingslogg;

import lombok.RequiredArgsConstructor;
import no.nav.tag.tiltaksgjennomforing.avtale.Avtalerolle;
import no.nav.tag.tiltaksgjennomforing.avtale.HendelseType;
import no.nav.tag.tiltaksgjennomforing.avtale.events.AvbruttAvVeileder;
import no.nav.tag.tiltaksgjennomforing.avtale.events.AvtaleDeltMedAvtalepart;
import no.nav.tag.tiltaksgjennomforing.avtale.events.AvtaleEndret;
import no.nav.tag.tiltaksgjennomforing.avtale.events.AvtaleGjenopprettet;
import no.nav.tag.tiltaksgjennomforing.avtale.events.AvtaleInngått;
import no.nav.tag.tiltaksgjennomforing.avtale.events.AvtaleLåstOpp;
import no.nav.tag.tiltaksgjennomforing.avtale.events.AvtaleNyVeileder;
import no.nav.tag.tiltaksgjennomforing.avtale.events.AvtaleOpprettetAvArbeidsgiver;
import no.nav.tag.tiltaksgjennomforing.avtale.events.AvtaleOpprettetAvArbeidsgiverErFordelt;
import no.nav.tag.tiltaksgjennomforing.avtale.events.AvtaleOpprettetAvVeileder;
import no.nav.tag.tiltaksgjennomforing.avtale.events.GodkjenningerOpphevetAvArbeidsgiver;
import no.nav.tag.tiltaksgjennomforing.avtale.events.GodkjenningerOpphevetAvVeileder;
import no.nav.tag.tiltaksgjennomforing.avtale.events.GodkjentAvArbeidsgiver;
import no.nav.tag.tiltaksgjennomforing.avtale.events.GodkjentAvDeltaker;
import no.nav.tag.tiltaksgjennomforing.avtale.events.GodkjentAvVeileder;
import no.nav.tag.tiltaksgjennomforing.avtale.events.GodkjentPaVegneAvArbeidsgiver;
import no.nav.tag.tiltaksgjennomforing.avtale.events.GodkjentPaVegneAvDeltaker;
import no.nav.tag.tiltaksgjennomforing.avtale.events.GodkjentPaVegneAvDeltakerOgArbeidsgiver;
import no.nav.tag.tiltaksgjennomforing.avtale.events.RefusjonFristForlenget;
import no.nav.tag.tiltaksgjennomforing.avtale.events.RefusjonKlar;
import no.nav.tag.tiltaksgjennomforing.avtale.events.RefusjonKlarRevarsel;
import no.nav.tag.tiltaksgjennomforing.avtale.events.RefusjonKorrigert;
import no.nav.tag.tiltaksgjennomforing.avtale.events.SignertAvMentor;
import no.nav.tag.tiltaksgjennomforing.avtale.events.TilskuddsperiodeAvslått;
import no.nav.tag.tiltaksgjennomforing.avtale.events.TilskuddsperiodeGodkjent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LagSporingsloggFraAvtaleHendelser {
    private final SporingsloggRepository sporingsloggRepository;

    @EventListener
    public void avtaleOpprettet(AvtaleOpprettetAvVeileder event) {
        sporingsloggRepository.save(Sporingslogg.nyHendelse(event.getAvtale(), HendelseType.OPPRETTET));
    }

    @EventListener
    public void avtaleKlarForRefusjon(RefusjonKlar event) {
        sporingsloggRepository.save(Sporingslogg.nyHendelse(event.getAvtale(), HendelseType.REFUSJON_KLAR));
    }

    @EventListener
    public void avtaleKlarForRefusjonRevarsel(RefusjonKlarRevarsel event) {
        sporingsloggRepository.save(Sporingslogg.nyHendelse(event.getAvtale(), HendelseType.REFUSJON_KLAR_REVARSEL));
    }

    @EventListener
    public void refusjonFristForlengetVarsel(RefusjonFristForlenget event) {
        sporingsloggRepository.save(Sporingslogg.nyHendelse(event.getAvtale(), HendelseType.REFUSJON_FRIST_FORLENGET));
    }

    @EventListener
    public void refusjonKorrigert(RefusjonKorrigert event) {
        sporingsloggRepository.save(Sporingslogg.nyHendelse(event.getAvtale(), HendelseType.REFUSJON_KORRIGERT));
    }

    @EventListener
    public void avtaleOpprettetAvArbeidsgiver(AvtaleOpprettetAvArbeidsgiver event) {
        sporingsloggRepository.save(Sporingslogg.nyHendelse(event.getAvtale(), HendelseType.OPPRETTET_AV_ARBEIDSGIVER));
    }

    @EventListener
    public void avtaleDeltMedAvtalepart(AvtaleDeltMedAvtalepart event) {
        if (event.getAvtalepart() == Avtalerolle.ARBEIDSGIVER) {
            sporingsloggRepository.save(Sporingslogg.nyHendelse(event.getAvtale(), HendelseType.DELT_MED_ARBEIDSGIVER));
        } else if (event.getAvtalepart() == Avtalerolle.DELTAKER) {
            sporingsloggRepository.save(Sporingslogg.nyHendelse(event.getAvtale(), HendelseType.DELT_MED_DELTAKER));
        } else if (event.getAvtalepart() == Avtalerolle.MENTOR) {
            sporingsloggRepository.save(Sporingslogg.nyHendelse(event.getAvtale(), HendelseType.DELT_MED_MENTOR));
        }
    }

    @EventListener
    public void tilskuddsperiodeAvslått(TilskuddsperiodeAvslått event) {
        sporingsloggRepository.save(Sporingslogg.nyHendelse(event.getAvtale(), HendelseType.TILSKUDDSPERIODE_AVSLATT));
    }

    @EventListener
    public void tilskuddsperiodeGodkjent(TilskuddsperiodeGodkjent event) {
        sporingsloggRepository.save(Sporingslogg.nyHendelse(event.getAvtale(), HendelseType.TILSKUDDSPERIODE_GODKJENT));
    }

    @EventListener
    public void avtaleEndret(AvtaleEndret event) {
        sporingsloggRepository.save(Sporingslogg.nyHendelse(event.getAvtale(), HendelseType.ENDRET));
    }

    @EventListener
    public void godkjenningerOpphevetAvArbeidsgiver(GodkjenningerOpphevetAvArbeidsgiver event) {
        sporingsloggRepository.save(Sporingslogg.nyHendelse(event.getAvtale(), HendelseType.GODKJENNINGER_OPPHEVET_AV_ARBEIDSGIVER));
    }

    @EventListener
    public void godkjenningerOpphevetAvVeileder(GodkjenningerOpphevetAvVeileder event) {
        sporingsloggRepository.save(Sporingslogg.nyHendelse(event.getAvtale(), HendelseType.GODKJENNINGER_OPPHEVET_AV_VEILEDER));
    }

    @EventListener
    public void godkjentAvDeltaker(GodkjentAvDeltaker event) {
        sporingsloggRepository.save(Sporingslogg.nyHendelse(event.getAvtale(), HendelseType.GODKJENT_AV_DELTAKER));
    }

    @EventListener
    public void godkjentAvDeltaker(SignertAvMentor event) {
        sporingsloggRepository.save(Sporingslogg.nyHendelse(event.getAvtale(), HendelseType.GODKJENT_AV_DELTAKER));
    }

    @EventListener
    public void godkjentAvArbeidsgiver(GodkjentAvArbeidsgiver event) {
        sporingsloggRepository.save(Sporingslogg.nyHendelse(event.getAvtale(), HendelseType.GODKJENT_AV_ARBEIDSGIVER));
    }

    @EventListener
    public void godkjentAvVeileder(GodkjentAvVeileder event) {
        sporingsloggRepository.save(Sporingslogg.nyHendelse(event.getAvtale(), HendelseType.GODKJENT_AV_VEILEDER));
    }

    @EventListener
    public void avtaleInngått(AvtaleInngått event) {
        sporingsloggRepository.save(Sporingslogg.nyHendelse(event.getAvtale(), HendelseType.AVTALE_INNGÅTT));
    }

    @EventListener
    public void godkjentPaVegneAv(GodkjentPaVegneAvDeltaker event) {
        sporingsloggRepository.save(Sporingslogg.nyHendelse(event.getAvtale(), HendelseType.GODKJENT_PAA_VEGNE_AV));
    }

    @EventListener
    public void godkjentPaVegneAvArbeidsgiver(GodkjentPaVegneAvArbeidsgiver event) {
        sporingsloggRepository.save(Sporingslogg.nyHendelse(event.getAvtale(), HendelseType.GODKJENT_PAA_VEGNE_AV_ARBEIDSGIVER));
    }

    @EventListener
    public void godkjentPaVegneAvDeltakerOgArbeidsgiver(GodkjentPaVegneAvDeltakerOgArbeidsgiver event) {
        sporingsloggRepository.save(Sporingslogg.nyHendelse(event.getAvtale(), HendelseType.GODKJENT_PAA_VEGNE_AV_DELTAKER_OG_ARBEIDSGIVER));
    }

    @EventListener
    public void nyVeileder(AvtaleNyVeileder event) {
        sporingsloggRepository.save(Sporingslogg.nyHendelse(event.getAvtale(), HendelseType.NY_VEILEDER));
    }

    @EventListener
    public void fordelt(AvtaleOpprettetAvArbeidsgiverErFordelt event) {
        sporingsloggRepository.save(Sporingslogg.nyHendelse(event.getAvtale(), HendelseType.AVTALE_FORDELT));
    }

    @EventListener
    public void avbrutt(AvbruttAvVeileder event) {
        sporingsloggRepository.save(Sporingslogg.nyHendelse(event.getAvtale(), HendelseType.AVBRUTT));
    }

    @EventListener
    public void låstOpp(AvtaleLåstOpp event) {
        sporingsloggRepository.save(Sporingslogg.nyHendelse(event.getAvtale(), HendelseType.LÅST_OPP));
    }

    @EventListener
    public void gjenopprettet(AvtaleGjenopprettet event) {
        sporingsloggRepository.save(Sporingslogg.nyHendelse(event.getAvtale(), HendelseType.GJENOPPRETTET));
    }
}
