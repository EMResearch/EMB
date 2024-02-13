package no.nav.tag.tiltaksgjennomforing.metrikker;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.tag.tiltaksgjennomforing.avtale.Avtalerolle;
import no.nav.tag.tiltaksgjennomforing.avtale.Tiltakstype;
import no.nav.tag.tiltaksgjennomforing.avtale.events.AvtaleDeltMedAvtalepart;
import no.nav.tag.tiltaksgjennomforing.avtale.events.AvtaleEndret;
import no.nav.tag.tiltaksgjennomforing.avtale.events.AvtaleInngått;
import no.nav.tag.tiltaksgjennomforing.avtale.events.AvtaleLåstOpp;
import no.nav.tag.tiltaksgjennomforing.avtale.events.AvtaleNyVeileder;
import no.nav.tag.tiltaksgjennomforing.avtale.events.AvtaleOpprettetAvArbeidsgiver;
import no.nav.tag.tiltaksgjennomforing.avtale.events.AvtaleOpprettetAvVeileder;
import no.nav.tag.tiltaksgjennomforing.avtale.events.AvtaleSlettemerket;
import no.nav.tag.tiltaksgjennomforing.avtale.events.GodkjenningerOpphevetAvArbeidsgiver;
import no.nav.tag.tiltaksgjennomforing.avtale.events.GodkjenningerOpphevetAvVeileder;
import no.nav.tag.tiltaksgjennomforing.avtale.events.GodkjentAvArbeidsgiver;
import no.nav.tag.tiltaksgjennomforing.avtale.events.GodkjentAvDeltaker;
import no.nav.tag.tiltaksgjennomforing.avtale.events.GodkjentAvVeileder;
import no.nav.tag.tiltaksgjennomforing.avtale.events.GodkjentPaVegneAvArbeidsgiver;
import no.nav.tag.tiltaksgjennomforing.avtale.events.GodkjentPaVegneAvDeltaker;
import no.nav.tag.tiltaksgjennomforing.avtale.events.GodkjentPaVegneAvDeltakerOgArbeidsgiver;
import no.nav.tag.tiltaksgjennomforing.avtale.events.SignertAvMentor;
import no.nav.tag.tiltaksgjennomforing.avtale.events.TilskuddsperiodeAvslått;
import no.nav.tag.tiltaksgjennomforing.avtale.events.TilskuddsperiodeGodkjent;
import no.nav.tag.tiltaksgjennomforing.varsel.events.SmsSendt;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class MetrikkRegistrering {
    private final MeterRegistry meterRegistry;

    @EventListener
    public void smsSendt(SmsSendt event) {
        Counter.builder("tiltaksgjennomforing.smsvarsel.sendt").register(meterRegistry).increment();
    }

    @EventListener
    public void avtaleOpprettet(AvtaleOpprettetAvVeileder event) {
        Tiltakstype tiltakstype = event.getAvtale().getTiltakstype();
        log.info("Avtale opprettet av veileder, avtaleId={} ident={}, tiltakstype={}", event.getAvtale().getId(), event.getUtfortAv(), tiltakstype);
        counter("avtale.opprettet", Avtalerolle.VEILEDER, tiltakstype).increment();
    }

    @EventListener
    public void avtaleOpprettetAvArbeidsgiver(AvtaleOpprettetAvArbeidsgiver event) {
        Tiltakstype tiltakstype = event.getAvtale().getTiltakstype();
        log.info("Avtale opprettet av arbeidsgiver, avtaleId={}, tiltakstype={}", event.getAvtale().getId(), tiltakstype);
        counter("avtale.opprettet", Avtalerolle.ARBEIDSGIVER, tiltakstype).increment();
    }

    @EventListener
    public void avtaleEndret(AvtaleEndret event) {
        Tiltakstype tiltakstype = event.getAvtale().getTiltakstype();
        log.info("Avtale endret, avtaleId={}, avtalepart={}, tiltakstype={}", event.getAvtale().getId(), event.getUtfortAvRolle(), tiltakstype);
        counter("avtale.endret", event.getUtfortAvRolle(), tiltakstype).increment();
    }

    @EventListener
    public void godkjenningerOpphevet(GodkjenningerOpphevetAvVeileder event) {
        Avtalerolle rolle = Avtalerolle.VEILEDER;
        Tiltakstype tiltakstype = event.getAvtale().getTiltakstype();
        log.info("Avtalens godkjenninger opphevet, avtaleId={}, avtalepart={}, tiltakstype={}", event.getAvtale().getId(), rolle, tiltakstype);
        counter("avtale.godkjenning.opphevet", rolle, tiltakstype).increment();
    }

    @EventListener
    public void godkjenningerOpphevet(GodkjenningerOpphevetAvArbeidsgiver event) {
        Avtalerolle rolle = Avtalerolle.ARBEIDSGIVER;
        Tiltakstype tiltakstype = event.getAvtale().getTiltakstype();
        log.info("Avtalens godkjenninger opphevet, avtaleId={}, avtalepart={}, tiltakstype={}", event.getAvtale().getId(), rolle, tiltakstype);
        counter("avtale.godkjenning.opphevet", rolle, tiltakstype).increment();
    }

    @EventListener
    public void godkjentAvDeltaker(GodkjentAvDeltaker event) {
        Avtalerolle rolle = Avtalerolle.DELTAKER;
        Tiltakstype tiltakstype = event.getAvtale().getTiltakstype();
        log.info("Avtale godkjent, avtaleId={}, avtalepart={}, tiltakstype={}", event.getAvtale().getId(), rolle, tiltakstype);
        counter("avtale.godkjenning.godkjent", rolle, tiltakstype).increment();
    }

    @EventListener
    public void godkjentAvMentor(SignertAvMentor event) {
        Avtalerolle rolle = Avtalerolle.MENTOR;
        Tiltakstype tiltakstype = event.getAvtale().getTiltakstype();
        log.info("Mentor har signert taushetserklæring, avtaleId={}, avtalepart={}, tiltakstype={}", event.getAvtale().getId(), rolle, tiltakstype);
        counter("avtale.godkjenning.godkjent", rolle, tiltakstype).increment();
    }

    @EventListener
    public void godkjentAvArbeidsgiver(GodkjentAvArbeidsgiver event) {
        Avtalerolle rolle = Avtalerolle.ARBEIDSGIVER;
        Tiltakstype tiltakstype = event.getAvtale().getTiltakstype();
        log.info("Avtale godkjent, avtaleId={}, avtalepart={}, tiltakstype={}", event.getAvtale().getId(), rolle, tiltakstype);
        counter("avtale.godkjenning.godkjent", rolle, tiltakstype).increment();
    }

    @EventListener
    public void godkjentAvVeileder(GodkjentAvVeileder event) {
        Avtalerolle rolle = Avtalerolle.VEILEDER;
        Tiltakstype tiltakstype = event.getAvtale().getTiltakstype();
        log.info("Avtale godkjent, avtaleId={}, avtalepart={}, tiltakstype={}", event.getAvtale().getId(), rolle, tiltakstype);
        counter("avtale.godkjenning.godkjent", rolle, tiltakstype).increment();
    }

    @EventListener
    public void avtaleInngått(AvtaleInngått event) {
        Avtalerolle rolle = event.getUtførtAvRolle();
        Tiltakstype tiltakstype = event.getAvtale().getTiltakstype();
        log.info("Avtale inngått, avtaleId={}, avtalepart={}, tiltakstype={}", event.getAvtale().getId(), rolle, tiltakstype);
        counter("avtale.godkjenning.inngaatt", rolle, tiltakstype).increment();
    }

    @EventListener
    public void tilskuddsperiodeGodkjent(TilskuddsperiodeGodkjent event) {
        Avtalerolle rolle = Avtalerolle.BESLUTTER;
        Tiltakstype tiltakstype = event.getAvtale().getTiltakstype();
        log.info("Tilskuddsperiode godkjent, avtaleId={}, tilskuddsperiodeId={}, løpenummer={}, avtalepart={}, tiltakstype={}", event.getAvtale().getId(), event.getTilskuddsperiode().getId(), event.getTilskuddsperiode().getLøpenummer(), rolle, tiltakstype);
        counter("avtale.tilskuddsperiode.godkjent", rolle, tiltakstype).increment();
    }

    @EventListener
    public void tilskuddsperiodeAvslått(TilskuddsperiodeAvslått event) {
        Avtalerolle rolle = Avtalerolle.BESLUTTER;
        Tiltakstype tiltakstype = event.getAvtale().getTiltakstype();
        log.info("Tilskuddsperiode avslått, avtaleId={}, tilskuddsperiodeId={}, løpenummer={}, avtalepart={}, tiltakstype={}", event.getAvtale().getId(), event.getTilskuddsperiode().getId(), event.getTilskuddsperiode().getLøpenummer(), rolle, tiltakstype);
        counter("avtale.tilskuddsperiode.avslaatt", rolle, tiltakstype).increment();
    }

    @EventListener
    public void godkjentPaVegneAv(GodkjentPaVegneAvDeltaker event) {
        Avtalerolle rolle = Avtalerolle.VEILEDER;
        Tiltakstype tiltakstype = event.getAvtale().getTiltakstype();
        log.info("Avtale godkjent på vegne av deltaker, avtaleId={}, avtalepart={}, tiltakstype={}", event.getAvtale().getId(), rolle, tiltakstype);
        counter("avtale.godkjenning.godkjentPaVegneAv", rolle, tiltakstype).increment();
    }

    @EventListener
    public void godkjentPaVegneAvArbeidsgiver(GodkjentPaVegneAvArbeidsgiver event) {
        Avtalerolle rolle = Avtalerolle.VEILEDER;
        Tiltakstype tiltakstype = event.getAvtale().getTiltakstype();
        log.info("Avtale godkjent på vegne av arbeidsgiver, avtaleId={}, avtalepart={}, tiltakstype={}", event.getAvtale().getId(), rolle, tiltakstype);
        counter("avtale.godkjenning.godkjentPaVegneAvArbeidsgiver", rolle, tiltakstype).increment();
    }

    @EventListener
    public void godkjentPaVegneAvDeltakerOgArbeidsgiver(GodkjentPaVegneAvDeltakerOgArbeidsgiver event) {
        Avtalerolle rolle = Avtalerolle.VEILEDER;
        Tiltakstype tiltakstype = event.getAvtale().getTiltakstype();
        log.info("Avtale godkjent på vegne av deltaker og arbeidsgiver, avtaleId={}, avtalepart={}, tiltakstype={}", event.getAvtale().getId(), rolle, tiltakstype);
        counter("avtale.godkjenning.godkjentPaVegneAvDeltakerOgArbeidsgiver", rolle, tiltakstype).increment();
    }

    @EventListener
    public void avtaleLåstOpp(AvtaleLåstOpp event) {
        Avtalerolle rolle = Avtalerolle.VEILEDER;
        Tiltakstype tiltakstype = event.getAvtale().getTiltakstype();
        log.info("Avtale låst opp, avtaleId={}, avtalepart={}, tiltakstype={}", event.getAvtale().getId(), rolle, tiltakstype);
        counter("avtale.laastOpp", rolle, tiltakstype).increment();
    }

    @EventListener
    public void avtaleDeltMedAvtalepart(AvtaleDeltMedAvtalepart event) {
        Avtalerolle rolle = Avtalerolle.VEILEDER;
        Tiltakstype tiltakstype = event.getAvtale().getTiltakstype();
        log.info("Avtale delt med {}, avtaleId={}, avtalepart={}, tiltakstype={}", event.getAvtalepart(), event.getAvtale().getId(), rolle, tiltakstype);
        counter("avtale.deltMedAvtalepart", rolle, tiltakstype).increment();
    }

    @EventListener
    public void avtaleNyVeileder(AvtaleNyVeileder event) {
        Avtalerolle rolle = Avtalerolle.VEILEDER;
        Tiltakstype tiltakstype = event.getAvtale().getTiltakstype();
        if (event.getTidligereVeileder() == null) {
            log.info("Avtale tildelt veileder: avtaleId={}, veileder={}", event.getAvtale().getId(), event.getAvtale().getVeilederNavIdent().asString());
        } else {
            log.info("Avtale byttet veileder: avtaleId={}, tidligere veileder={}, ny veileder={}", event.getAvtale().getId(), event.getTidligereVeileder().asString(), event.getAvtale().getVeilederNavIdent().asString());
        }
        counter("avtale.endretVEileder", rolle, tiltakstype).increment();
    }
    @EventListener
    public void avtaleSlettemerket(AvtaleSlettemerket event) {
        log.info("Avtale slettemerket, utfortAv={}, avtaleId={}", event.getUtfortAv().asString(), event.getAvtale().getId());
    }

    private Counter counter(String navn, Avtalerolle avtalerolle, Tiltakstype tiltakstype) {
        var builder = Counter.builder("tiltaksgjennomforing." + navn)
                .tag("tiltak", tiltakstype.name())
                .tag("avtalepart", avtalerolle.name());
        return builder.register(meterRegistry);
    }

}
