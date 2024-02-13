package no.nav.tag.tiltaksgjennomforing.varsel;

import lombok.RequiredArgsConstructor;
import no.nav.tag.tiltaksgjennomforing.avtale.*;
import no.nav.tag.tiltaksgjennomforing.avtale.events.*;
import no.nav.tag.tiltaksgjennomforing.varsel.kafka.SmsProducer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty("tiltaksgjennomforing.kafka.enabled")
public class LagSmsFraAvtaleHendelse {
    private final SmsRepository smsRepository;
    private final SmsProducer smsProducer;

    private static final BedriftNr NAV_ORGNR = new BedriftNr("889640782");

    @EventListener
    public void avtaleDeltMedAvtalepart(AvtaleDeltMedAvtalepart event) {
        switch(event.getAvtalepart()){
            case ARBEIDSGIVER -> lagreOgSendKafkaMelding(smsTilArbeidsgiver(event.getAvtale(), HendelseType.DELT_MED_ARBEIDSGIVER));
            case DELTAKER -> lagreOgSendKafkaMelding(smsTilDeltaker(event.getAvtale(), HendelseType.DELT_MED_DELTAKER));
            case MENTOR -> lagreOgSendKafkaMelding(smsTilMentor(event.getAvtale(), HendelseType.DELT_MED_MENTOR));
        }
    }
    @EventListener
    public void avtaleGodkjentAvDeltaker(GodkjentAvDeltaker event) {
        var sms = smsTilVeileder(event.getAvtale(), HendelseType.GODKJENT_AV_DELTAKER);
        lagreOgSendKafkaMelding(sms);
    }
    @EventListener
    public void avtaleGodkjentAvArbeidsgiver(GodkjentAvArbeidsgiver event) {
        var sms = smsTilVeileder(event.getAvtale(), HendelseType.GODKJENT_AV_ARBEIDSGIVER);
        lagreOgSendKafkaMelding(sms);
    }
    @EventListener
    public void avtaleInngått(AvtaleInngått event) {
        var smsTilDeltaker = smsTilDeltaker(event.getAvtale(), HendelseType.AVTALE_INNGÅTT);
        var smsTilArbeidsgiver = smsTilArbeidsgiver(event.getAvtale(), HendelseType.AVTALE_INNGÅTT);

        if(event.getAvtale().getTiltakstype() == Tiltakstype.MENTOR) {
            var smsTilMentor = smsTilMentor(event.getAvtale(), HendelseType.AVTALE_INNGÅTT);
            lagreOgSendKafkaMelding(smsTilMentor);
        }

        lagreOgSendKafkaMelding(smsTilDeltaker);
        lagreOgSendKafkaMelding(smsTilArbeidsgiver);

    }
    @EventListener
    public void godkjenningerOpphevetAvArbeidsgiver(GodkjenningerOpphevetAvArbeidsgiver event) {
        if (event.getGamleVerdier().isGodkjentAvDeltaker()) {
            var smsTilDeltaker = smsTilDeltaker(event.getAvtale(), HendelseType.GODKJENNINGER_OPPHEVET_AV_ARBEIDSGIVER);
            lagreOgSendKafkaMelding(smsTilDeltaker);
        }
        var smsTilVeileder = smsTilVeileder(event.getAvtale(), HendelseType.OPPRETTET_AV_ARBEIDSGIVER);
        lagreOgSendKafkaMelding(smsTilVeileder);
    }
    @EventListener
    public void godkjenningerOpphevetAvVeileder(GodkjenningerOpphevetAvVeileder event) {
        if (event.getGamleVerdier().isGodkjentAvDeltaker()) {
            var smsTilDeltaker = smsTilDeltaker(event.getAvtale(), HendelseType.GODKJENNINGER_OPPHEVET_AV_VEILEDER);
            lagreOgSendKafkaMelding(smsTilDeltaker);
        }
        if (event.getGamleVerdier().isGodkjentAvArbeidsgiver()) {
            var smsTilArbeidsgiver = smsTilArbeidsgiver(event.getAvtale(), HendelseType.GODKJENNINGER_OPPHEVET_AV_VEILEDER);
            lagreOgSendKafkaMelding(smsTilArbeidsgiver);
        }
    }
    @EventListener
    public void refusjonKlar(RefusjonKlar event) {
        if(event.getAvtale().getTiltakstype() == Tiltakstype.SOMMERJOBB || event.getAvtale().getTiltakstype() == Tiltakstype.MIDLERTIDIG_LONNSTILSKUDD || event.getAvtale().getTiltakstype() == Tiltakstype.VARIG_LONNSTILSKUDD || event.getAvtale().getTiltakstype() == Tiltakstype.MENTOR){
            String tiltakNavn = event.getAvtale().getTiltakstype().getBeskrivelse().toLowerCase();
            String smsTekst = String.format("Dere kan nå søke om refusjon for tilskudd til %s for avtale med nr: %s. Frist for å søke er %s. Søk om refusjon her: https://tiltak-refusjon.nav.no. Hilsen NAV.", tiltakNavn, event.getAvtale().getAvtaleNr(), event.getFristForGodkjenning());
            refusjonVarslingMedKontaktperson(event.getAvtale(), smsTekst, HendelseType.REFUSJON_KLAR);
        }
    }

    @EventListener
    public void refusjonKlarRevarsel(RefusjonKlarRevarsel event) {
        if(event.getAvtale().getTiltakstype() == Tiltakstype.SOMMERJOBB || event.getAvtale().getTiltakstype() == Tiltakstype.MIDLERTIDIG_LONNSTILSKUDD || event.getAvtale().getTiltakstype() == Tiltakstype.VARIG_LONNSTILSKUDD || event.getAvtale().getTiltakstype() == Tiltakstype.MENTOR) {
            String tiltakNavn = event.getAvtale().getTiltakstype().getBeskrivelse().toLowerCase();
            String smsTekst = String.format("Fristen nærmer seg for å søke om refusjon for tilskudd til %s for avtale med nr: %s. Frist for å søke er %s. Søk om refusjon her: https://tiltak-refusjon.nav.no. Hilsen NAV.",tiltakNavn, event.getAvtale().getAvtaleNr(), event.getFristForGodkjenning());
            refusjonVarslingMedKontaktperson(event.getAvtale(), smsTekst, HendelseType.REFUSJON_KLAR_REVARSEL);
        }
    }

    @EventListener
    public void refusjonFristForlenget(RefusjonFristForlenget event) {
        if(event.getAvtale().getTiltakstype() == Tiltakstype.SOMMERJOBB || event.getAvtale().getTiltakstype() == Tiltakstype.MIDLERTIDIG_LONNSTILSKUDD || event.getAvtale().getTiltakstype() == Tiltakstype.VARIG_LONNSTILSKUDD || event.getAvtale().getTiltakstype() == Tiltakstype.MENTOR) {
            String smsTekst = String.format("Fristen for å godkjenne refusjon for avtale med nr: %s har blitt forlenget. Du kan sjekke fristen og søke om refusjon her: https://tiltak-refusjon.nav.no. Hilsen NAV.", event.getAvtale().getAvtaleNr());
            refusjonVarslingMedKontaktperson(event.getAvtale(), smsTekst, HendelseType.REFUSJON_FRIST_FORLENGET);
        }
    }

    @EventListener
    public void refusjonKorrigert(RefusjonKorrigert event) {
        if(event.getAvtale().getTiltakstype() == Tiltakstype.SOMMERJOBB || event.getAvtale().getTiltakstype() == Tiltakstype.MIDLERTIDIG_LONNSTILSKUDD || event.getAvtale().getTiltakstype() == Tiltakstype.VARIG_LONNSTILSKUDD || event.getAvtale().getTiltakstype() == Tiltakstype.MENTOR) {
            String smsTekst = String.format("Tidligere innsendt refusjon på avtale med nr %d er korrigert. Se detaljer her: https://tiltak-refusjon.nav.no. Hilsen NAV.", event.getAvtale().getAvtaleNr());
            refusjonVarslingMedKontaktperson(event.getAvtale(), smsTekst, HendelseType.REFUSJON_KORRIGERT);
        }
    }

    private void refusjonVarslingMedKontaktperson(Avtale avtale, String smsTekst, HendelseType hendelseType) {
        if (avtale.getGjeldendeInnhold().getRefusjonKontaktperson() != null && avtale.getGjeldendeInnhold().getRefusjonKontaktperson().getRefusjonKontaktpersonTlf() != null) {
            if (avtale.getGjeldendeInnhold().getRefusjonKontaktperson().getØnskerVarslingOmRefusjon() != null && avtale.getGjeldendeInnhold().getRefusjonKontaktperson().getØnskerVarslingOmRefusjon()) {
                Sms smsTilArbeidsgiver = Sms.nyttVarsel(avtale.getGjeldendeInnhold().getArbeidsgiverTlf(), avtale.getBedriftNr(), smsTekst, hendelseType, avtale.getId());
                lagreOgSendKafkaMelding(smsTilArbeidsgiver);
            }
            Sms smsTilKontaktpersonForRefusjon = Sms.nyttVarsel(avtale.getGjeldendeInnhold().getRefusjonKontaktperson().getRefusjonKontaktpersonTlf(), avtale.getBedriftNr(), smsTekst, hendelseType, avtale.getId());
            lagreOgSendKafkaMelding(smsTilKontaktpersonForRefusjon);
        } else {
            Sms sms = Sms.nyttVarsel(avtale.getGjeldendeInnhold().getArbeidsgiverTlf(), avtale.getBedriftNr(), smsTekst, hendelseType, avtale.getId());
            lagreOgSendKafkaMelding(sms);
        }
    }


    private void lagreOgSendKafkaMelding(Sms sms) {
        smsRepository.save(sms);
        smsProducer.sendSmsVarselMeldingTilKafka(sms);
    }

    private static Sms smsTilDeltaker(Avtale avtale, HendelseType hendelse) {
        return Sms.nyttVarsel(avtale.getGjeldendeInnhold().getDeltakerTlf(), avtale.getDeltakerFnr(), "Du har mottatt et nytt varsel på https://arbeidsgiver.nav.no/tiltaksgjennomforing", hendelse, avtale.getId());
    }

    private static Sms smsTilMentor(Avtale avtale, HendelseType hendelse) {
        return Sms.nyttVarsel(avtale.getGjeldendeInnhold().getMentorTlf(), avtale.getMentorFnr(), "Du har mottatt et nytt varsel på https://arbeidsgiver.nav.no/tiltaksgjennomforing", hendelse, avtale.getId());
    }

    private static Sms smsTilArbeidsgiver(Avtale avtale, HendelseType hendelse) {
        return Sms.nyttVarsel(avtale.getGjeldendeInnhold().getArbeidsgiverTlf(), avtale.getBedriftNr(), "Du har mottatt et nytt varsel på https://arbeidsgiver.nav.no/tiltaksgjennomforing", hendelse, avtale.getId());
    }

    private static Sms smsTilVeileder(Avtale avtale, HendelseType hendelse) {
        return Sms.nyttVarsel(avtale.getGjeldendeInnhold().getVeilederTlf(), NAV_ORGNR, "Du har mottatt et nytt varsel på https://tiltaksgjennomforing.intern.nav.no/tiltaksgjennomforing", hendelse, avtale.getId());
    }
}
