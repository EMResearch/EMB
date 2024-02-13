package no.nav.tag.tiltaksgjennomforing.varsel.events;

import lombok.Value;
import no.nav.tag.tiltaksgjennomforing.varsel.Sms;

@Value
public class SmsSendt {
    Sms sms;
}
