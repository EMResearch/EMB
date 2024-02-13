package no.nav.tag.tiltaksgjennomforing.datadeling;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.tag.tiltaksgjennomforing.avtale.*;
import no.nav.tag.tiltaksgjennomforing.avtale.events.*;
import no.nav.tag.tiltaksgjennomforing.utils.Now;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AvtaleHendelseLytter {

    private final AvtaleMeldingEntitetRepository avtaleMeldingEntitetRepository;
    private final ObjectMapper objectMapper;

    @EventListener
    public void avtaleOpprettetAvArbeidsgiver(AvtaleOpprettetAvArbeidsgiver event) {
        Avtale avtale = event.getAvtale();
        lagHendelse(avtale, HendelseType.OPPRETTET_AV_ARBEIDSGIVER, avtale.getBedriftNr(), Avtalerolle.ARBEIDSGIVER);
    }

    @EventListener
    public void avtaleOpprettetAvVeileder(AvtaleOpprettetAvVeileder event) {
        Avtale avtale = event.getAvtale();
        lagHendelse(avtale, HendelseType.OPPRETTET, event.getUtfortAv(), Avtalerolle.VEILEDER);
    }

    @EventListener
    public void avtaleEndret(AvtaleEndret event) {
        Avtale avtale = event.getAvtale();
        lagHendelse(avtale, HendelseType.ENDRET, event.getUtfortAv(), event.getUtfortAvRolle());
    }

    @EventListener
    public void avtaleInngått(AvtaleInngått event) {
        Avtale avtale = event.getAvtale();
        lagHendelse(avtale, HendelseType.AVTALE_INNGÅTT, event.getUtførtAv(), event.getUtførtAvRolle());
    }

    @EventListener
    public void avtaleForlenget(AvtaleForlenget event) {
        lagHendelse(event.getAvtale(), HendelseType.AVTALE_FORLENGET, event.getUtførtAv(), Avtalerolle.VEILEDER);
    }

    @EventListener
    public void avtaleForkortet(AvtaleForkortet event) {
        lagHendelse(event.getAvtale(), HendelseType.AVTALE_FORKORTET, event.getUtførtAv(), Avtalerolle.VEILEDER);
    }

    @EventListener
    public void avtaleAnnullert(AnnullertAvVeileder event) {
        lagHendelse(event.getAvtale(), HendelseType.ANNULLERT, event.getUtfortAv(), Avtalerolle.VEILEDER);
    }

    @EventListener
    public void tilskuddsberegningEndret(TilskuddsberegningEndret event) {
        lagHendelse(event.getAvtale(), HendelseType.TILSKUDDSBEREGNING_ENDRET, event.getUtførtAv(), Avtalerolle.VEILEDER);
    }

    @EventListener
    public void stillingsbeskrivelseEndret(StillingsbeskrivelseEndret event) {
        lagHendelse(event.getAvtale(), HendelseType.STILLINGSBESKRIVELSE_ENDRET, event.getUtførtAv(), Avtalerolle.VEILEDER);
    }

    @EventListener
    public void kontaktinformasjonEndret(KontaktinformasjonEndret event) {
        lagHendelse(event.getAvtale(), HendelseType.KONTAKTINFORMASJON_ENDRET, event.getUtførtAv(), Avtalerolle.VEILEDER);
    }

    @EventListener
    public void oppfølgingOgTilretteleggingEndret(OppfølgingOgTilretteleggingEndret event) {
        lagHendelse(event.getAvtale(), HendelseType.OPPFØLGING_OG_TILRETTELEGGING_ENDRET, event.getUtførtAv(), Avtalerolle.VEILEDER);
    }

    @EventListener
    public void målEndret(MålEndret event) {
        lagHendelse(event.getAvtale(), HendelseType.MÅL_ENDRET, event.getUtførtAv(), Avtalerolle.VEILEDER);
    }

    @EventListener
    public void inkluderingstilskuddEndret(InkluderingstilskuddEndret event) {
        lagHendelse(event.getAvtale(), HendelseType.INKLUDERINGSTILSKUDD_ENDRET, event.getUtførtAv(), Avtalerolle.VEILEDER);
    }

    @EventListener
    public void omMentorEndret(OmMentorEndret event) {
        lagHendelse(event.getAvtale(), HendelseType.OM_MENTOR_ENDRET, event.getUtførtAv(), Avtalerolle.VEILEDER);
    }

    @EventListener
    public void nyVeilederPåAvtale(AvtaleNyVeileder event) {
        lagHendelse(event.getAvtale(), HendelseType.NY_VEILEDER, event.getAvtale().getVeilederNavIdent(), Avtalerolle.VEILEDER);
    }

    @EventListener
    public void godkjentAvArbeidsgiver(GodkjentAvArbeidsgiver event) {
        lagHendelse(event.getAvtale(), HendelseType.GODKJENT_AV_ARBEIDSGIVER, event.getUtfortAv(), Avtalerolle.ARBEIDSGIVER);
    }
    @EventListener
    public void godkjentAvVeileder(GodkjentAvVeileder event) {
        lagHendelse(event.getAvtale(), HendelseType.GODKJENT_AV_VEILEDER, event.getUtfortAv(), Avtalerolle.VEILEDER);
    }
    @EventListener
    public void godkjentAvDeltaker(GodkjentAvDeltaker event) {
        lagHendelse(event.getAvtale(), HendelseType.GODKJENT_AV_DELTAKER, event.getUtfortAv(), Avtalerolle.DELTAKER);
    }
    @EventListener
    public void godkjentPåVegneAvDeltaker(GodkjentPaVegneAvDeltaker event) {
        lagHendelse(event.getAvtale(), HendelseType.GODKJENT_PAA_VEGNE_AV, event.getUtfortAv(), Avtalerolle.VEILEDER);
    }
    @EventListener
    public void godkjentPåVegneAvArbeidsgiver(GodkjentPaVegneAvArbeidsgiver event) {
        lagHendelse(event.getAvtale(), HendelseType.GODKJENT_PAA_VEGNE_AV_ARBEIDSGIVER, event.getUtfortAv(), Avtalerolle.VEILEDER);
    }
    @EventListener
    public void godkjentPåVegneAvDeltakerOgArbeidsgiver(GodkjentPaVegneAvDeltakerOgArbeidsgiver event) {
        lagHendelse(event.getAvtale(), HendelseType.GODKJENT_PAA_VEGNE_AV_DELTAKER_OG_ARBEIDSGIVER, event.getUtfortAv(), Avtalerolle.VEILEDER);
    }
    @EventListener
    public void signertAvMentor(SignertAvMentor event) {
        lagHendelse(event.getAvtale(), HendelseType.SIGNERT_AV_MENTOR, event.getUtfortAv(), Avtalerolle.MENTOR);
    }

    private void lagHendelse(Avtale avtale, HendelseType hendelseType, Identifikator utførtAv, Avtalerolle utførtAvRolle) {
        LocalDateTime tidspunkt = Now.localDateTime();
        UUID meldingId = UUID.randomUUID();

        AvtaleHendelseUtførtAvRolle rolle = AvtaleHendelseUtførtAvRolle.SYSTEM;
        if(utførtAvRolle != null) {
            rolle = AvtaleHendelseUtførtAvRolle.valueOf(utførtAvRolle.name());
        }
        var melding = AvtaleMelding.create(avtale, avtale.getGjeldendeInnhold(), utførtAv, rolle, hendelseType);
        try {
            String meldingSomString = objectMapper.writeValueAsString(melding);
            AvtaleMeldingEntitet entitet = new AvtaleMeldingEntitet(meldingId, avtale.getId(), tidspunkt, hendelseType, avtale.statusSomEnum(), meldingSomString);
            avtaleMeldingEntitetRepository.save(entitet);
        } catch (JsonProcessingException e) {
            log.error("Feil ved parsing av AvtaleHendelseMelding til json for avtale med id: {}", avtale.getId());
            throw new RuntimeException(e);
        }
    }

}
