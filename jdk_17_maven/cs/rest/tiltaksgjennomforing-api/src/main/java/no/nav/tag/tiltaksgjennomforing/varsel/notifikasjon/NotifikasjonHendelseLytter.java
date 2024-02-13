package no.nav.tag.tiltaksgjennomforing.varsel.notifikasjon;

import lombok.RequiredArgsConstructor;
import no.nav.tag.tiltaksgjennomforing.avtale.Avtale;
import no.nav.tag.tiltaksgjennomforing.avtale.HendelseType;
import no.nav.tag.tiltaksgjennomforing.avtale.events.*;
import no.nav.tag.tiltaksgjennomforing.varsel.notifikasjon.response.MutationStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty("tiltaksgjennomforing.notifikasjoner.enabled")
public class NotifikasjonHendelseLytter {

    private final ArbeidsgiverNotifikasjonRepository arbeidsgiverNotifikasjonRepository;
    private final NotifikasjonService notifikasjonService;
    private final NotifikasjonParser parser;

    private void opprettOgSendNyBeskjed(Avtale avtale, HendelseType hendelseType, NotifikasjonTekst tekst) {
        final ArbeidsgiverNotifikasjon notifikasjon = ArbeidsgiverNotifikasjon.nyHendelse(avtale,
                hendelseType, notifikasjonService, parser);
        arbeidsgiverNotifikasjonRepository.save(notifikasjon);
        notifikasjonService.opprettNyBeskjed(notifikasjon,
                NotifikasjonMerkelapp.getMerkelapp(avtale.getTiltakstype().getBeskrivelse()),
                tekst);
    }

    @EventListener
    public void avtaleOpprettet(AvtaleOpprettetAvVeileder event) {
        final ArbeidsgiverNotifikasjon notifikasjon = ArbeidsgiverNotifikasjon.nyHendelse(event.getAvtale(),
                HendelseType.OPPRETTET, notifikasjonService, parser);
        arbeidsgiverNotifikasjonRepository.save(notifikasjon);
        notifikasjonService.opprettOppgave(notifikasjon,
                NotifikasjonMerkelapp.getMerkelapp(event.getAvtale().getTiltakstype().getBeskrivelse()),
                NotifikasjonTekst.TILTAK_AVTALE_OPPRETTET);
    }

    @EventListener
    public void godkjenningerOpphevetAvVeileder(GodkjenningerOpphevetAvVeileder event) {
        final ArbeidsgiverNotifikasjon notifikasjon = ArbeidsgiverNotifikasjon.nyHendelse(event.getAvtale(),
                HendelseType.GODKJENNINGER_OPPHEVET_AV_VEILEDER, notifikasjonService, parser);
        arbeidsgiverNotifikasjonRepository.save(notifikasjon);
        notifikasjonService.opprettOppgave(notifikasjon,
                NotifikasjonMerkelapp.getMerkelapp(event.getAvtale().getTiltakstype().getBeskrivelse()),
                NotifikasjonTekst.TILTAK_GODKJENNINGER_OPPHEVET_AV_VEILEDER);
    }

    @EventListener
    public void avtaleKlarForRefusjon(RefusjonKlar event) {
        opprettOgSendNyBeskjed(event.getAvtale(), HendelseType.REFUSJON_KLAR,
                NotifikasjonTekst.TILTAK_AVTALE_KLAR_REFUSJON);
    }

    @EventListener
    public void godkjentAvArbeidsgiver(GodkjentAvArbeidsgiver event) {
        notifikasjonService.oppgaveUtfoert(
                event.getAvtale(), HendelseType.OPPRETTET,
                MutationStatus.NY_OPPGAVE_VELLYKKET, HendelseType.AVTALE_INNGÅTT);
        notifikasjonService.oppgaveUtfoert(event.getAvtale(),
                HendelseType.GODKJENNINGER_OPPHEVET_AV_VEILEDER, MutationStatus.NY_OPPGAVE_VELLYKKET, HendelseType.AVTALE_INNGÅTT);
    }

    @EventListener
    public void godkjentAvVeileder(GodkjentAvVeileder event) {
        notifikasjonService.oppgaveUtfoert(event.getAvtale(),
                HendelseType.OPPRETTET, MutationStatus.NY_OPPGAVE_VELLYKKET, HendelseType.AVTALE_INNGÅTT);
        notifikasjonService.oppgaveUtfoert(event.getAvtale(),
                HendelseType.GODKJENNINGER_OPPHEVET_AV_VEILEDER, MutationStatus.NY_OPPGAVE_VELLYKKET, HendelseType.AVTALE_INNGÅTT);
    }

    @EventListener
    public void godkjentPaVegneAv(GodkjentPaVegneAvDeltaker event) {
        notifikasjonService.oppgaveUtfoert(event.getAvtale(),
                HendelseType.OPPRETTET, MutationStatus.NY_OPPGAVE_VELLYKKET, HendelseType.AVTALE_INNGÅTT);
        notifikasjonService.oppgaveUtfoert(event.getAvtale(),
                HendelseType.GODKJENNINGER_OPPHEVET_AV_VEILEDER, MutationStatus.NY_OPPGAVE_VELLYKKET, HendelseType.AVTALE_INNGÅTT);
    }

    @EventListener
    public void godkjentPaVegneAvArbeidsgiver(GodkjentPaVegneAvArbeidsgiver event) {
        notifikasjonService.oppgaveUtfoert(event.getAvtale(),
                HendelseType.OPPRETTET, MutationStatus.NY_OPPGAVE_VELLYKKET, HendelseType.AVTALE_INNGÅTT);
        notifikasjonService.oppgaveUtfoert(event.getAvtale(),
                HendelseType.GODKJENNINGER_OPPHEVET_AV_VEILEDER, MutationStatus.NY_OPPGAVE_VELLYKKET, HendelseType.AVTALE_INNGÅTT);
    }

    @EventListener
    public void godkjentPaVegneAvDeltakerOgArbeidsgiver(GodkjentPaVegneAvDeltakerOgArbeidsgiver event) {
        notifikasjonService.oppgaveUtfoert(event.getAvtale(),
                HendelseType.OPPRETTET, MutationStatus.NY_OPPGAVE_VELLYKKET, HendelseType.AVTALE_INNGÅTT);
        notifikasjonService.oppgaveUtfoert(event.getAvtale(),
                HendelseType.GODKJENNINGER_OPPHEVET_AV_VEILEDER, MutationStatus.NY_OPPGAVE_VELLYKKET, HendelseType.AVTALE_INNGÅTT);
    }

    @EventListener
    public void avtaleInngått(AvtaleInngått event) {
        notifikasjonService.oppgaveUtfoert(event.getAvtale(),
                HendelseType.OPPRETTET, MutationStatus.NY_OPPGAVE_VELLYKKET, HendelseType.AVTALE_INNGÅTT);
        notifikasjonService.oppgaveUtfoert(event.getAvtale(),
                HendelseType.GODKJENNINGER_OPPHEVET_AV_VEILEDER, MutationStatus.NY_OPPGAVE_VELLYKKET, HendelseType.AVTALE_INNGÅTT);
        opprettOgSendNyBeskjed(event.getAvtale(), HendelseType.AVTALE_INNGÅTT, NotifikasjonTekst.TILTAK_AVTALE_INNGATT);
    }

    @EventListener
    public void sletteNotifikasjon(AvtaleSlettemerket event) {
        notifikasjonService.softDeleteNotifikasjoner(event.getAvtale());
    }

    @EventListener
    public void avtaleAnnullert(AnnullertAvVeileder event) {
        notifikasjonService.softDeleteNotifikasjoner(event.getAvtale());
    }

    @EventListener
    public void målEndret(MålEndret event) {
        opprettOgSendNyBeskjed(event.getAvtale(), HendelseType.MÅL_ENDRET,
                NotifikasjonTekst.TILTAK_MÅL_ENDRET);
    }

    @EventListener
    public void inkluderingstilskuddEndret(InkluderingstilskuddEndret event) {
        opprettOgSendNyBeskjed(event.getAvtale(), HendelseType.INKLUDERINGSTILSKUDD_ENDRET,
                NotifikasjonTekst.TILTAK_INKLUDERINGSTILSKUDD_ENDRET);
    }

    @EventListener
    public void omMentorEndret(OmMentorEndret event) {
        opprettOgSendNyBeskjed(event.getAvtale(), HendelseType.OM_MENTOR_ENDRET,
                NotifikasjonTekst.TILTAK_OM_MENTOR_ENDRET);
    }

    @EventListener
    public void endreStillingbeskrivelse(StillingsbeskrivelseEndret event) {
        opprettOgSendNyBeskjed(event.getAvtale(), HendelseType.STILLINGSBESKRIVELSE_ENDRET,
                NotifikasjonTekst.TILTAK_STILLINGSBESKRIVELSE_ENDRET);
    }

    @EventListener
    public void endreOppfølgingOgTilretteleggingInformasjon(OppfølgingOgTilretteleggingEndret event) {
        opprettOgSendNyBeskjed(event.getAvtale(), HendelseType.OPPFØLGING_OG_TILRETTELEGGING_ENDRET,
                NotifikasjonTekst.TILTAK_OPPFØLGING_OG_TILRETTELEGGING_ENDRET);
    }

    @EventListener
    public void endreKontaktInformasjon(KontaktinformasjonEndret event) {
        opprettOgSendNyBeskjed(event.getAvtale(), HendelseType.KONTAKTINFORMASJON_ENDRET,
                NotifikasjonTekst.TILTAK_KONTAKTINFORMASJON_ENDRET);
    }

    @EventListener
    public void endreTilskuddsberegning(TilskuddsberegningEndret event) {
        opprettOgSendNyBeskjed(event.getAvtale(), HendelseType.TILSKUDDSBEREGNING_ENDRET,
                NotifikasjonTekst.TILTAK_TILSKUDDSBEREGNING_ENDRET);
    }

    @EventListener
    public void forkortAvtale(AvtaleForkortet event) {
        opprettOgSendNyBeskjed(event.getAvtale(), HendelseType.AVTALE_FORKORTET,
                NotifikasjonTekst.TILTAK_AVTALE_FORKORTET);
    }

    @EventListener
    public void forlengAvtale(AvtaleForlenget event) {
        opprettOgSendNyBeskjed(event.getAvtale(), HendelseType.AVTALE_FORLENGET,
                NotifikasjonTekst.TILTAK_AVTALE_FORLENGET);
    }
}
