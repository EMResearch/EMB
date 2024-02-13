package no.nav.tag.tiltaksgjennomforing.varsel.notifikasjon.request;

import lombok.Data;
import java.util.UUID;

@Data
public class Variables {
    UUID eksternId;
    String virksomhetsnummer;
    String lenke;
    String serviceCode;
    String serviceEdition;
    String merkelapp;
    String tekst;
    UUID id;
    UUID grupperingsId;
}
